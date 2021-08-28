package inz;

import androidx.appcompat.app.AppCompatActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    static {
        System.loadLibrary("opencv_java3");
    }

    /**
     * Method to get and set stuff after view creation.
     * @param savedInstanceState creation bundle - no important
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        OpenCVLoader.initDebug();
        setContentView(R.layout.activity_camera);
        mOpenCvCameraView = findViewById(R.id.java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(View.VISIBLE);

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
        // frame operation.
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
}