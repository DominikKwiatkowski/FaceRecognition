package com.libs.globaldata;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class GlobalData {
    private final static Map<ModelType, ModelObject> modelsStorage = new HashMap<>();

    /**
     * Singleton model instance getter. Initializes ModelObject instance if not initialized earlier.
     * Returns static NeuralModel instance.
     *
     * @param modelType - type of the neural network
     * @param context   - app/activity context
     * @return instance - singleton ModelObject instance
     */
    public static ModelObject getModel(ModelType modelType, Context context) {
        ModelObject model = modelsStorage.get(modelType);

        if (model == null) {
            synchronized (ModelObject.class) {
                if (model == null) {
                    model = new ModelObject(modelType, context);
                    modelsStorage.put(modelType, model);
                }
            }
        }

        return model;
    }

    /**
     * Remove given ModelObject with it's database if exists.
     *
     * @param modelType - type of the neural network
     * @param context   - app/activity context
     */
    public static void clearModel(ModelType modelType, Context context) {
        ModelObject model = modelsStorage.get(modelType);

        if (model != null) {
            synchronized (ModelObject.class) {
                if (model != null) {
                    model.clear();
                    modelsStorage.remove(modelType);
                }
            }
        }
    }
}
