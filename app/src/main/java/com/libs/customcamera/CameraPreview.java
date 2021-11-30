package com.libs.customcamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.camera2.CaptureRequest;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraPreview {
    private static final String Tag = "CameraWrapper";

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Activity targetActivity;
    private final PreviewView previewView;
    private final List<CameraPreviewListener> cameraPreviewListeners = new ArrayList<>();

    private ProcessCameraProvider cameraProvider;
    private int targetCamera = CameraSelector.LENS_FACING_BACK;

    /**
     * Wrapper class to androidx.camera. Manages camera preview and frame capturing.
     *
     * @param targetActivity activity in which the camera will be displayed
     * @param previewView    View on which the camera stream will be displayed
     */
    public CameraPreview(Activity targetActivity, PreviewView previewView) {
        this.targetActivity = targetActivity;
        this.previewView = previewView;
    }

    /**
     * Get current lens of the camera.
     *
     * @return id of the camera as
     * CameraSelector.LENS_FACING_FRONT or CameraSelector.LENS_FACING_FRONT.
     */
    public int getLensFacing() {
        return targetCamera;
    }

    /**
     * Switch camera lens.
     */
    public void switchCamera() {
        if (targetCamera == CameraSelector.LENS_FACING_BACK) {
            targetCamera = CameraSelector.LENS_FACING_FRONT;
        } else {
            targetCamera = CameraSelector.LENS_FACING_BACK;
        }

        closeCamera();
        openCamera();
    }

    /**
     * Close camera connection.
     */
    public void closeCamera() {
        if(cameraProvider != null){
            cameraProvider.unbindAll();
        }
    }

    /**
     * Open camera stream on given preview.
     */
    public void openCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(targetActivity);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // This should never be reached.
                    throw new AssertionError("Unable to initialize camera");
                }
            }
        }, ContextCompat.getMainExecutor(targetActivity));
    }

    /**
     * Initialize stream configuration and bind it to preview.
     */
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        this.cameraProvider = cameraProvider;

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(Surface.ROTATION_0)
                .build();

        // // Preview image's resolution (larger image takes longer to process)
        // int imageAnalysisWidth = 640;
        // int imageAnalysisHeight = imageAnalysisWidth *
        //         previewResolution.getHeight() / previewResolution.getWidth();

        // Most optimal solution
        ImageAnalysis.Builder imageAnalysisBuilder = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888);

        Camera2Interop.Extender<ImageAnalysis> ext = new Camera2Interop.Extender<>(imageAnalysisBuilder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(30, 30));

        ImageAnalysis imageAnalysis = imageAnalysisBuilder.build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(targetCamera)
                .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                // The image rotation and RGB image buffer are initialized only once
                // the analyzer has started running
                int imageRotationDegrees = image.getImageInfo().getRotationDegrees();
                Bitmap bitmapBuffer = Bitmap.createBitmap(
                        image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

                // Copy out RGB bits to our shared buffer
                bitmapBuffer.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
                image.close();

                // Notify all listeners about new frame
                notifyCameraFrame(bitmapBuffer, imageRotationDegrees);
            }
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                (LifecycleOwner) targetActivity,
                cameraSelector,
                preview,
                imageAnalysis
        );

        preview.setSurfaceProvider(this.previewView.getSurfaceProvider());
    }

    /**
     * Notify listener about onCameraFrame event.
     *
     * @param bitmap          new camera frame
     * @param rotationDegrees rotation angle of the image
     */
    private void notifyCameraFrame(Bitmap bitmap, int rotationDegrees) {
        synchronized (cameraPreviewListeners) {
            for (CameraPreviewListener listener : cameraPreviewListeners) {
                if (listener != null) {
                    listener.onCameraFrame(bitmap, rotationDegrees);
                }
            }
        }
    }

    /**
     * Register new listener.
     *
     * @param listener listener to register
     */
    public void setListener(CameraPreviewListener listener) {
        synchronized (cameraPreviewListeners) {
            cameraPreviewListeners.add(listener);
        }
    }

    /**
     * Remove listener.
     *
     * @param listener listener to remove
     */
    public void removeListener(CameraPreviewListener listener) {
        synchronized (cameraPreviewListeners) {
            cameraPreviewListeners.remove(listener);
        }
    }
}
