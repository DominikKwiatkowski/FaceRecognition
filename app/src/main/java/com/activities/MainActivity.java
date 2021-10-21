package com.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.common.PermissionsWrapper;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;
import com.libs.globaldata.userdatabase.UserRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // TODO: remove after basic workflow is finished
    ModelObject modelObject = null;

    ActivityResultLauncher<Intent> addFace = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Create all user records
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Put all user records into database.
                    ArrayList<UserRecord> userRecordArray =
                            (ArrayList<UserRecord>) result.getData().getSerializableExtra(
                            getResources().getString(R.string.addFace_resultData_name));
                    for(UserRecord userRecord : userRecordArray){
                        modelObject.userDatabase.addUserRecord(userRecord);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Validate app permissions
        final List<String> targetPermissions = Arrays.asList(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        );

        PermissionsWrapper.validatePermissions(targetPermissions, this);

        SharedPreferences userSettings = GlobalData.getUserSettings(this);
        // TODO: remove after basic workflow is finished
        // Load NeuralModel
        modelObject = GlobalData.getModel(getApplicationContext(),
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]),
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]));
    }

    /**
     * Handle user's response to permissions request.
     *
     * @param requestCode    code of permission request
     * @param permissions    name of permission which was requested
     * @param grantedResults request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantedResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults);

        if (requestCode == PermissionsWrapper.REQUEST_CODE_PERMISSIONS) {
            if (!PermissionsWrapper.ifAllPermissionsGranted(grantedResults)) {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle(getResources().getString(R.string.main_NoPermissions_hint));
                adb.setPositiveButton("Yes",
                        (dialog, which) -> MainActivity.super.finish());
                adb.create().show();
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
            case R.id.cameraPreview:
                Intent i2 = new Intent(this, CameraPreviewActivity.class);
                i2.putExtra(CameraPreviewActivity.CAMERA_MODE_KEY,
                        CameraPreviewActivity.CameraPreviewMode.RECOGNITION);
                startActivity(i2);
                break;
            case R.id.sampleDatabase:
                // TODO: remove after all basic workflow is finished
                modelObject.userDatabase.loadSampleDatabase(getApplicationContext());
                break;
            case R.id.loadDatabase:
                // TODO: remove after all basic workflow is finished
                modelObject.userDatabase.loadDatabase();
                break;
            case R.id.saveDatabase:
                // TODO: remove after all basic workflow is finished
                modelObject.userDatabase.saveDatabase();
                break;
            case R.id.clearModel:
                // TODO: remove after all basic workflow is finished
                modelObject.clear();
                break;
            case R.id.addUser:
                Intent addFaceIntent = new Intent(this, AddFaceActivity.class);
                addFace.launch(addFaceIntent);
                break;
            case R.id.settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;

        }
        return super.onOptionsItemSelected(item);
    }
}
