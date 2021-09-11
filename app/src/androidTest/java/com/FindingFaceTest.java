package com;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for detecting faces on images. All result have some margin check, due to
 * different method may be used.
 */
@RunWith(AndroidJUnit4.class)
public class FindingFaceTest {
    // Number of margin percentage
    static final int margin = 10;
    static final int testCases = 2;

    static {
        System.loadLibrary("opencv_java3");
    }

    NeuralModel model;

    /**
     * Function to check if results are correct. WE use margin, which is equal to margin field.
     * It is possible to test only photo with one face on it.
     * @param image image on which we will find faces
     * @param face expected face position
     * @return true if result is correct
     */
    boolean photoIsCorect(Mat image, Rect face) {
        Rect[] result = model.detectAllFaces(image).toArray();

        if (result.length != 1) {
            return false;
        }

        int widthMargin = face.width / margin;
        int heightMargin = face.height / margin;

        if (face.width - widthMargin > result[0].width) {
            return false;
        }
        if (face.width + widthMargin < result[0].width) {
            return false;
        }
        if (face.x - widthMargin > result[0].x) {
            return false;
        }
        if (face.x + widthMargin < result[0].x) {
            return false;
        }
        if (face.height - heightMargin > result[0].height) {
            return false;
        }
        if (face.height + heightMargin < result[0].height) {
            return false;
        }
        if (face.y - heightMargin > result[0].y) {
            return false;
        }
        return face.y + heightMargin >= result[0].y;
    }

    @Test
    /**
     * Check if face detector find face on image and position of face is close where it should be.
     */
    public void performTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Name of model does not have any impact on results, it is never used.
        model = NeuralModel.getInstance(appContext, "Facenet-optimized.tflite");

        // Expected Results
        Rect[] faces = new Rect[testCases];
        faces[0] = new Rect(140, 170, 691, 800);
        faces[1] = new Rect(48, 33, 74, 74);

        // Load images.
        Mat[] images = new Mat[testCases];
        try {
            images[0] = Utils.loadResource(appContext, R.drawable.face1);
            images[1] = Utils.loadResource(appContext, R.drawable.face2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Test checks.
        assertTrue(OpenCVLoader.initDebug());
        for (int i = 0; i < testCases; i++) {
            assertTrue(photoIsCorect(images[i], faces[i]));
        }
    }
}