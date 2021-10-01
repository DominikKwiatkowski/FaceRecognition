package com.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.libs.imgprocessing.FrameProcessTask;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
    private int CameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    private CameraBridgeViewBase mOpenCvCameraView;
    private FrameProcessTask frameProcessTask;
    private Button takePhotoButton;

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

        // Create thread class
        frameProcessTask = new FrameProcessTask(this);

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
        takePhotoButton.setActivated(addUserMode);
    }

    /**
     * If Intent will be paused, we should disable camera.
     */
    @Override
    public void onPause() {
        super.onPause();

        // Stop processing frames.
        frameProcessTask.setStop(true);

        // Disable camera.
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * Disable camera on intent destroy.
     */
    public void onDestroy() {
        super.onDestroy();

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
        // Set and get synchronized data
        frameProcessTask.setFrame(inputFrame);
        MatOfRect faces = frameProcessTask.getFaces();

        // Until we will proceed first image, we can't proceed results
        if (faces != null) {
            // Draw rectangle for each face found in photo.
            for (Rect face : faces.toArray()) {
                Imgproc.rectangle(
                        inputFrame,                                                      // Image
                        new Point(face.x, face.y),                                       // p1
                        new Point(face.x + face.width, face.y + face.height),            // p2
                        new Scalar(0, 0, 255),                                           // color
                        5                                                                // Thickness
                );
            }
        }

        return inputFrame;
    }

    /**
     * In case of resuming up, we have to turn on camera again.
     */
    @Override
    public void onResume() {
        mOpenCvCameraView.enableView();
        singleThreadExecutor.execute(frameProcessTask);
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
     * Save last frame to file in internal app storage, insert filename in
     * activity result and finish activity.
     *
     * @param view current view
     */
    public void takePhoto(View view) {
        Mat frame = frameProcessTask.getFrame();
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
