package com.activities;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.R;
import com.common.PermissionsWrapper;
import com.common.TransitionsLibrary;
import com.google.android.material.navigation.NavigationView;
import com.libs.facerecognition.NeuralModelProvider;
import com.libs.globaldata.GlobalData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set set drawer to enable
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navDrawer_open_state, R.string.navDrawer_close_state);
        drawer.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();

        // Setup navigation click operations
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(navigationViewListener);

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

    private final NavigationView.OnNavigationItemSelectedListener navigationViewListener = navItem -> {
        switch (navItem.getItemId()) {
            case R.id.navSettings:
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            break;
        }

        // //This is for maintaining the behavior of the Navigation view
        // NavigationUI.onNavDestinationSelected(navItem, navController);
        //This is for closing the drawer after acting on it
        drawer.closeDrawer(GravityCompat.START);
        return false;
    };

    /**
     * Return list of strings representing chosen models and databases.
     */
    private ArrayList<String> getChosenModels() {
        SharedPreferences userSettings = GlobalData.getUserSettings(this);
        ArrayList<String> chosenModels = new ArrayList<>();

        chosenModels.add(userSettings.getString(
                getString(R.string.settings_userModel_key),
                getResources().getStringArray(R.array.models)[0]));
        chosenModels.add(userSettings.getString(
                getString(R.string.settings_userModel_key),
                getResources().getStringArray(R.array.models)[0]));

        return chosenModels;
    }

    /**
     * Methods executed on buttons OnClick events.
     */
    public void onAddUserClick(View view) {
        Intent addFaceIntent = new Intent(this, AddFaceActivity.class);
        addFaceIntent.putExtra(getResources().getString(R.string.addFace_ChooseModelName_intentValue), getChosenModels());
        startActivity(addFaceIntent);
        TransitionsLibrary.executeToLeftTransition(this);
    }

    public void onDeleteUserClick(View view) {
        Intent deleteFaceIntent = new Intent(this, DeleteUserActivity.class);
        deleteFaceIntent.putExtra(getResources().getString(R.string.addFace_ChooseModelName_intentValue), getChosenModels());
        startActivity(deleteFaceIntent);
        TransitionsLibrary.executeToLeftTransition(this);
    }

    public void onOpenPreviewClick(View view) {
        Intent cameraPreviewIntent = new Intent(this, CameraPreviewActivity.class);
        cameraPreviewIntent.putExtra(CameraPreviewActivity.CAMERA_MODE_KEY,
                CameraPreviewActivity.CameraPreviewMode.RECOGNITION);
        startActivity(cameraPreviewIntent);
        TransitionsLibrary.executeToLeftTransition(this);
    }

    public void onOpenBenchmarkClick(View view) {
        Intent benchmarkIntent = new Intent(this, BenchmarkModeActivity.class);
        startActivity(benchmarkIntent);
        TransitionsLibrary.executeToLeftTransition(this);
    }
}
