package com.libs.benchmark;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Pair;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.activities.AddFaceActivity;
import com.libs.globaldata.GlobalData;

import java.util.ArrayList;

public class BenchmarkLayout implements LayoutClassInterface {
    private final ArrayList<Pair<String, String>> supportedModels = new ArrayList<>();
    private final AppCompatActivity caller;

    private final LayoutClassInterface addPhotoLayout;
    private final LayoutClassInterface displayResultLayout;

    public BenchmarkLayout(AppCompatActivity caller) {
        this.caller = caller;

        SharedPreferences userSettings = GlobalData.getUserSettings(caller);

        // Read supported models.
        for (String model : caller.getResources().getStringArray(R.array.models)) {
            if (userSettings.getBoolean(model + caller.getString(R.string.settings_benchModel_suffix), true)) {
                supportedModels.add(new Pair<>(model, model + caller.getString(R.string.BenchmarkMode_ModelDatabaseName_Suffix)));
            }
        }

        addPhotoLayout = new AddPhotoLayout(caller, this);
        displayResultLayout = new DisplayResultsLayout(caller, (AddPhotoLayout) addPhotoLayout, supportedModels);
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
