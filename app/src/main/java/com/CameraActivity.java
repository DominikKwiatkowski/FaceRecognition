package com;

import androidx.appcompat.app.AppCompatActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.support.image.TensorImage;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private NeuralModel model;
    private int CameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    static {
        System.loadLibrary("opencv_java3");
    }

    Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
    MatOfRect faceMat = null;
    String [] names = null;
    Mat lastFrame = null;

    /**
     * Method to get and set stuff after view creation.
     * @param savedInstanceState creation bundle - no important
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Activity creation.
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        // Load OpenCv
        if(OpenCVLoader.initDebug())
        {
            Log.d("OPENCV", "OpenCv loaded succesfully");
        }

        // Camera setup.
        mOpenCvCameraView = findViewById(R.id.java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(View.VISIBLE);

        // Neural model load.
        model = new NeuralModel(this, "Facenet-optimized.tflite");

        //Set camera change button
        Button CameraChange = findViewById(R.id.cameraChange);
        CameraChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });

        singleThreadExecutor.execute(runnable);
    }

    /**
     * If Intent will be paused, we should disable camera.
     */
    @Override
    public void onPause() {
        super.onPause();
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
     *
     * @param width -  the width of the frames that will be delivered.
     * @param height - the height of the frames that will be delivered.
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    /**
     *
     */
    @Override
    public void onCameraViewStopped() {
    }

    /**
     * Most important function. It enable us to manipulate frame and show it to user.
     * @param inputFrame frame from camera.
     * @return frame, which will be shown on screen.
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        // Set and get synchronized data.
        setFrame(inputFrame);
        MatOfRect faces = getFaces();

        // Until we will proceed first image, we can't proceed results.
        if(faces != null) {
            // Draw rectangle for each face found in photo.
            for (Rect face : faces.toArray()) {
                Imgproc.rectangle(
                        inputFrame,                                                 // Image
                        new Point(face.x, face.y),                                  //p1
                        new Point(face.x + face.width, face.y + face.height),//p2
                        new Scalar(0, 0, 255),                                     //color
                        5                                                //Thickness
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
        super.onResume();
    }

    /**
     * change camera, front->back, back->front.
     */
    public void swapCamera() {
        // Change camera index
        if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
            CameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
        }
        else {
            CameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
        }

        // disable old camera, change camera, enable new camera
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(CameraIndex);
        mOpenCvCameraView.enableView();
    }

    private Runnable runnable = new Runnable() {
        /**
         * This function will constantly get newest frame and proceed it with face detection
         * and recognition.
         */
        @Override
        public void run() {
            while(true)
            {
                // Get frame. Set frame to null to avoid doing same operation twice.
                Mat inputFrame = getFrame();
                setFrame(null);

                if(inputFrame != null) {
                    MatOfRect faces = model.detectAllFaces(inputFrame);
                    // TODO: Some way to track face, make some noise detection,
                    // TODO: maybe some number of frames with similar object?

                    // TODO: we have to proceed this input and put some number to face, and apply
                    // TODO: face detection for frame operation.

                    //Pre process all images
                    ArrayList<Mat> faceImages = model.preProcessAllFaces(inputFrame, faces);

                    // Calculate vector for each face
                    for (Mat face : faceImages) {
                        TensorImage image = model.changeImageRes(face);
                        model.processImage(image);
                    }

                    // Set result synchronized.
                    setFaces(faces);
                }
                else
                {
                    // In case of no ready frame, wait some time
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    /**
     * Synchronized function to set last frame.
     * @param lastFrame last frame recieved from video
     */
    public synchronized void setFrame (Mat lastFrame)
    {
        this.lastFrame = lastFrame;
    }

    /**
     * Synchronized function to get last frame.
     * @return last frame of video
     */
    public synchronized Mat getFrame ()
    {
        return lastFrame;
    }

    /**
     * Synchronized function to set all detected faces.
     * @param newFaces Array of Rect, which determines face position
     */
    public synchronized void setFaces (MatOfRect newFaces)
    {
        faceMat = newFaces;
    }

    /**
     * Synchronized function to get detected faces.
     * @return Array of Rect, which determines face position
     */
    public synchronized MatOfRect getFaces ()
    {
        return faceMat;
    }

    /**
     * Synchronized function to set all names of detected faces.
     * @param names names of people on image
     */
    public synchronized void setNames (String[] names)
    {
        this.names = names;
    }

    /**
     * Synchronized function to get all names of detected faces.
     * @return names of people on image
     */
    public synchronized String[] getnames ()
    {
        return names;
    }
}