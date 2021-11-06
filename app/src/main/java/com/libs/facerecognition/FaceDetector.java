package com.libs.facerecognition;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.libs.globaldata.GlobalData;

import java.util.List;


public class FaceDetector {
    private final FacePreprocessor facePreprocessor;
    private Task<List<Face>> detectFaceTask = null;
    private List<Face> lastCollectedFaces = null;

    public FaceDetector() {
        facePreprocessor = GlobalData.getFacePreProcessor();
    }

    /**
     * Asynchronously detect faces on image. This method put result into currentFaces field.
     *
     * @param inputFrame
     * @return list of last collected faces. If no faces were detected, return null.
     */
    public List<Face> detectFaces(Bitmap inputFrame) {
        // Collect results if ready
        if (detectFaceTask != null && detectFaceTask.isComplete()) {
            lastCollectedFaces = detectFaceTask.getResult();
        }

        // If no task assigned, start new one
        if (detectFaceTask == null || detectFaceTask.isComplete()) {
            detectFaceTask = facePreprocessor.detectAllFacesUsingML(inputFrame);
        }

        return lastCollectedFaces;
    }
}
