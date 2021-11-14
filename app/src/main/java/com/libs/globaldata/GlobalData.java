package com.libs.globaldata;

import android.content.Context;
import android.content.SharedPreferences;

import com.R;
import com.libs.facerecognition.FacePreprocessor;

import java.util.HashMap;
import java.util.Map;

public class GlobalData {
    private final static Map<String, ModelObject> modelsStorage = new HashMap<>();
    private static SharedPreferences userSettings;
    private static FacePreprocessor facePreProcessor;

    /**
     * Singleton model instance getter. Initializes ModelObject instance if not initialized earlier.
     * Returns static NeuralModel instance.
     *
     * @param context       - app/activity context
     * @param modelName     - name of the neural network model
     * @param databaseName  - name of database, it must be unique
     * @return instance - singleton ModelObject instance
     */
    public static ModelObject getModel(Context context, String modelName, String databaseName) {
        ModelObject model;
        synchronized (ModelObject.class) {
            model = modelsStorage.get(databaseName);
            if (model == null) {
                model = new ModelObject(context, modelName, databaseName);
                modelsStorage.put(databaseName, model);
            }
        }
        return model;
    }

    /**
     * Remove given ModelObject with it's database if exists.
     *
     * @param modelName     - name of the neural network model
     * @param databaseName  - name of database, it must be unique
     */
    public static void clearModel(String modelName, String databaseName) {
        ModelObject model;

        synchronized (ModelObject.class) {
            model = modelsStorage.get(databaseName);
            if (model != null) {
                model.clear();
                modelsStorage.remove(databaseName);
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
        synchronized (SharedPreferences.class) {
            if (userSettings == null) {
                userSettings = context.getSharedPreferences(context.getResources().getString(R.string.settings_userName), Context.MODE_PRIVATE);
            }
        }
        return userSettings;
    }

    /**
     * Get image preprocessor.
     *
     * @return facePreProcessor - defined pre processor of app.
     */
    public static FacePreprocessor getFacePreProcessor(){
        synchronized (FacePreprocessor.class) {
            if (facePreProcessor == null) {
                facePreProcessor = new FacePreprocessor();
            }
        }
        return facePreProcessor;
    }
}
