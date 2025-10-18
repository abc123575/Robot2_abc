package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.CRServo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@TeleOp(name="GamePad", group="Net")
public class GamePad extends OpMode {

    // Hardware
    private DcMotor motor;   // motor1 (controlled by UDP)
    private DcMotor motor2;  // still controlled by sticks
    private DcMotor motor3;  // still controlled by sticks
    private CRServo servo1;
    private CRServo servo2;

    // UDP
    private static final int UDP_PORT = 9000;
    private static final int SOCKET_TIMEOUT_MS = 300; // non-blocking exit
    private static final int FAILSAFE_MS = 1200;      // stop motor if stale
    private volatile boolean running = true;
    private Thread udpThread;
    private DatagramSocket udpSocket = null;

    private final AtomicReference<Double> udpSpeed = new AtomicReference<>(0.0);
    private volatile long lastRxMs = 0L;

    @Override
    public void init() {
        motor  = hardwareMap.get(DcMotor.class, "motor1");
        motor2 = hardwareMap.get(DcMotor.class, "motor2");
        motor3 = hardwareMap.get(DcMotor.class, "motor3");
        servo1 = hardwareMap.get(CRServo.class, "servo1");
        servo2 = hardwareMap.get(CRServo.class, "servo2");

        // Optional: set directions if needed for your build
        // motor.setDirection(DcMotor.Direction.FORWARD);

        // Start UDP listener
        udpThread = new Thread(this::listenUdp, "UDP-Listener");
        udpThread.start();

        telemetry.addLine("UDP listener on port " + UDP_PORT);
    }

    private void listenUdp() {
        byte[] buf = new byte[128];
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);

        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            udpSocket = socket;
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            System.out.println("UDP listening on " + UDP_PORT);

            while (running) {
                try {
                    socket.receive(pkt);
                    String msg = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8).trim();

                    // Accept plain float (e.g., "0.4", "-0.2"); ignore junk
                    double val = Double.parseDouble(msg);
                    // Clamp to motor safe range
                    if (val > 1.0)  val = 1.0;
                    if (val < -1.0) val = -1.0;

                    udpSpeed.set(val);
                    lastRxMs = System.currentTimeMillis();
                } catch (java.net.SocketTimeoutException ignore) {
                    // periodic timeout so we can exit cleanly
                } catch (Exception ex) {
                    // bad packet, ignore
                }
            }
        } catch (Exception e) {
            System.out.println("UDP socket error: " + e.getMessage());
        } finally {
            System.out.println("UDP listener stopped.");
        }
    }

    @Override
    public void loop() {
        // --- Read gamepad (still available if you want to mix) ---
        double ly = -gamepad1.left_stick_y;   // invert Y
        double ry = -gamepad1.right_stick_y;
        double triggerR = gamepad1.right_trigger;

        // --- Pull latest UDP speed for motor1 ---
        double cmd = udpSpeed.get();
        // Failsafe: stop if we haven't heard anything recently
        long now = System.currentTimeMillis();
        if (now - lastRxMs > FAILSAFE_MS) {
            cmd = 0.0;
        }

        // Apply to motor1 (forward/reverse with fractions)
        motor.setPower(cmd);

        // Optional: let right trigger override motor1 for testing
        if (triggerR > 0) {
            motor.setPower(triggerR); // override while trigger held
        }

        // Keep your existing wheel logic on motor2/motor3
        if (ry > 0) {
            motor2.setDirection(DcMotor.Direction.REVERSE);
            motor2.setPower(ry);
        } else if (ry < 0) {
            motor2.setDirection(DcMotor.Direction.FORWARD);
            motor2.setPower(-ry);
        } else if (ly > 0) {
            motor3.setDirection(DcMotor.Direction.REVERSE);
            motor3.setPower(ly);
        } else if (ly < 0) {
            motor3.setDirection(DcMotor.Direction.FORWARD);
            motor3.setPower(-ly);
        } else {
            motor2.setPower(0);
            motor3.setPower(0);
        }

        // Servos like before
        if (gamepad1.a)      servo1.setPower(1.0);
        else if (gamepad1.b) servo2.setPower(1.0);
        else { servo1.setPower(0.0); servo2.setPower(0.0); }

        // Telemetry
        telemetry.addData("UDP Speed", cmd);
        telemetry.addData("last packet (ms)", (now - lastRxMs));
        telemetry.addData("L Y", ly);
        telemetry.addData("R Y", ry);
        telemetry.addData("Trig R", triggerR);
        telemetry.addData("Servo1", servo1.getPower());
        telemetry.addData("Servo2", servo2.getPower());
        telemetry.update();
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close(); // unblock receive()
        } catch (Exception ignore) {}
        try {
            if (udpThread != null) udpThread.join(300);
        } catch (Exception ignore) {}
        motor.setPower(0);
    }
}
