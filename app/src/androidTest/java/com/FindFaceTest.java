package com;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.libs.facerecognition.FacePreProcessor;
import com.libs.globaldata.GlobalData;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private FacePreProcessor preProcessor;

    /**
     * Function to check if results are correct. We use margin, which is equal to margin field.
     * It is only possible to test photo with one face on it.
     *
     * @param image image to find faces on
     * @param face  expected face position
     * @return true if result is correct
     */
    boolean photoIsCorrect(Bitmap image, Rect face) {
        Task<List<Face>> task = preProcessor.detectAllFacesUsingML(image);
        while(!task.isComplete())
        {

        }
        ArrayList<Rect> result = new ArrayList<>();
        for(Face faceRes : task.getResult()){
            result.add(faceRes.getBoundingBox());
        }

        if (result.size() != 1) {
            return false;
        }

        int widthMargin = face.width() / margin;
        int heightMargin = face.height() / margin;

        if (face.width() - widthMargin > result.get(0).width()) {
            return false;
        }
        if (face.width() + widthMargin < result.get(0).width()) {
            return false;
        }
        if (face.left - widthMargin > result.get(0).left) {
            return false;
        }
        if (face.left + widthMargin < result.get(0).left) {
            return false;
        }
        if (face.height() - heightMargin > result.get(0).height()) {
            return false;
        }
        if (face.height() + heightMargin < result.get(0).height()) {
            return false;
        }
        if (face.top - heightMargin > result.get(0).top) {
            return false;
        }
        return face.top + heightMargin >= result.get(0).top;
    }

    @Test
    /**
     * Check if face detector found the face on the image and if position of face is close to where it should be.
     */
    public void performTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Resources res = appContext.getResources();
        // Name of model does not have any impact on results, it is never used.
        preProcessor = GlobalData.getFacePreProcessor();

        // Expected Results
        Rect[] faces = new Rect[testCases];
        faces[0] = new Rect(396, 519, 2317, 2400);
        faces[1] = new Rect(118, 86, 320, 288);

        // Load images.
        Bitmap[] images = new Bitmap[testCases];

        images[0] = BitmapFactory.decodeResource(res, R.drawable.face1);
        images[1] = BitmapFactory.decodeResource(res, R.drawable.face2);


        for (int i = 0; i < testCases; i++) {
            assertTrue(photoIsCorrect(images[i], faces[i]));
        }
    }
}