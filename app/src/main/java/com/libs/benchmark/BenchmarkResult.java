package com.libs.benchmark;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;

public class BenchmarkResult {
    private final Bitmap photo;
    private HashMap<String, String> modelResult = new HashMap<>();
    public final String name;

    public BenchmarkResult(Bitmap photo, String filename) {
        this.photo = photo;
        this.name = filename;
    }

    /**
     * Add result of model from this photo.
     *
     * @param neuralModelName - name of model witch gives result
     * @param name            - result of face recognition
     */
    public synchronized void addResult(String neuralModelName, String name) {
        modelResult.put(neuralModelName, name);
    }

    /**
     * Get HashMap(model,result)
     *
     * @return results from this photo
     */
    public HashMap<String, String> getResults() {
        return modelResult;
    }

    /**
     * Get photo for which results are stored in this class.
     *
     * @return face photo
     */
    public Bitmap getPhoto() {
        return photo;
    }

}
