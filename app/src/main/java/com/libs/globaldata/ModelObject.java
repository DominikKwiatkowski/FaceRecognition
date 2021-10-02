package com.libs.globaldata;

import android.content.Context;

import com.libs.facerecognition.NeuralModel;
import com.libs.facerecognition.NeuralModelProvider;
import com.libs.globaldata.userdatabase.UserDatabase;

public class ModelObject {
    public final NeuralModel neuralModel;
    public final UserDatabase userDatabase;

    /**
     * ModelObject constructor. Depending on the type, acquires proper neural network model.
     * Always creates new database with the unique filename assigned.
     *
     * @param context       - app/activity context
     * @param modelName     - name of the neural network model
     * @param modelFilename - filename of the neural network model
     */
    ModelObject(Context context, String modelName, String modelFilename) {
        neuralModel = NeuralModelProvider.getInstance(context, modelName, modelFilename);
        userDatabase = new UserDatabase(context, modelName, neuralModel.getOutputSize());
    }

    /**
     * Remove database of the ModelObject.
     */
    public void clear() {
        userDatabase.clearFile();
    }
}
