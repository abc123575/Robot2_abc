package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name = "Drive Forward", group = "Linear Opmode")
public class DriveForwardAuto extends LinearOpMode {

    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    // In

    @Override
    public void runOpMode() {itialize the hardware variables. Note: the names must match your config!
        leftDrive  = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");

        // Reverse one motor so the robot drives forward
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);

        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        // Drive forward at full speed for 2 seconds
        leftDrive.setPower(0.5);
        rightDrive.setPower(0.5);

        sleep(2000); // time in milliseconds (2000ms = 2 seconds)

        // Stop all motion
        leftDrive.setPower(0);
        rightDrive.setPower(0);
    }
}
