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
import com.libs.facerecognition.FacePreprocessor;
import com.libs.globaldata.GlobalData;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for detecting faces on images. All results have some margin due to the possibility
 * of using different methods.
 */
@RunWith(AndroidJUnit4.class)
public class PreProcessorTest {

    // Number of margin percentage
    private static final int margin = 10;
    private static final int testCases = 2;

    private FacePreprocessor preProcessor;
    private Resources res;
    private Context appContext;

    public PreProcessorTest()
    {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        res = appContext.getResources();
        // Name of model does not have any impact on results, it is never used.
        preProcessor = GlobalData.getFacePreProcessor();
    }
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
     * Check if face detector found the face on the image and if position of face is close to where
     * it should be.
     */
    public void FindFaceTest() {

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

    @Test
    /**
     * Testing if face on image works correctly.
     */
    public void IsFaceOnImageTest(){
        Bitmap faceNotInImagePhoto = BitmapFactory.decodeResource(res, R.drawable.face_not_in_image);
        Bitmap faceInImagePhoto = BitmapFactory.decodeResource(res, R.drawable.face_in_image);

        Task<List<Face>> faceOutsideImage = preProcessor.detectAllFacesUsingML(faceNotInImagePhoto);
        Task<List<Face>> faceInsideImage = preProcessor.detectAllFacesUsingML(faceInImagePhoto);

        preProcessor.waitForTask(faceOutsideImage);
        preProcessor.waitForTask(faceInsideImage);

        assertFalse(preProcessor.isFaceOnImage(faceOutsideImage.getResult().get(0), faceNotInImagePhoto));
        assertTrue(preProcessor.isFaceOnImage(faceInsideImage.getResult().get(0), faceInImagePhoto));
    }

    @Test
    /**
     * Check rotate and trim face works correctly.
     */
    public void rotateAndTrimFace(){
        Bitmap faceInImagePhoto = BitmapFactory.decodeResource(res, R.drawable.face_in_image);
        Task<List<Face>> faceInsideImage = preProcessor.detectAllFacesUsingML(faceInImagePhoto);
        preProcessor.waitForTask(faceInsideImage);

        Bitmap trimmed = preProcessor.rotateAndTrimFace(faceInImagePhoto, faceInsideImage.getResult().get(0));
        Bitmap expectedTrimmed = BitmapFactory.decodeResource(res, R.drawable.face_trimmed);
        equals(trimmed,expectedTrimmed);
    }

    private boolean equals(Bitmap bitmap1, Bitmap bitmap2) {
        ByteBuffer buffer1 = ByteBuffer.allocate(bitmap1.getHeight() * bitmap1.getRowBytes());
        bitmap1.copyPixelsToBuffer(buffer1);

        ByteBuffer buffer2 = ByteBuffer.allocate(bitmap2.getHeight() * bitmap2.getRowBytes());
        bitmap2.copyPixelsToBuffer(buffer2);

        return Arrays.equals(buffer1.array(), buffer2.array());
    }
}