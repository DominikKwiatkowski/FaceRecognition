package com.libs.globaldata;

import android.content.Context;
import android.content.SharedPreferences;

import com.R;

import org.junit.runner.manipulation.Ordering;

import java.util.HashMap;
import java.util.Map;

public class GlobalData {
    private final static Map<String, ModelObject> modelsStorage = new HashMap<>();
    private static SharedPreferences userSettings;

    /**
     * Singleton model instance getter. Initializes ModelObject instance if not initialized earlier.
     * Returns static NeuralModel instance.
     *
     * @param context       - app/activity context
     * @param modelName     - name of the neural network model
     * @return instance - singleton ModelObject instance
     */
    public static ModelObject getModel(Context context, String modelName, String databaseName) {
        ModelObject model = modelsStorage.get(modelName);

        if (model == null) {
            synchronized (ModelObject.class) {
                if (model == null) {
                    model = new ModelObject(context, modelName, databaseName);
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

    /**
     * Get All user settings defined in SharedPreferences.
     *
     * @param context - app/activity context
     * @return userSettings - preferences with all user settings
     */
    public static SharedPreferences getUserSettings(Context context){
        synchronized (ModelObject.class) {
            if (userSettings == null) {
                userSettings = context.getSharedPreferences(context.getResources().getString(R.string.settings_userName), Context.MODE_PRIVATE);
            }
        }
        return userSettings;
    }
}
