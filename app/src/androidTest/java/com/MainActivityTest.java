package com;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.common.FaceProcessingException;
import com.common.VectorOperations;
import com.libs.facerecognition.FacePreprocessor;
import com.libs.facerecognition.NeuralModel;
import com.libs.globaldata.GlobalData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Class to test recognition of faces.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    /**
     * Function check if face recognition work correctly. It loads 3 faces, 2 of them are same
     * person. For each face it takes vector from neural network. Check if vectors of same person
     * are closer then vector of strange one.
     */
    @Test
    public void performTest() throws FaceProcessingException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Resources res = appContext.getResources();

        final int numOfPhotos = 3;
        float[][] result = new float[numOfPhotos][];
        Bitmap[] photos = new Bitmap[numOfPhotos];
        SharedPreferences userSettings = GlobalData.getUserSettings(appContext);

        NeuralModel model = GlobalData.getModel(appContext,
                userSettings.getString(
                        res.getString(R.string.settings_userModel_key),
                        res.getStringArray(R.array.models)[0]),
                userSettings.getString(
                        res.getString(R.string.settings_userModel_key),
                        res.getStringArray(R.array.models)[0]))
                .neuralModel;

        FacePreprocessor facePreProcessor = GlobalData.getFacePreProcessor();
        // Load images. Image 2 and 3 are images of same person.

        photos[0] = BitmapFactory.decodeResource(res, R.drawable.face1);
        photos[1] = BitmapFactory.decodeResource(res, R.drawable.face2);
        photos[2] = BitmapFactory.decodeResource(res, R.drawable.face3);


        // Process and proceed all test photos
        for (int i = 0; i < photos.length; i++) {
            Bitmap faceImage = facePreProcessor.preProcessOneFace(photos[i]);
            TensorImage image = model.changeImageRes(faceImage);
            result[i] = model.processImage(image)[0];
        }

        // Check if same person is closer than other one
        assertTrue(VectorOperations.euclideanDistance(result[0], result[1]) >
                VectorOperations.euclideanDistance(result[1], result[2]));
        assertTrue(VectorOperations.euclideanDistance(result[0], result[2]) >
                VectorOperations.euclideanDistance(result[1], result[2]));
    }
}
