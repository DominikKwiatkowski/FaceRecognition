package com;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.libs.facerecognition.NeuralModel;
import com.libs.globaldata.GlobalData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for detecting faces on images. All results have some margin due to the possibility
 * of using different methods.
 */
@RunWith(AndroidJUnit4.class)
public class FindFaceTest {

    // Number of margin percentage
    private static final int margin = 10;
    private static final int testCases = 2;

    static {
        System.loadLibrary("opencv_java3");
    }

    private NeuralModel model;

    /**
     * Function to check if results are correct. We use margin, which is equal to margin field.
     * It is only possible to test photo with one face on it.
     *
     * @param image image to find faces on
     * @param face  expected face position
     * @return true if result is correct
     */
    boolean photoIsCorrect(Mat image, Rect face) {
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
     * Check if face detector found the face on the image and if position of face is close to where it should be.
     */
    public void performTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Resources res = appContext.getResources();
        // Name of model does not have any impact on results, it is never used.
        model = GlobalData.getModel(appContext,
                GlobalData.getUserSettings(appContext).getString(
                        res.getString(R.string.user_Settings_user_model_key),
                        res.getStringArray(R.array.models)[0]),
                GlobalData.getUserSettings(appContext).getString(
                        res.getString(R.string.user_Settings_user_model_key),
                        res.getStringArray(R.array.models)[0]))
                .neuralModel;

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
            assertTrue(photoIsCorrect(images[i], faces[i]));
        }
    }
}