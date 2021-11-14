package com.libs.benchmark;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Pair;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.activities.AddFaceActivity;
import com.activities.DeleteUserActivity;
import com.common.ToastWrapper;
import com.libs.facerecognition.NeuralModelProvider;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BenchmarkLayout implements LayoutClassInterface {
    private final List<Pair<String, String>> supportedModels = new ArrayList<>();
    private final AppCompatActivity caller;

    private final AddPhotoLayout addPhotoLayout;
    private final LayoutClassInterface displayResultLayout;
    private final ToastWrapper toastWrapper;
    private final ActivityResultLauncher<Intent> faceOperationLauncher;
    // This field is only to get data about state of database
    private ModelObject sampleModelObject;
    private TextView numberOfPhotos;
    private TextView numberOfFaces;

    public BenchmarkLayout(AppCompatActivity caller) {
        this.caller = caller;
        toastWrapper = new ToastWrapper(caller);
        SharedPreferences userSettings = GlobalData.getUserSettings(caller);
        // TODO Clear models before every start!!!
        // Read supported models.
        for (String model : caller.getResources().getStringArray(R.array.models)) {
            if (userSettings.getBoolean(model + caller.getString(R.string.settings_benchModel_suffix), true)) {
                supportedModels.add(new Pair<>(model, model + caller.getString(R.string.BenchmarkMode_ModelDatabaseName_Suffix)));
            }
        }
        // Check if there is a supported model
        if (supportedModels.size() == 0) {
            toastWrapper.showToast(caller.getString(R.string.BenchmarkMode_NoPhoto_Toast), Toast.LENGTH_LONG);
            caller.finish();
        }

        addPhotoLayout = new AddPhotoLayout(caller, this);
        displayResultLayout = new DisplayResultsLayout(
                caller, addPhotoLayout, supportedModels);

        sampleModelObject = GlobalData.getModel(
                caller, supportedModels.get(0).first, supportedModels.get(0).second);

        faceOperationLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    updateUI();
                }
        );

        CompletableFuture.runAsync(() -> loadData());
    }

    @Override
    public void makeActive() {
        caller.setContentView(R.layout.activity_benchmark_mode);

        Button addUserButton = caller.findViewById(R.id.benchAddFace);
        addUserButton.setOnClickListener(v -> addUser());

        Button addPhotoButton = caller.findViewById(R.id.benchMakePhoto);
        addPhotoButton.setOnClickListener(v -> addPhoto());

        Button testButton = caller.findViewById(R.id.benchTest);
        testButton.setOnClickListener(v -> test());
        testButton.setEnabled(addPhotoLayout.testPhotos.size() != 0 &&
                sampleModelObject.userDatabase.getNumberOfUsers() != 0);

        Button deleteUserButton = caller.findViewById(R.id.benchDeleteFace);
        deleteUserButton.setOnClickListener(v -> deleteUser());

        numberOfFaces = caller.findViewById(R.id.numberOfFacesView);
        numberOfFaces.setText(String.format(
                caller.getString(R.string.BenchmarkMode_NumberOfUsers_Format),
                sampleModelObject.userDatabase.getNumberOfUsers()));

        numberOfPhotos = caller.findViewById(R.id.numberOfPhotosView);
        numberOfPhotos.setText(String.format(
                caller.getString(R.string.BenchmarkMode_NumberOfPhotos_Format),
                addPhotoLayout.testPhotos.size()));
    }

    /**
     * Update UI after user database activities are finished.
     */
    private void updateUI() {
        //TODO check why this get is necessary
        sampleModelObject = GlobalData.getModel(
                caller, supportedModels.get(0).first, supportedModels.get(0).second);

        numberOfFaces.setText(String.format(
                caller.getString(R.string.BenchmarkMode_NumberOfUsers_Format),
                sampleModelObject.userDatabase.getNumberOfUsers()));
        numberOfPhotos.setText(String.format(
                caller.getString(R.string.BenchmarkMode_NumberOfPhotos_Format),
                addPhotoLayout.testPhotos.size()));
    }

    /**
     * Start add user activity.
     */
    private void addUser() {
        Intent addFaceIntent = new Intent(caller, AddFaceActivity.class);
        ArrayList<String> chosenModels = new ArrayList<>();
        for (Pair<String, String> model : supportedModels) {
            chosenModels.add(model.first);
            chosenModels.add(model.second);
        }
        addFaceIntent.putExtra(caller.getString(R.string.addFace_ChooseModelName_intentValue), chosenModels);
        faceOperationLauncher.launch(addFaceIntent);
    }

    /**
     * Start delete user activity.
     */
    private void deleteUser() {
        Intent deleteFaceIntent = new Intent(caller, DeleteUserActivity.class);
        ArrayList<String> chosenModels = new ArrayList<>();
        for (Pair<String, String> model : supportedModels) {
            chosenModels.add(model.first);
            chosenModels.add(model.second);
        }
        deleteFaceIntent.putExtra(caller.getString(R.string.addFace_ChooseModelName_intentValue), chosenModels);
        faceOperationLauncher.launch(deleteFaceIntent);
    }

    /**
     * Activate add photo layout.
     */
    private void addPhoto() {
        addPhotoLayout.makeActive();
    }

    /**
     * Activate test layout.
     */
    private void test() {
        displayResultLayout.makeActive();
    }

    /**
     * Load all requested models asynchronously.
     */
    private void loadData()
    {
        for (Pair<String,String> supportedModel:supportedModels) {
            NeuralModelProvider.getInstance(caller,supportedModel.first);
        }
    }
}
