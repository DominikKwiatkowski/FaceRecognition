package com.libs.facerecognition;

import android.graphics.Bitmap;

import com.google.mlkit.vision.face.Face;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.userdatabase.UserDatabase;
import com.libs.globaldata.userdatabase.UserRecord;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FaceRecognizer {
    private final NeuralModel model;
    private final UserDatabase userDatabase;

    private final Executor recognizeThreadExecutor = Executors.newSingleThreadExecutor();
    private final FacePreprocessor facePreprocessor;

    private CompletableFuture<String[]> recognizeFaceTask = null;
    private Bitmap currentInputFrame = null;
    private List<Face> currentDetectedFaces = null;

    public FaceRecognizer(NeuralModel model, UserDatabase userDatabase) {
        this.facePreprocessor = GlobalData.getFacePreProcessor();
        this.model = model;
        this.userDatabase = userDatabase;
    }

    /**
     * Asynchronously recognise face on images. Collects last frame and detected faces
     * from class fields.
     *
     * @return list of recognised names. If no names were recognised, return null.
     */
    public String[] recogniseFaces(Bitmap inputFrame, List<Face> detectedFaces) {
        String[] currentNames = null;

        // Check if there are faces to be recognised
        if (inputFrame != null && detectedFaces != null && !detectedFaces.isEmpty()) {
            // Collect results if ready
            if (recognizeFaceTask != null && recognizeFaceTask.isDone()) {
                try {
                    currentNames = recognizeFaceTask.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // If no task assigned, start new one
            if (recognizeFaceTask == null || recognizeFaceTask.isDone()) {
                currentInputFrame = inputFrame;
                currentDetectedFaces = detectedFaces;

                recognizeFaceTask = CompletableFuture.supplyAsync(this::performFaceRecognition,
                        recognizeThreadExecutor);
            }
        }

        return currentNames;
    }

    /**
     * Recognise all faces stored in class params.
     *
     * @return Names of all recognised users.
     */
    private String[] performFaceRecognition() {
        // From inputFrame and detectedFaces generate images per each face
        List<Bitmap> faceImages = facePreprocessor.preProcessAllFaces(
                currentInputFrame,
                currentDetectedFaces);

        String[] newNames = null;

        if (faceImages != null && faceImages.size() > 0) {
            newNames = new String[faceImages.size()];

            // Calculate vector for each face
            for (int i = 0; i < faceImages.size(); i++) {
                float[] result = null;
                try {
                    result = model.resizeAndProcess(faceImages.get(i));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } finally {
                    // Find closest user in database.
                    // TODO: Add threshold to avoid detection of non-existing users
                    UserRecord closestUser = userDatabase.findClosestRecordBelowThreshold(result);
                    if (closestUser != null) {
                        newNames[i] = closestUser.username;
                    } else {
                        newNames[i] = "";
                    }
                }
            }
        }

        return newNames;
    }
}
