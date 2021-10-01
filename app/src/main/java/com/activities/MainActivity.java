package com.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.R;
import com.libs.facerecognition.NeuralModel;
import com.libs.userdatabase.UserDatabase;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private NeuralModel model = null;
    private UserDatabase userDatabase = null;
    private Uri fileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Validate app permissions
        final List<String> targetPermissions = Arrays.asList(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        );
        validatePermissions(targetPermissions);

        // TODO: All of this needs to be moved along with Model Selection functionality

        // Load OpenCv
        if (OpenCVLoader.initDebug()) {
            Log.d("OPENCV", "OpenCv loaded succesfully");
        }

        // Load NeuralModel
        model = NeuralModel.getInstance(getApplicationContext(), "Facenet-optimized.tflite");

        // Initialize database
        // TODO: Temporary. Later on it should be moved to the model selection menu
        //  (database will be defined per model).
        userDatabase = UserDatabase.getInstance(
                getApplicationContext(),        // App specific internal storage location
                "Facenet",        // Model name TODO: temporary
                128                // Vector size TODO: temporary
        );
    }

    /**
     * Validate permissions from targetPermissions list and request non granted ones.
     *
     * @param targetPermissions list of target permissions
     */
    private void validatePermissions(List<String> targetPermissions) {
        List<String> missingPermissions = new ArrayList<>();

        // Check if target permissions are granted
        for (String permission : targetPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        // Request missing permissions
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), 0);
        }
    }

    /**
     * Handle user's response to permissions request.
     *
     * @param requestCode  code of permission request
     * @param permissions  name of permission which was requested
     * @param grantedResults request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantedResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults);
        Resources res = getResources();

        if (requestCode == 0) {
            for (int result : grantedResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    adb.setTitle(res.getString(R.string.main_NoPermissions_hint));
                    adb.setPositiveButton("Yes",
                            (dialog, which) -> MainActivity.super.finish());
                    adb.create().show();
                }
            }
        }
    }

    /**
     * @param menu class of menu to be used. It should be some resource within menu directory.
     * @return true if successful.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Options menu callback.
     *
     * @param item - item chosen by user
     * @return true if successful.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cameraScreen:
                Intent i = new Intent(this, CameraActivity.class);
                startActivity(i);
                break;
            case R.id.sampleDatabase:
                userDatabase.loadSampleDatabase();
                break;
            case R.id.loadDatabase:
                userDatabase.loadDatabase();
                break;
            case R.id.saveDatabase:
                userDatabase.saveDatabase();
                break;
            case R.id.addUser:
                Intent addFaceIntent = new Intent(this, AddFaceActivity.class);
                startActivity(addFaceIntent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }
}
