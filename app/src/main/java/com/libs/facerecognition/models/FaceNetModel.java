package com.libs.facerecognition.models;

import android.content.Context;

import com.R;
import com.libs.facerecognition.NeuralModel;

public class FaceNetModel extends NeuralModel {
    private static FaceNetModel instance = null;

    FaceNetModel (Context context) {
        super("FaceNetModel", context.getResources().getString(R.string.model_Facenet), context);
    }

    /**
     * Singleton instance getter. Initializes FaceNetModel instance if not initialized earlier.
     * Returns static NeuralModel instance.
     *
     * @param context - app/activity context
     * @return instance - singleton FaceNetModel instance
     */
    public static FaceNetModel getInstance(Context context) {
        if (instance == null) {
            synchronized (FaceNetModel.class) {
                if (instance == null) {
                    instance = new FaceNetModel(context);
                }
            }
        }

        return instance;
    }
}
