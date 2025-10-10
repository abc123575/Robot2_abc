package org.firstinspires.ftc.teamcode;


import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp
public class GamePad extends OpMode {

    @Override
    public void init(){

    }

    @Override
    public void loop(){
        telemetry.addData("x", gamepad1.left_stick_x);
        telemetry.addData("y", gamepad1.left_stick_y);
        telemetry.addData("a button", gamepad1.a);
        telemetry.addData("x", gamepad1.right_stick_x);
        telemetry.addData("y", gamepad1.left_stick_y);


    }





}
