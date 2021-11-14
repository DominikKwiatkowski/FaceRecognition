package com.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.common.PermissionsWrapper;
import com.libs.facerecognition.NeuralModelProvider;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {

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

        CompletableFuture.runAsync(() -> loadData());
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
        SharedPreferences userSettings = GlobalData.getUserSettings(this);
        ArrayList<String> chosenModels = new ArrayList<>();

        // TODO: remove after basic workflow is finished
        // Load NeuralModel
        ModelObject modelObject = GlobalData.getModel(getApplicationContext(),
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]),
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]));

        chosenModels.add(
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]));
        chosenModels.add(userSettings.getString(
                getString(R.string.settings_userModel_key),
                getResources().getStringArray(R.array.models)[0]));
        
        // Switch between options.
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
                addFaceIntent.putExtra(getResources().getString(R.string.addFace_ChooseModelName_intentValue), chosenModels);
                startActivity(addFaceIntent);
                break;
            case R.id.deleteUser:
                Intent deleteFaceIntent = new Intent(this, DeleteUserActivity.class);
                deleteFaceIntent.putExtra(getResources().getString(R.string.addFace_ChooseModelName_intentValue), chosenModels);
                startActivity(deleteFaceIntent);
                break;
            case R.id.settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;
            case R.id.benchmarkMode:
                Intent benchmark = new Intent(this, BenchmarkModeActivity.class);
                startActivity(benchmark);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Load all requested models asynchronously.
     */
    private void loadData()
    {
        GlobalData.getFacePreProcessor();
        SharedPreferences userSettings = GlobalData.getUserSettings(this);
        NeuralModelProvider.getInstance(getApplicationContext(),
                userSettings.getString(
                        getString(R.string.settings_userModel_key),
                        getResources().getStringArray(R.array.models)[0]));
    }
}
