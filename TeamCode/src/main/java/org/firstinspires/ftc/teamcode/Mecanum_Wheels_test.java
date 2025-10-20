package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.RobotLog;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
//
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
@SuppressWarnings("unused")
@TeleOp(name = "Mecanum Crab (LS) + Gyro", group = "Drive")
public class Mecanum_Wheels_test extends OpMode {

    // Drive motors
    private DcMotor frontLeft, frontRight, backLeft, backRight;

    // IMU
    private IMU imu;
    private boolean fieldCentric = false;

    // simple button edge detectors
    private boolean backPrev = false;
    private boolean yPrev = false;

    // tuning
    private static final double DEADBAND = 0.05;
    private static final double ROT_SCALE = 0.8; // limit rotation aggressiveness if using right stick

    @Override
    public void init() {
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");

        // Set motor directions to match your wiring/gearboxes
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);

        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // --- IMU init ---
        imu = hardwareMap.get(IMU.class, "imu");

        // Set the physical orientation of your Hub on the robot.
        // Update these if your Hub is mounted differently.
        RevHubOrientationOnRobot.LogoFacingDirection logo = RevHubOrientationOnRobot.LogoFacingDirection.UP;
        RevHubOrientationOnRobot.UsbFacingDirection usb  = RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;

        IMU.Parameters imuParams = new IMU.Parameters(new RevHubOrientationOnRobot(logo, usb));
        imu.initialize(imuParams);

        telemetry.addLine("Mecanum Crab + Gyro");
        telemetry.addLine("Back = toggle field-centric");
        telemetry.addLine("Y = zero heading");
        telemetry.addLine("Left stick = crab, Right X = rotate");
        telemetry.update();
    }

    @Override
    public void loop() {
        // --- Read gamepad ---
        double lx = gamepad1.left_stick_x;   // +right
        double ly = -gamepad1.left_stick_y;  // +forward (invert Y)
        double rx = gamepad1.right_stick_x * ROT_SCALE; // rotation input (set to 0 for pure crab)

        // Deadband
        lx = (Math.abs(lx) < DEADBAND) ? 0 : lx;
        ly = (Math.abs(ly) < DEADBAND) ? 0 : ly;
        rx = (Math.abs(rx) < DEADBAND) ? 0 : rx;

        // --- Field-centric toggle (edge detect on Back) ---
        boolean backNow = gamepad1.back;
        if (backNow && !backPrev) {
            fieldCentric = !fieldCentric;
        }
        backPrev = backNow;

        // --- Zero heading (edge detect on Y) ---
        boolean yNow = gamepad1.y;
        if (yNow && !yPrev) {
            imu.resetYaw();
        }
        yPrev = yNow;

        // --- Transform stick vector by -heading if field-centric ---
        double x = lx;
        double y = ly;

        if (fieldCentric) {
            YawPitchRollAngles ypr = imu.getRobotYawPitchRollAngles();
            double heading = ypr.getYaw(AngleUnit.RADIANS); // +CCW

            double cosA = Math.cos(-heading);
            double sinA = Math.sin(-heading);

            double rotX = x * cosA - y * sinA;
            double rotY = x * sinA + y * cosA;

            x = rotX;
            y = rotY;
        }

        // --- Mecanum mix (X-strfe, Y-forward, R-rotate) ---
        double fl = y + x + rx;
        double fr = y - x - rx;
        double bl = y - x + rx;
        double br = y + x - rx;

        // Normalize so |power| ≤ 1
        double max = Math.max(1.0, Math.max(Math.abs(fl),
                Math.max(Math.abs(fr), Math.max(Math.abs(bl), Math.abs(br)))));

        fl /= max; fr /= max; bl /= max; br /= max;

        // Optional: scale by stick magnitude for finer low-speed control
        double mag = Range.clip(Math.hypot(gamepad1.left_stick_x, gamepad1.left_stick_y), 0, 1);
        fl *= mag; fr *= mag; bl *= mag; br *= mag;

        // --- Send to motors ---
        frontLeft.setPower(fl);
        frontRight.setPower(fr);
        backLeft.setPower(bl);
        backRight.setPower(br);

        // --- Telemetry ---
        telemetry.addData("Mode", fieldCentric ? "Field-centric" : "Robot-centric");
        telemetry.addData("Heading (deg)", imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
        telemetry.addData("LX/LY", "%.2f / %.2f", gamepad1.left_stick_x, gamepad1.left_stick_y);
        telemetry.addData("Powers", "FL %.2f  FR %.2f  BL %.2f  BR %.2f", fl, fr, bl, br);
        telemetry.update();
    }

    @Override
    public void stop() {
        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setPower(0);
        }
    }
}
