package com.libs.customcamera;

import android.graphics.Bitmap;

public interface CameraPreviewListener {
    void onCameraFrame(Bitmap newFrame, int rotationDegrees);
}
