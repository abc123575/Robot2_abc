package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.tfod.Recognition;
import org.firstinspires.ftc.vision.tfod.TfodProcessor;


import android.util.Size;
import java.util.List;

@Autonomous(name = "AprilTag + TFOD Balls (SDK 11)", group = "Concept")
public class AprilTagTfodBalls extends LinearOpMode {

    private VisionPortal visionPortal;
    private AprilTagProcessor aprilTag;
    private TfodProcessor tfod;

    @Override
    public void runOpMode() {

        initVision();

        telemetry.addLine("✅ Vision initialized");
        telemetry.addLine("Tap the 📷 icon on the DS for live view");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            displayAprilTags();
            displayTfodDetections();
            telemetry.addData("Camera State", visionPortal.getCameraState());
            telemetry.update();
        }

        visionPortal.close();
    }

    /** Initialize both AprilTag and TensorFlow processors. */
    private void initVision() {

        // --- AprilTag processor ---
        aprilTag = new AprilTagProcessor.Builder()
                .setDrawAxes(true)
                .setDrawCubeProjection(true)
                .setDrawTagOutline(true)
                .build();

        // --- TensorFlow processor ---
        // Place these files in TeamCode/src/main/assets/
        //    balls.tflite
        //    labels.txt   (contents: 0 green_balls ↵ 1 purple_balls)
        tfod = new TfodProcessor.Builder()
                .setModelAssetName("balls.tflite")
                .setModelLabels(new String[]{"green_balls", "purple_balls"})
                .setUseObjectTracker(true)
                .setMaxNumRecognitions(10)
                .setNumDetectorThreads(2)
                .setTrackerMaxOverlap(0.3)
                .setTrackerMinSize(16)
                .setMinResultConfidence(0.45f)     // adjust if needed
                .build();

        // --- VisionPortal ties everything together ---
        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .setCameraResolution(new Size(640, 480))
                .addProcessor(aprilTag)
                .addProcessor(tfod)                 // 👈 add TFOD processor
                .build();
    }

    /** Telemetry + pose data for AprilTags. */
    private void displayAprilTags() {
        List<AprilTagDetection> tags = aprilTag.getDetections();
        telemetry.addData("AprilTags Detected", tags.size());
        if (!tags.isEmpty()) {
            AprilTagDetection tag = tags.get(0);
            telemetry.addData("Tag ID", tag.id);
            telemetry.addData("X (in)", "%.2f", tag.ftcPose.x);
            telemetry.addData("Y (in)", "%.2f", tag.ftcPose.y);
            telemetry.addData("Z (in)", "%.2f", tag.ftcPose.z);
        }
    }

    /** Telemetry + bounding boxes for TFOD recognitions. */
    private void displayTfodDetections() {
        List<Recognition> recognitions = tfod.getRecognitions();
        telemetry.addData("TFOD Objects", recognitions.size());

        // Draw bounding boxes directly in DS preview
        tfod.setClippingMargins(0, 0, 0, 0);
        tfod.setZoom(1.0); // keep natural FOV

        for (Recognition rec : recognitions) {
            telemetry.addData("Label", rec.getLabel());
            telemetry.addData("Conf", "%.2f", rec.getConfidence());
            telemetry.addData("Box (L,T,R,B)", "%.0f, %.0f, %.0f, %.0f",
                    rec.getLeft(), rec.getTop(), rec.getRight(), rec.getBottom());
        }
    }
}
