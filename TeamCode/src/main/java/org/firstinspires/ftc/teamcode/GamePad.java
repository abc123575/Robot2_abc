package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.RobotLog;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
@SuppressWarnings("unused")
@TeleOp(name = "GamePad + UDP Float", group = "Net")
public class GamePad extends OpMode {

    private DcMotor motor1;
    private DcMotor motor2;
    private DcMotor motor3;
    private CRServo servo1;
    private CRServo servo2;

    // --- UDP config ---
    private static final int UDP_PORT = 9000;
    private static final int UDP_TIMEOUT_MS = 200; // short timeout so we can shut down fast
    private volatile boolean running = false;
    private Thread udpThread;
    private DatagramSocket udpSocket; // so we can close it in stop()
    private final AtomicReference<Double> udpSpeed = new AtomicReference<>(0.0);

    @Override
    public void init() {
        motor1 = hardwareMap.get(DcMotor.class, "motor1");
        motor2 = hardwareMap.get(DcMotor.class, "motor2");
        motor3 = hardwareMap.get(DcMotor.class, "motor3");
        servo1 = hardwareMap.get(CRServo.class, "servo1");
        servo2 = hardwareMap.get(CRServo.class, "servo2");

        // Optional: consistent stopping behavior
        motor1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor3.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        running = true;
        udpThread = new Thread(this::listenUdp, "UDP-Listener");
        udpThread.setDaemon(true);
        udpThread.start();
        telemetry.addLine("UDP listener starting on port " + UDP_PORT);
    }

    // This method runs in a separate thread to listen for UDP packets
    private void listenUdp() {
        byte[] buffer = new byte[64];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            udpSocket = socket;
            socket.setSoTimeout(UDP_TIMEOUT_MS);
            RobotLog.d("UDP socket created; listening on port " + UDP_PORT);

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    socket.receive(packet);
                    String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();

                    double receivedSpeed;
                    try {
                        receivedSpeed = Double.parseDouble(data);
                    } catch (NumberFormatException nfe) {
                        RobotLog.e("UDP parse error for payload: '" + data + "'");
                        continue;
                    }

                    receivedSpeed = Range.clip(receivedSpeed, -1.0, 1.0);
                    udpSpeed.set(receivedSpeed);

                } catch (SocketTimeoutException ste) {
                    // normal: loop again so we can notice 'running' changes
                } catch (Exception e) {
                    if (running) {
                        RobotLog.e("UDP receive error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            RobotLog.e("UDP socket error: " + e.getMessage());
        } finally {
            RobotLog.d("UDP listener stopped.");
            udpSocket = null;
        }
    }

    @Override
    public void loop() {
        // --- Gamepad controls ---
        double ly = -gamepad1.left_stick_y;   // FTC sticks: up is negative
        double ry = -gamepad1.right_stick_y;
        double rt = gamepad1.right_trigger;

        // Latest speed via UDP
        double udpVal = udpSpeed.get();

        // Example control: mix gamepad with UDP as a scale factor
        double p1 = Range.clip(ly * (0.5 + 0.5 * Math.abs(udpVal)), -1, 1);
        double p2 = Range.clip(ry * (0.5 + 0.5 * Math.abs(udpVal)), -1, 1);
        double p3 = udpVal; // direct UDP on motor3, if you want

        motor1.setPower(p1);
        motor2.setPower(p2);
        motor3.setPower(p3);

        // Example servos (comment out if not used)
        // servo1.setPower(udpVal);
        // servo2.setPower(-udpVal);

        // --- Telemetry ---
        telemetry.addData("Left Stick Y", ly);
        telemetry.addData("Right Stick Y", ry);
        telemetry.addData("Right Trigger", rt);
        telemetry.addData("UDP Speed", udpVal);
        telemetry.update();
    }

    @Override
    public void stop() {
        running = false;

        // Close socket to unblock receive()
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

        if (udpThread != null) {
            udpThread.interrupt();
        }

        // Safe stop
        if (motor1 != null) motor1.setPower(0);
        if (motor2 != null) motor2.setPower(0);
        if (motor3 != null) motor3.setPower(0);
        if (servo1 != null) servo1.setPower(0);
        if (servo2 != null) servo2.setPower(0);
    }
}



