package com.libs.facerecognition;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.common.FaceProcessingException;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;

import static android.graphics.Bitmap.createBitmap;

/**
 * Class for all operations before image is put into face recognition. It provides method to detect
 * faces and trim faces.
 */
public class FacePreprocessor {
    public final String Tag = "ImagePreProcessor";
    private final FaceDetector faceDetector;
    private final int FaceMargin = 10;
    /**
     * Create pre processor instance. Build google face detector.
     */
    public FacePreprocessor() {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        faceDetector = FaceDetection.getClient(options);
    }

    /**
     * Detect face, rotate it and trim.
     *
     * @param image image of face which will be preprocessed
     * @return Matrix of preprocessed face
     */
    public Bitmap detectAndPreProcessOneFace(Bitmap image) throws FaceProcessingException {
        Task<List<Face>> task = detectAllFacesUsingML(image);
        waitForTask(task);
        // Check number of detected faces.
        List<Face> faces = task.getResult();
        if (faces.size() == 0)
            throw new FaceProcessingException(FaceProcessingException.NO_FACES);
        else if (faces.size() > 1)
            throw new FaceProcessingException(FaceProcessingException.MORE_THAN_ONE_FACE);

        // Rotate faces.
        Face face = faces.get(0);

        return rotateAndTrimFace(image, face);
    }

    /**
     * Detect all faces on given frame. This function will use Google Ml Kit face detector.
     *
     * @param frame image on which faces will be found
     * @return Task<List < Face>> task which will detect faces. When task ends, we can get all
     * detected faces.
     */
    public Task<List<Face>> detectAllFacesUsingML(Bitmap frame) {
        InputImage miImage = InputImage.fromBitmap(frame, 0);
        Task<List<Face>> result = faceDetector.process(miImage);

        Log.d(Tag, "Face detection started");
        return result;
    }

    /**
     * Wait until given task has ended.
     *
     * @param task detect faces task for which we need to wait.
     */
    public void waitForTask(Task<List<Face>> task) {
        while (!task.isComplete()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Rotate the image by given angle.
     *
     * @param image Image with face to be transformed
     * @param face  Face from face detection
     * @return rotated image
     */
    private Bitmap rotateAndTrimFace(Bitmap image, Face face) {
        double angle = face.getHeadEulerAngleZ();
        Matrix rotationMatrix = new Matrix();

        // Create rotation matrix
        rotationMatrix.setRotate((float) angle,
                (float) image.getWidth() / 2,
                (float) image.getHeight() / 2);

        // Rotate image
        Bitmap rotatedImage = createBitmap(image,
                0,
                0,
                image.getWidth(),
                image.getHeight(),
                rotationMatrix,
                false);

        // Calculate new bounding box center coordinate
        int newBoundingBoxCenterX = face.getBoundingBox().centerX() * rotatedImage.getWidth() / image.getWidth();
        int newBoundingBoxCenterY = face.getBoundingBox().centerY() * rotatedImage.getHeight() / image.getHeight();

        // Create moved bounding box
        RectF boundingBoxF = new RectF(
                face.getBoundingBox().left + newBoundingBoxCenterX - face.getBoundingBox().centerX(),
                face.getBoundingBox().top + newBoundingBoxCenterY - face.getBoundingBox().centerY(),
                face.getBoundingBox().right + newBoundingBoxCenterX - face.getBoundingBox().centerX(),
                face.getBoundingBox().bottom + newBoundingBoxCenterY - face.getBoundingBox().centerY()
        );

        // Set new rotation matrix and apply it
        rotationMatrix.setRotate(
                (float) angle,
                (float) rotatedImage.getWidth() / 2,
                (float) rotatedImage.getHeight() / 2);

        rotationMatrix.mapRect(boundingBoxF);

        // Map result to int( we can't cut half of the pixel)
        Rect rotatedBox = new Rect();
        boundingBoxF.roundOut(rotatedBox);

        // For some reason after rotation bounding box have different size, it will fix it
        int xCordScale = (rotatedBox.width() - face.getBoundingBox().width()) / 2;
        int yCordScale = (rotatedBox.height() - face.getBoundingBox().height()) / 2;
        rotatedBox.set(
                rotatedBox.left + xCordScale,
                rotatedBox.top + yCordScale,
                rotatedBox.right - xCordScale,
                rotatedBox.bottom - yCordScale
        );

        Log.d(Tag, "Rotated and trimmed face");

        // Trim and return preprocessed face.
        return createBitmap(rotatedImage, rotatedBox.left, rotatedBox.top, rotatedBox.width(), rotatedBox.height());
    }

    /**
     * Try to rotate face and trim rest of photo.
     *
     * @param frame       image with many faces which will be preprocessed
     * @param listOfFaces List of all faces on this image.
     * @return Matrix of all faces after trimming and rotation
     */
    public List<Bitmap> preProcessAllFaces(Bitmap frame, List<Face> listOfFaces) {
        List<Bitmap> cutFaces = new ArrayList<>();

        for (Face face : listOfFaces) {
            // Check if face is on image.
            if(face.getBoundingBox().top<=FaceMargin ||
            face.getBoundingBox().left<=FaceMargin ||
            face.getBoundingBox().bottom>=frame.getHeight()-FaceMargin ||
            face.getBoundingBox().right >= frame.getWidth()-FaceMargin) {
                cutFaces.add(null);
            }
            else {
                cutFaces.add(rotateAndTrimFace(frame, face));
            }
        }

        Log.d(Tag, "Preprocessed all images");
        return cutFaces;
    }
}
