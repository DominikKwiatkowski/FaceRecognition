package com.common;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsWrapper {

    static final public int REQUEST_CODE_PERMISSIONS = 1001;

    /**
     * Validate permissions from targetPermissions list and request non granted ones.
     *
     * @param targetPermissions list of target permissions
     */
    static public void validatePermissions(List<String> targetPermissions, Activity activity) {
        List<String> missingPermissions = new ArrayList<>();

        // Check if target permissions are granted
        for (String permission : targetPermissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        // Request missing permissions
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, missingPermissions.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * Validate permissions from targetPermissions list and request non granted ones.
     *
     * @param grantedResults list of target permissions
     */
    static public boolean ifAllPermissionsGranted(int[] grantedResults) {
        for (int result : grantedResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }
}
