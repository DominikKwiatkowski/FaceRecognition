package com.libs.benchmark;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Pair;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.activities.AddFaceActivity;
import com.common.ToastWrapper;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;

import java.util.ArrayList;

public class BenchmarkLayout implements LayoutClassInterface {
    private final ArrayList<Pair<String, String>> supportedModels = new ArrayList<>();
    private final AppCompatActivity caller;

    private final AddPhotoLayout addPhotoLayout;
    private final LayoutClassInterface displayResultLayout;
    // This field is only to get data about state of database
    private final ModelObject sampleModelObject;
    ToastWrapper toastWrapper;

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
        if(supportedModels.size()==0){
            toastWrapper.showToast("Lacking supported model!!!", Toast.LENGTH_LONG);
            caller.finish();

        }

        addPhotoLayout = new AddPhotoLayout(caller, this);
        displayResultLayout = new DisplayResultsLayout(
                caller, (AddPhotoLayout) addPhotoLayout, supportedModels);

        sampleModelObject = GlobalData.getModel(
                caller,supportedModels.get(0).first,supportedModels.get(0).second);
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

        TextView numberOfFaces = caller.findViewById(R.id.numberOfFacesView);
        numberOfFaces.setText(String.format(
                caller.getString(R.string.BenchmarkMode_NumberOfUsers_Format),
                sampleModelObject.userDatabase.getNumberOfUsers()));

        TextView numberOfPhotos = caller.findViewById(R.id.numberOfPhotosView);
        numberOfPhotos.setText(String.format(
                caller.getString(R.string.BenchmarkMode_NumberOfPhotos_Format),
                addPhotoLayout.testPhotos.size()));

        testButton.setEnabled(addPhotoLayout.testPhotos.size() != 0 &&
                sampleModelObject.userDatabase.getNumberOfUsers() != 0);
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
        caller.startActivity(addFaceIntent);
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
}
