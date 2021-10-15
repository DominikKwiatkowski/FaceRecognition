package com.libs.facerecognition;

import android.graphics.Bitmap;
import android.util.Log;

import com.common.FaceProcessingException;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;
import static org.opencv.imgproc.Imgproc.warpAffine;

/**
 * Class for all operations before image is put into face recognition. It provides method to detect
 * faces and trim faces.
 */
public class FacePreProcessor {
    public final String Tag = "ImagePreProcessor";
    private FaceDetector faceDetector;

    /**
     * Creates pre processor instance. Load cascade classifiers and build google face detector.
     */
    public FacePreProcessor(){
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        faceDetector = FaceDetection.getClient(options);
    }

    /**
     * Detect all faces on given frame. This function will use Google Ml Kit face detector.
     * @param frame image on which faces will be found
     * @return MatOfRect matrix of all face rectangular. Face rectangular is beginning point and
     * size
     */
    public Task<List<Face>> detectAllFacesUsingML(Mat frame) {
        Bitmap image = Bitmap.createBitmap(frame.cols(),  frame.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame,image);
        InputImage miImage = InputImage.fromBitmap(image,0);
        Task<List<Face>> result = faceDetector.process(miImage);

        Log.d(Tag, "Face detection started");
        return result;
    }

    /**
     * Detect face, rotate it and trim.
     *
     * @param image image of face which will be preprocessed
     * @return Matrix of preprocessed face
     */
    public Mat preProcessOneFace(Mat image ) throws FaceProcessingException {
        Task<List<Face>> task = detectAllFacesUsingML(image);
        while(!task.isComplete()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Check number of detected faces.
        List<Face> faces = task.getResult();
        if (faces.size() == 0)
            throw new FaceProcessingException(FaceProcessingException.NO_FACES);
        else if (faces.size() > 1)
            throw new FaceProcessingException(FaceProcessingException.MORE_THAN_ONE_FACE);

        // Rotate faces.
        Face face = faces.get(0);
        double angle =  -face.getHeadEulerAngleZ();
        Mat rotatedImage = rotateImageByAngle(image, angle, face.getBoundingBox().centerX(),face.getBoundingBox().centerY());
        android.graphics.Rect rectangle = face.getBoundingBox();

        // Calculate new bounding box.
        RotatedRect boundingBox = new RotatedRect(new Point(rectangle.centerX(),rectangle.centerY()),new Size(rectangle.width(),rectangle.height()),angle);

        Log.d(Tag, "Preprocessed one face");

        // Return trimmed face.
        return rotatedImage.submat(boundingBox.boundingRect());
    }

    /**
     * Try to rotate face and trim rest of photo.
     *
     * @param frame         image with many faces which will be preprocessed
     * @param task Rect of this face
     * @return Matrix of all faces after trimming and rotation
     */
    public ArrayList<Mat> preProcessAllFaces(Mat frame, Task<List<Face>> task) {
        // Wait until task is done.
        while(!task.isComplete()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ArrayList<Mat> cutFaces = new ArrayList<>();

        // Rotate each face
        for (Face face : task.getResult()) {
            android.graphics.Rect rectangle = face.getBoundingBox();

            Mat faceImg = frame.submat(new Rect(rectangle.left,rectangle.top,rectangle.width(),rectangle.height()));
            double angle = -face.getHeadEulerAngleZ();
            cutFaces.add(rotateImageByAngle(faceImg, angle, rectangle.centerX(), rectangle.centerY()));
        }

        Log.d(Tag, "Preprocessed all iamges");
        return cutFaces;
    }

    /**
     * Rotate the image relative to the eyes.
     *
     * @param image    image with face to be transformed
     * @param angle Array of rectangles marking eyes. Has to be two-element
     * @return rotated image
     */
    private Mat rotateImageByAngle(Mat image, double angle, int x, int y) {
        int rows = image.rows();
        int cols = image.cols();

        Mat M = getRotationMatrix2D(new Point(x, y), angle, 1);
        Mat imageRot = new Mat();
        warpAffine(image, imageRot, M, new Size(cols, rows));

        Log.d(Tag, "Image rotated");
        return imageRot;
    }
}
