package com;

import static org.junit.Assert.assertEquals;
import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;
import static org.opencv.imgproc.Imgproc.warpAffine;

import static java.lang.Math.atan2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/** Represents neural model. User can define which one will be used. */
public class NeuralModel {
    private final int inputSize = 160;
    private final int outputSize = 128;

    private final String nameOfModel;
    private final String TAG = "NeuralModelClass ";
    private final CascadeClassifier faceCascade = new CascadeClassifier();
    private final CascadeClassifier eyeCascade = new CascadeClassifier();
    Resources res;
    ImageProcessor imageProcessor =
            new ImageProcessor.Builder()
                    .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(127.5f, 127.5f))
                    .build();
    Context context;
    private Interpreter model;

    /**
     * Class constructor.
     *
     * @param context actual Activity
     * @param nameOfModel name of model to be used. All models must be inside ml folder
     */
    public NeuralModel(Context context, String nameOfModel) {
        this.context = context;
        this.nameOfModel = nameOfModel;

        try {
            model = new Interpreter(FileUtil.loadMappedFile(context, this.nameOfModel));
        } catch (IOException e) {
            Log.e(TAG + nameOfModel, "Error reading model", e);
        }

        res = context.getResources();

        loadClassifier(R.raw.haarcascade_frontalface_alt2, faceCascade);
        loadClassifier(R.raw.haarcascade_eye, eyeCascade);
    }

    /**
     * Prepare image to be putted inside neural network. It will change resolution and format of
     * given image to resolution needed by neural network.
     *
     * @param image image to be prepared
     * @return tImage image processed and ready to be putted by neural network.
     * @throws NullPointerException in case of null image
     */
    public TensorImage changeImageRes(Mat image) {
        if (image == null) {
            throw new NullPointerException("Image can't be null");
        }

        TensorImage tImage = new TensorImage(DataType.UINT8);
        Bitmap bitmap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        tImage.load(bitmap);
        tImage = imageProcessor.process(tImage);

        return tImage;
    }

    /**
     * Receive target image and serialize it to image's vector using neural network model.
     *
     * @param tImage image to be processed by network. It must be preprocessed
     * @return probabilityBuffer Buffer of face properties. Sized of buffer must be specified
     * @throws NullPointerException in case of null image
     */
    public float[][] processImage(TensorImage tImage) {
        if (tImage == null) {
            throw new NullPointerException("Image can't be null");
        }

        float[][] probabilityBuffer = new float[1][outputSize];
        model.run(tImage.getBuffer(), probabilityBuffer);
        Log.i(TAG + nameOfModel, "proceeded image successfully");

        return probabilityBuffer;
    }

    /**
     * Load Classifier from resources.
     *
     * @param resourceId image classifier id in resources space
     * @param classifier object of classifier to be loaded into
     */
    private void loadClassifier(int resourceId, CascadeClassifier classifier) {
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
            Log.i(TAG + nameOfModel, "Cascade loaded successfully");

        } catch (IOException e) {
            Log.e(TAG + nameOfModel, "Cascade not found");
        }
    }

    /**
     * Detect all faces on given frame.
     *
     * @param frame image on which faces will be found
     * @return MatOfRect matrix of all face rectangular. Face rectangular is beginning point and
     *     Size
     */
    public MatOfRect detectAllFaces(Mat frame) {
        Mat frameGray = new Mat();
        Imgproc.cvtColor(frame, frameGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(frameGray, frameGray);
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(frameGray, faces);

        return faces;
    }

    /**
     * Try to rotate face and trim rest of photo.
     *
     * @param face image of face which will be preprocessed
     * @param detectedFaces Rect of this face
     * @return Matrix of preprocessed face
     */
    public Mat preProcessOneFace(Mat face, Rect detectedFaces) {
        Rect[] eyeArray = findEyesOnImg(face);

        if (eyeArray.length == 2) {
            face = rotateImageByEye(face, eyeArray);
        }

        Rect[] faceArray = detectAllFaces(face).toArray();
        assertEquals("Wrong image, more than 1 face", 1, faceArray.length);

        return face.submat(faceArray[0]);
    }

    /**
     * Try to rotate face and trim rest of photo.
     *
     * @param frame image with many faces which will be preprocessed
     * @param detectedFaces Rect of this face
     * @return Matrix of all faces after trimming and rotatiton
     */
    public ArrayList<Mat> preProcessAllFaces(Mat frame, MatOfRect detectedFaces) {
        ArrayList<Mat> cutFaces;
        cutFaces = new ArrayList();

        for (Rect face : detectedFaces.toArray()) {
            Mat faceImg = frame.submat(face);
            Rect[] eyeArray = findEyesOnImg(faceImg);

            if (eyeArray.length != 2) {
                // Sth go wrong, we should stop preprocessing
                cutFaces.add(faceImg);
            } else {
                cutFaces.add(rotateImageByEye(faceImg, eyeArray));
            }
        }

        return cutFaces;
    }

    /**
     * Rotate image by eye.
     *
     * @param image image with face which will rotated
     * @param eyeArray Array of eyes. It has to have 2 elements
     * @return image after rotation
     */
    private Mat rotateImageByEye(Mat image, Rect[] eyeArray) {
        assertEquals("Wrong number of eyes", 2, eyeArray.length);

        double delta_x = (eyeArray[0].x + eyeArray[0].width) - (eyeArray[1].x + eyeArray[1].width);

        double delta_y =
                (eyeArray[0].y + eyeArray[0].height) - (eyeArray[1].y + eyeArray[1].height);

        double angle = atan2(delta_y, delta_x);
        int rows = image.rows();
        int cols = image.cols();

        Mat M = getRotationMatrix2D(new Point(cols / 2, rows / 2), angle, 1);
        Mat imageRot = new Mat();
        warpAffine(image, imageRot, M, new Size(cols, rows));

        return imageRot;
    }

    /**
     * Find all eyes on image.
     *
     * @param faceImg image with face on which eyes will be spotted
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
