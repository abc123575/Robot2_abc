package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.CRServo;

@TeleOp(name = "GamePad", group = "TeleOp")
public class GamePad extends OpMode {

    private DcMotor motor;
    private DcMotor motor2;
    private DcMotor motor3;
    private CRServo servo1;
    private CRServo servo2;

    @Override
    public void init() {
        motor = hardwareMap.get(DcMotor.class, "motor1");
        motor2 = hardwareMap.get(DcMotor.class, "motor2");
        motor3 = hardwareMap.get(DcMotor.class, "motor3");
        servo1 = hardwareMap.get(CRServo.class, "servo1");
        servo2 = hardwareMap.get(CRServo.class, "servo2");
        telemetry.addData("Status", "Initialized");
    }

    @Override
    public void loop() {
        double speedForward_ly = gamepad1.left_stick_y;
        double speedForward_ry = gamepad1.right_stick_y;
        boolean rearRightBumper = gamepad1.right_bumper;

        if (rearRightBumper) motor.setPower(1);
        else motor.setPower(0);

        if (speedForward_ry != 0) {
            motor2.setPower(-speedForward_ry);
        } else if (speedForward_ly != 0) {
            motor3.setPower(-speedForward_ly);
        } else {
            motor2.setPower(0);
            motor3.setPower(0);
        }

        // ...
        if (gamepad1.a) { // Press A to run servo1 forward
            servo1.setPower(1.0);

            telemetry.addData("Servo1", "Running");
        }
        else if (gamepad1.b) { // Press B to run servo2 forward
            servo2.setPower(1.0);
            telemetry.addData("Servo2", "Running");
        }
        else {
            // If neither button is pressed, stop both servos
            servo1.setPower(0.0);
            servo2.setPower(0.0);
            telemetry.addData("Servos", "Stopped");
        }
//...


        telemetry.addData("Servo1 Power", servo1.getPower());
        telemetry.addData("Servo2 Power", servo2.getPower());
    }
}
