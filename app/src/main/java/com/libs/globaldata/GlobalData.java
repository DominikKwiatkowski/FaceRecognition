package com.libs.globaldata;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class GlobalData {
    private final static Map<String, ModelObject> modelsStorage = new HashMap<>();

    /**
     * Singleton model instance getter. Initializes ModelObject instance if not initialized earlier.
     * Returns static NeuralModel instance.
     *
     * @param context       - app/activity context
     * @param modelName     - name of the neural network model
     * @param modelFilename - filename of the neural network model
     * @return instance - singleton ModelObject instance
     */
    public static ModelObject getModel(Context context, String modelName, String modelFilename) {
        ModelObject model = modelsStorage.get(modelName);

        if (model == null) {
            synchronized (ModelObject.class) {
                if (model == null) {
                    model = new ModelObject(context, modelName, modelFilename);
                    modelsStorage.put(modelName, model);
                }
            }
        }

        return model;
    }

    /**
     * Remove given ModelObject with it's database if exists.
     *
     * @param modelName - name of the neural network model
     * @param context   - app/activity context
     */
    public static void clearModel(String modelName, Context context) {
        ModelObject model = modelsStorage.get(modelName);

        if (model != null) {
            synchronized (ModelObject.class) {
                if (model != null) {
                    model.clear();
                    modelsStorage.remove(modelName);
                }
            }
        }
    }
}
