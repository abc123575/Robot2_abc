package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

public class AprilTagWebcam {

    private AprilTagProcessor aprilTagProcessor;
    private VisionPortal visionPortal;
    private Telemetry telemetry;

    public void init(HardwareMap hardwareMap, Telemetry telemetry) {
        this.telemetry = telemetry;

        // Create the AprilTag processor
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setDrawAxes(true)
                .setDrawCubeProjection(true)
                .setDrawTagOutline(true)
                .build();

        // Build the vision portal using the default webcam
        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1")) // Match your config name!
                .addProcessor(aprilTagProcessor)
                .build();

        telemetry.addLine("AprilTag Webcam Initialized");
        telemetry.update();
    }

    public void update() {
        if (visionPortal != null) {
            // VisionPortal auto-updates, but we can add telemetry refresh here
            List<AprilTagDetection> currentDetections = aprilTagProcessor.getDetections();
            telemetry.addData("Detected Tags", currentDetections.size());
        }
    }

    public AprilTagDetection getTagBySpecificId(int id) {
        List<AprilTagDetection> detections = aprilTagProcessor.getDetections();
        for (AprilTagDetection tag : detections) {
            if (tag.id == id) {
                return tag;
            }
        }
        return null;
    }

    public void displayDetectionTelemetry(AprilTagDetection detection) {
        if (detection == null) {
            telemetry.addLine("No Tag Detected");
        } else {
            telemetry.addData("Tag ID", detection.id);
            telemetry.addData("X (in)", "%.2f", detection.ftcPose.x);
            telemetry.addData("Y (in)", "%.2f", detection.ftcPose.y);
            telemetry.addData("Z (in)", "%.2f", detection.ftcPose.z);
            telemetry.addData("Yaw (deg)", "%.2f", detection.ftcPose.yaw);
            telemetry.addData("Pitch (deg)", "%.2f", detection.ftcPose.pitch);
            telemetry.addData("Roll (deg)", "%.2f", detection.ftcPose.roll);
        }
        telemetry.update();
    }

    public void stop() {
        if (visionPortal != null) {
            visionPortal.close();
        }
    }
}
