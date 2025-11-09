package org.firstinspires.ftc.teamcode;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import java.util.ArrayList;
import java.util.List;

/**
 * AprilTag + Ball Tracker (Tuned for Real-World Use)
 * - Looser HSV thresholds for easier detection
 * - Gentle filtering and morphology for stability
 * - Debug overlays show color pixel counts and detections
 */
@Autonomous(name = "AprilTag + Ball Tracker (Tuned)", group = "Vision")
public class AprilTagAndBallTracker extends LinearOpMode {

    private VisionPortal visionPortal;
    private AprilTagProcessor aprilTag;
    private ColorBallProcessor colorProcessor;

    @Override
    public void runOpMode() {
        // --- AprilTag setup ---
        aprilTag = new AprilTagProcessor.Builder()
                .setDrawAxes(true)
                .setDrawTagOutline(true)
                .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                .build();

        // --- Color tracking processor ---
        colorProcessor = new ColorBallProcessor();

        // --- VisionPortal setup ---
        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .addProcessor(aprilTag)
                .addProcessor(colorProcessor)
                .enableLiveView(true)
                .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
                .build();

        telemetry.addLine("✅ Vision initialized — press ▶ to start");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            List<AprilTagDetection> detections = aprilTag.getDetections();
            int id = detections.isEmpty() ? -1 : detections.get(0).id;

            telemetry.addData("AprilTag ID", id);
            telemetry.addData("Detected Color", colorProcessor.getDetectedColorName());
            telemetry.update();

            sleep(50);
        }

        visionPortal.close();
    }

    /**
     * VisionProcessor for detecting purple and green balls.
     */
    static class ColorBallProcessor implements VisionProcessor {

        private int detectedColor = 0; // 0 = none, 1 = purple, 2 = green
        private double purpleCount = 0;
        private double greenCount = 0;

        @Override
        public void init(int width, int height, CameraCalibration calibration) {
            // Nothing to initialize
        }

        @Override
        public Object processFrame(Mat frame, long captureTimeNanos) {
            Mat hsv = new Mat();
            Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_RGB2HSV);

            // --- Wider HSV ranges for robustness ---
            Scalar lowerPurple = new Scalar(115, 50, 50);
            Scalar upperPurple = new Scalar(165, 255, 255);
            Scalar lowerGreen  = new Scalar(30, 40, 40);
            Scalar upperGreen  = new Scalar(90, 255, 255);

            Mat purpleMask = new Mat();
            Mat greenMask = new Mat();
            Core.inRange(hsv, lowerPurple, upperPurple, purpleMask);
            Core.inRange(hsv, lowerGreen, upperGreen, greenMask);

            // --- Gentle morphological smoothing ---
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3));
            Imgproc.morphologyEx(purpleMask, purpleMask, Imgproc.MORPH_CLOSE, kernel);
            Imgproc.morphologyEx(greenMask, greenMask, Imgproc.MORPH_CLOSE, kernel);

            // --- Count active pixels for debug ---
            purpleCount = Core.countNonZero(purpleMask);
            greenCount = Core.countNonZero(greenMask);

            // --- Detect both colors individually ---
            detectBalls(frame, purpleMask, new Scalar(255, 0, 255), "PURPLE");
            detectBalls(frame, greenMask, new Scalar(0, 255, 0), "GREEN");

            // --- Determine dominant color for telemetry ---
            if (purpleCount > 2000 && purpleCount > greenCount) detectedColor = 1;
            else if (greenCount > 2000 && greenCount > purpleCount) detectedColor = 2;
            else detectedColor = 0;

            // --- Debug overlays on frame ---
            Imgproc.putText(frame, "PurpleCount: " + (int)purpleCount, new Point(20,40),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255,0,255), 2);
            Imgproc.putText(frame, "GreenCount: " + (int)greenCount, new Point(20,70),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0,255,0), 2);

            hsv.release();
            purpleMask.release();
            greenMask.release();
            kernel.release();
            return frame;
        }

        /**
         * Detects and draws circular blobs for a given mask.
         */
        private void detectBalls(Mat frame, Mat mask, Scalar drawColor, String label) {
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mask, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area < 400) continue; // Skip small specks

                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                Point center = new Point();
                float[] radius = new float[1];
                Imgproc.minEnclosingCircle(contour2f, center, radius);

                double circleArea = Math.PI * radius[0] * radius[0];
                double circularity = area / circleArea;

                // Allow moderately round blobs
                if (circularity > 0.4 && circularity < 1.5 && radius[0] > 6 && radius[0] < 200) {
                    Imgproc.circle(frame, center, (int) radius[0], drawColor, 3);
                    Imgproc.putText(frame, label,
                            new Point(center.x - 30, center.y - radius[0] - 10),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, drawColor, 2);
                }

                contour2f.release();
            }

            hierarchy.release();
            for (MatOfPoint c : contours) c.release();
        }

        @Override
        public void onDrawFrame(android.graphics.Canvas canvas,
                                int onscreenWidth,
                                int onscreenHeight,
                                float scaleBmpPxToCanvasPx,
                                float scaleCanvasDensity,
                                Object userContext) {
            // Not used; OpenCV draws directly on frame
        }

        public int getDetectedColor() {
            return detectedColor;
        }

        public String getDetectedColorName() {
            if (detectedColor == 1) return "Purple";
            else if (detectedColor == 2) return "Green";
            else return "None";
        }
    }
}
