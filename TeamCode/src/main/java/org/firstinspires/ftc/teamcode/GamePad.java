package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;


import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp
public class GamePad extends OpMode {

    private DcMotor motor;



    @Override
    public void init(){
        motor = hardwareMap.get(DcMotor.class, "motor1");
    }



    @Override
    public void loop() {
        double speedForward_ly = gamepad1.left_stick_y;
        double speedForward_lx = gamepad1.left_stick_x;
        double speedForward_rx = gamepad1.right_stick_x;
        double speedForward_ry = gamepad1.right_stick_y;
        double diff = gamepad1.left_stick_x - gamepad1.right_stick_x;
        double rearTriggerR = gamepad1.right_trigger;
        double rearTriggerL = gamepad1.left_trigger;
        double sumTriggers = (gamepad1.left_trigger + gamepad1.right_trigger)/2;
        boolean rearLeftBumper = gamepad1.left_bumper;
        boolean rearRightBumper = gamepad1.right_bumper;


        telemetry.addData("left x", speedForward_lx);
        telemetry.addData("left y", speedForward_ly);
        telemetry.addData("right x", speedForward_rx);
        telemetry.addData("right y", speedForward_ry);

        telemetry.addData("a button", gamepad1.a);
        telemetry.addData("b button", gamepad1.b);
        telemetry.addData("DIFFERENCE", diff);

        telemetry.addData("Rear Right Trigger", rearTriggerR);
        telemetry.addData("Rear Left Trigger", gamepad1.left_trigger);
        telemetry.addData("Sum of Triggers", sumTriggers);

        telemetry.addData("Rear Left Bumper", rearLeftBumper);
        telemetry.addData("Rear Right Bumper", rearRightBumper); //comment

        if (rearRightBumper == true) {
            motor.setPower(0.5);
        } else {
            motor.setPower(0);
        }


    }

}
