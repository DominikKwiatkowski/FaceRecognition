package com.libs.facerecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.R;
import com.common.FaceProcessingException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.atan2;
import static org.junit.Assert.assertEquals;
import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;
import static org.opencv.imgproc.Imgproc.warpAffine;

public class FacePreProcessor {
    private final CascadeClassifier faceCascade = new CascadeClassifier();
    private final CascadeClassifier eyeCascade = new CascadeClassifier();
    public final String Tag = "ImagePreProcessor";
    private FaceDetector faceDetector;


    public FacePreProcessor(Context context){
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        faceDetector = FaceDetection.getClient(options);

        loadClassifier(R.raw.haarcascade_frontalface_alt2, faceCascade, context);
        loadClassifier(R.raw.haarcascade_eye, eyeCascade, context);
    }
    /**
     * Load Classifier from resources.
     *
     * @param resourceId image classifier id in resources space
     * @param classifier object of classifier to be loaded into
     * @param context - app/activity context
     */
    private void loadClassifier(int resourceId, CascadeClassifier classifier, Context context) {
        try {
            InputStream is = context.getResources().openRawResource(resourceId);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "classifier_file.xml");

            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            classifier.load(cascadeFile.getAbsolutePath());
            Log.i(Tag, "Cascade loaded successfully");
        } catch (IOException e) {
            Log.e(Tag , "Cascade not found");
        }
    }

    /**
     * Detect all faces on given frame.
     *
     * @param frame image on which faces will be found
     * @return MatOfRect matrix of all face rectangular. Face rectangular is beginning point and
     * Size
     */
    public MatOfRect detectAllFacesUsingCascade(Mat frame) {
        Mat frameGray = new Mat();
        Imgproc.cvtColor(frame, frameGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(frameGray, frameGray);
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(frameGray, faces);

        return faces;
    }

    public MatOfRect detectAllFacesUsingML(Mat frame) {
        Bitmap image = Bitmap.createBitmap(frame.cols(),  frame.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame,image);
        InputImage miImage = InputImage.fromBitmap(image,0);
        Task<List<Face>> result = faceDetector.process(miImage);
        ArrayList<Rect> facesRect = new ArrayList<Rect>();
        MatOfRect returnResult = new MatOfRect();
        // TODO: Since it works asynchronously we should change our workflow.
        while(!result.isComplete()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        List<Face> faces = result.getResult();
        for (Face face : faces){
            android.graphics.Rect rectangle = face.getBoundingBox();
            facesRect.add(new Rect(rectangle.left,rectangle.top,rectangle.width(),rectangle.height()));
        }
        returnResult.fromList(facesRect);
        return returnResult;
    }
    /**
     * Try to rotate face and trim rest of photo.
     *
     * @param face image of face which will be preprocessed
     * @return Matrix of preprocessed face
     */
    public Mat preProcessOneFace(Mat face) throws FaceProcessingException {
        Rect[] eyeArray = findEyesOnImg(face);

        if (eyeArray.length == 2) {
            face = rotateImageByEye(face, eyeArray);
        }

        Rect[] faceArray = detectAllFacesUsingCascade(face).toArray();
        if (faceArray.length == 0)
            throw new FaceProcessingException(FaceProcessingException.NO_FACES);
        else if (faceArray.length > 1)
            throw new FaceProcessingException(FaceProcessingException.MORE_THAN_ONE_FACE);

        return face.submat(faceArray[0]);
    }

    /**
     * Try to rotate face and trim rest of photo.
     *
     * @param frame         image with many faces which will be preprocessed
     * @param detectedFaces Rect of this face
     * @return Matrix of all faces after trimming and rotation
     */
    public ArrayList<Mat> preProcessAllFaces(Mat frame, MatOfRect detectedFaces) {
        ArrayList<Mat> cutFaces = new ArrayList<>();

        for (Rect face : detectedFaces.toArray()) {
            Mat faceImg = frame.submat(face);
            Rect[] eyeArray = findEyesOnImg(faceImg);

            if (eyeArray.length != 2) {
                // Sth went wrong, preprocessing should be stopped
                cutFaces.add(faceImg);
            } else {
                cutFaces.add(rotateImageByEye(faceImg, eyeArray));
            }
        }

        return cutFaces;
    }

    /**
     * Rotate the image relative to the eyes.
     *
     * @param image    image with face to be transformed
     * @param eyeArray Array of rectangles marking eyes. Has to be two-element
     * @return rotated image
     */
    private Mat rotateImageByEye(Mat image, Rect[] eyeArray) {
        assertEquals("Wrong number of eyes", 2, eyeArray.length);

        double delta_x = (eyeArray[0].x + eyeArray[0].width) -
                (eyeArray[1].x + eyeArray[1].width);

        double delta_y = (eyeArray[0].y + eyeArray[0].height) -
                (eyeArray[1].y + eyeArray[1].height);

        double angle = atan2(delta_y, delta_x);
        int rows = image.rows();
        int cols = image.cols();

        Mat M = getRotationMatrix2D(new Point(cols / 2, rows / 2), angle, 1);
        Mat imageRot = new Mat();
        warpAffine(image, imageRot, M, new Size(cols, rows));

        return imageRot;
    }

    /**
     * Find eyes on image.
     *
     * @param faceImg image with face, on which eyes will be spotted
     * @return Array of Rect of all eyes on image
     */
    private Rect[] findEyesOnImg(Mat faceImg) {
        Mat faceImgGray = new Mat();
        Imgproc.cvtColor(faceImg, faceImgGray, Imgproc.COLOR_BGR2GRAY);
        MatOfRect eyes = new MatOfRect();
        eyeCascade.detectMultiScale(faceImgGray, eyes);

        return eyes.toArray();
    }
}
