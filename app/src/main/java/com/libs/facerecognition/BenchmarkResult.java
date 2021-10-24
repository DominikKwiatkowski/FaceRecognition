package com.libs.facerecognition;

import android.graphics.Bitmap;

import java.util.HashMap;

public class BenchmarkResult {
    private Bitmap photo;
    private HashMap<String, String> modelResult = new HashMap<>();

    public BenchmarkResult(Bitmap photo){
        this.photo = photo;
    }

    public synchronized void addResult(String neuralModelName, String name){
        modelResult.put(neuralModelName,name);
    }

    public HashMap<String, String> getResults(){
        return modelResult;
    }

    public Bitmap getPhoto() {
        return photo;
    }
}
