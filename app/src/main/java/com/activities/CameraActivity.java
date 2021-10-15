package com.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.libs.facerecognition.FacePreProcessor;
import com.libs.facerecognition.NeuralModel;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;
import com.libs.globaldata.userdatabase.UserDatabase;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.opencv.core.Core.FONT_HERSHEY_SIMPLEX;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private Executor recognizeThreadExecutor = Executors.newSingleThreadExecutor();
    private int CameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Button takePhotoButton;
    private boolean saveNextFrame = false;
    private final String Tag = "CameraActivity";
    private CompletableFuture<String[]> recognizedFacesCompletableFuture = null;
    private Mat currentDetectedFrame = null;
    private String[] currentNames = null;
    private Task<List<Face>> oldFaces = null;
    private Task<List<Face>> currentFaces = null;
    private List<Face> lastDrawnFaces = null;
    private NeuralModel model;
    private UserDatabase userDatabase = null;
    private FacePreProcessor facePreProcessor = null;
    private Task<List<Face>> detectedFacesTask;

    /**
     * Method to get and set stuff after view creation.
     *
     * @param savedInstanceState creation bundle - no important
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Initialize activity
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        // Camera setup.
        mOpenCvCameraView = findViewById(R.id.java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(View.VISIBLE);

        // Set camera change button
        Button CameraChange = findViewById(R.id.cameraChange);
        CameraChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });

        takePhotoButton = findViewById(R.id.takePhotoButton);
        boolean addUserMode = getIntent().getBooleanExtra("TakePhotoMode", false);
        if(addUserMode) {
            takePhotoButton.setVisibility(View.VISIBLE);
        }

        Context context = getApplicationContext();
        Resources res = context.getResources();
        SharedPreferences userSettings = GlobalData.getUserSettings(context);
        ModelObject modelObject = GlobalData.getModel(getApplicationContext(),
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]),
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]));

        // Get network model instance
        model = modelObject.neuralModel;

        // Get database instance
        userDatabase = modelObject.userDatabase;

        facePreProcessor = GlobalData.getFacePreProcessor();
    }

    /**
     * If Intent will be paused, we should disable camera.
     */
    @Override
    public void onPause() {
        super.onPause();

        // Disable camera.
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * Disable camera on intent destroy.
     */
    public void onDestroy() {
        super.onDestroy();

        // Disable camera.
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * We won't do anything in this case, just needed implementation
     *
     * @param width  -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    /**
     * We won't do anything in this case, just needed implementation
     */
    @Override
    public void onCameraViewStopped() {
    }

    /**
     * Most important function. It enable us to manipulate frame and show it to user.
     *
     * @param inputFrame frame from camera
     * @return frame, which will be shown on screen
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        // Check if camera should take picture.
        if(saveNextFrame){
            savePhoto(inputFrame);
            return inputFrame;
        }

        // Set and get synchronized data


        detectFaces(inputFrame);

        recogniseFacesTask();


        Task<List<Face>> facesToDraw = currentFaces;

        // Until we will proceed first image, we can't proceed results
        if (facesToDraw != null) {
            // Draw rectangle for each face found in photo.
            List<Face> newFaces = facesToDraw.getResult();
            List<Face> lastFaces = null;
            if (lastDrawnFaces != null){
                lastFaces = lastDrawnFaces;
            }else{
                lastFaces = newFaces;
            }

            for (int i = 0; i < lastFaces.size(); i++) {
                if(lastFaces.size() == newFaces.size()) {
                    lastFaces.get(i).getBoundingBox().left = (lastFaces.get(i).getBoundingBox().left + newFaces.get(i).getBoundingBox().left) / 2;
                    lastFaces.get(i).getBoundingBox().top = (lastFaces.get(i).getBoundingBox().top + newFaces.get(i).getBoundingBox().top) / 2;
                    lastFaces.get(i).getBoundingBox().right = (lastFaces.get(i).getBoundingBox().right + newFaces.get(i).getBoundingBox().right) / 2;
                    lastFaces.get(i).getBoundingBox().bottom = (lastFaces.get(i).getBoundingBox().bottom + newFaces.get(i).getBoundingBox().bottom) / 2;
                }
                Imgproc.rectangle(
                        inputFrame,                                                      // Image
                        new Point(lastFaces.get(i).getBoundingBox().left, lastFaces.get(i).getBoundingBox().top),                                       // p1
                        new Point(lastFaces.get(i).getBoundingBox().right, lastFaces.get(i).getBoundingBox().bottom),            // p2
                        new Scalar(0, 0, 255),                                         // color
                        5                                                             // Thickness
                );
                if(currentNames!=null && currentNames.length > i){
                    Imgproc.putText(inputFrame,currentNames[i], new Point(lastFaces.get(i).getBoundingBox().left, lastFaces.get(i).getBoundingBox().top - 10), FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 1);
                }
            }
            lastDrawnFaces = lastFaces;
        }

        return inputFrame;
    }

    /**
     * Asynchronously detect faces on image. This method put result into currentFaces field.
     * @param inputFrame
     */
    private void detectFaces(Mat inputFrame){
        // Check if results are ready
        if(detectedFacesTask != null &&  detectedFacesTask.isComplete()) {
            // Try to get result
            oldFaces = currentFaces;
            currentFaces = detectedFacesTask;
            if (oldFaces != null)
                lastDrawnFaces = oldFaces.getResult();

        }
        if(detectedFacesTask == null ||  detectedFacesTask.isComplete()) {
            detectedFacesTask = facePreProcessor.detectAllFacesUsingML(inputFrame);
            currentDetectedFrame = inputFrame;
        }
    }

    /**
     * Asynchronously recognise face on images. This method takes last frame and detected faces
     * from class fields.
     */
    private void recogniseFacesTask(){
        // Check if there are faces to be recognised
        if(currentFaces != null){
            // Check if results are ready to get
            if (recognizedFacesCompletableFuture != null && recognizedFacesCompletableFuture.isDone()) {
                // Try to get results
                try {
                    currentNames = recognizedFacesCompletableFuture.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // If task is done, put new task
            if (recognizedFacesCompletableFuture == null ||
                    recognizedFacesCompletableFuture.isDone()) {

                recognizedFacesCompletableFuture = CompletableFuture.supplyAsync(() ->
                        recogniseFaces(), recognizeThreadExecutor);
            }
        }
    }
    public String [] recogniseFaces(){
        // Trim all faces
        ArrayList<Mat> faceImages = facePreProcessor.preProcessAllFaces(currentDetectedFrame, currentFaces);
        String[] newNames = null;

        if (faceImages != null && faceImages.size() > 0) {

            // Calculate vector for each face
            newNames = new String[faceImages.size()];
            for (int i = 0; i < faceImages.size(); i++) {
                // Predict face parameters
                float[] result = null;
                try {
                    result = model.resizeAndProcess(faceImages.get(i))[0];
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } finally {
                    // Find closest user in database.
                    // TODO: Add some threshold to prevent wrong
                    String name = userDatabase.findClosestRecord(result).username;
                    newNames[i] = name;
                }
            }
        }
        return newNames;
    }
    /**
     * In case of resuming up, we have to turn on camera again.
     */
    @Override
    public void onResume() {
        mOpenCvCameraView.enableView();
        super.onResume();
    }

    /**
     * Swap camera mode
     */
    public void swapCamera() {
        // Change camera index
        if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
            CameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
        } else {
            CameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
        }

        // Disable old camera, change camera, enable new camera
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(CameraIndex);
        mOpenCvCameraView.enableView();
    }

    /**
     * Set flag to save next frame to memory.
     *
     * @param view current view
     */
    public void takePhoto(View view) {
        saveNextFrame = true;
    }

    /**
     * Save given frame to file in internal app storage, insert filename in
     * activity result and finish activity.
     *
     * @param frame to be saved
     */
    private void savePhoto(Mat frame){
        if(frame == null){
            Log.e(Tag, "Trying to save null image.");
            throw new NullPointerException();
        }
        Bitmap frameBmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, frameBmp);

        try {
            String filename = "cachedImage";
            FileOutputStream fos = getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);
            frameBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Intent resultData = new Intent();
            resultData.putExtra("UserPhoto", filename);
            setResult(RESULT_OK, resultData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        finish();
    }
}
