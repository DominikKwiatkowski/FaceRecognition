package com.libs.globaldata;

import android.content.Context;
import android.content.res.Resources;

import com.R;
import com.libs.facerecognition.NeuralModel;
import com.libs.facerecognition.NeuralModelProvider;
import com.libs.globaldata.userdatabase.Metric;
import com.libs.globaldata.userdatabase.UserDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ModelObject {
    public final NeuralModel neuralModel;
    public final UserDatabase userDatabase;

    /**
     * ModelObject constructor. Depending on the type, acquires proper neural network model.
     * Always creates new database with the unique filename assigned.
     *
     * @param context       - app/activity context
     * @param modelName     - name of the neural network model
     */
    ModelObject(Context context, String modelName, String databaseName) {
        neuralModel = NeuralModelProvider.getInstance(context, modelName);

        String[] models = context.getResources().getStringArray(R.array.models);
        String[] metrics = context.getResources().getStringArray(R.array.metrics);
        String[] thresholds = context.getResources().getStringArray(R.array.threshold);
        for(int i = 0; i < models.length; i++){
            if(models[i].equals(modelName)){
                Metric metric = Metric.valueOf(metrics[i].toUpperCase());
                float threshold = Float.parseFloat(thresholds[i]);
                userDatabase = new UserDatabase(context, databaseName, neuralModel.getOutputSize(),
                        true, metric, threshold);
                return;
            }
        }
        throw new AssertionError("Metrics or threshold not specified for " + modelName);
    }

    /**
     * Remove database of the ModelObject.
     */
    public void clear() {
        userDatabase.clear();
    }
}
