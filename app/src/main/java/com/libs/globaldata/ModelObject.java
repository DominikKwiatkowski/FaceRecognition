package com.libs.globaldata;

import android.content.Context;

import com.libs.facerecognition.NeuralModel;
import com.libs.facerecognition.models.FaceNetModel;
import com.libs.globaldata.userdatabase.UserDatabase;

public class ModelObject {
    public final NeuralModel neuralModel;
    public final UserDatabase userDatabase;

    /**
     * ModelObject constructor. Depending on the type, acquires proper neural network model.
     * Always creates new database with the unique filename assigned.
     *
     * @param modelType - type of the neural network
     * @param context   - app/activity context
     */
    ModelObject(ModelType modelType, Context context) {
        switch (modelType) {
            case FACENET_USER:
                neuralModel = FaceNetModel.getInstance(context);
                userDatabase = new UserDatabase(context, neuralModel.Tag + "_user", neuralModel.getOutputSize());
                break;
            case FACENET_BENCHMARK:
                neuralModel = FaceNetModel.getInstance(context);
                userDatabase = new UserDatabase(context, neuralModel.Tag + "_benchmark", neuralModel.getOutputSize());
                break;
            default:
                neuralModel = null;
                userDatabase = null;
                throw new AssertionError("Incorrect model type.");
        }
    }

    /**
     * Remove database of the ModelObject.
     */
    public void clear() {
        userDatabase.clearFile();
    }
}
