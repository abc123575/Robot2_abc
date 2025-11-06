package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "launchMechanism", group = "TeleOp")
public class launchMechanism extends LinearOpMode {

    private DcMotor LLaunch;
    private DcMotor RLaunch;

    private static final double LAUNCH_POWER = 0.6;   // Reduced power for slower shooting
    private static final int FORWARD_TICKS = 1440;
    private static final int BACK_TICKS = 200;

    private boolean forwardDirection = true;
    private boolean triggerPressed = false;

    @Override
    public void runOpMode() throws InterruptedException {

        // Hardware mapping
        LLaunch = hardwareMap.get(DcMotor.class, "LLaunch");
        RLaunch = hardwareMap.get(DcMotor.class, "RLaunch");

        // Reverse one motor if needed
        RLaunch.setDirection(DcMotorSimple.Direction.REVERSE);

        // Reset encoders
        LLaunch.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        RLaunch.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Use encoder mode
        LLaunch.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        RLaunch.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addLine("Launcher initialized. Waiting for start...");
        telemetry.update();

        // ✅ REQUIRED: Wait for driver to press Play
        waitForStart();

        // ✅ Optional: Run an active loop (so it doesn’t immediately end)
        while (opModeIsActive()) {
            telemetry.addData("Status", "Running");
            telemetry.addData("LLaunch position", LLaunch.getCurrentPosition());
            telemetry.addData("RLaunch position", RLaunch.getCurrentPosition());
            telemetry.update();
        }
    }
}
