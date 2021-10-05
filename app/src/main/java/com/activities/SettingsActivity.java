package com.activities;

import com.R;
import com.libs.globaldata.GlobalData;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout layout;
    private Spinner modelChoiceSpinner;
    private SharedPreferences userSettings;
    private SharedPreferences.Editor editor;
    private String[] models;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        layout = findViewById(R.id.settingsLayout);
        modelChoiceSpinner = findViewById(R.id.chooseModelSpinner);

        // Get all models
        models = getResources().getStringArray(R.array.models);

        // Load user settings preferences
        userSettings = GlobalData.getUserSettings(this);
        editor = userSettings.edit();

        // Check if user already have some preferences, if not create default one
        if (!userSettings.contains(getString(R.string.user_Settings_user_model_key))) {
            editor.putString(getString(R.string.user_Settings_user_model_key), models[0]);
            for (String model : models) {
                editor.putBoolean(model + getString(R.string.user_settings_model_suffix), true);
            }
            // We want to use commit here, because data must be stored before next loop will work
            editor.commit();
        }

        // Setup switches per every model
        for (String model : models) {
            // Create switch
            Switch modelSwitch = new Switch(this);
            modelSwitch.setText(model);
            modelSwitch.setChecked(userSettings.getBoolean(model + getString(R.string.user_settings_model_suffix), true));
            // If value changed, update preferences.
            modelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    //commit prefs on change
                    editor.putBoolean(model + getString(R.string.user_settings_model_suffix), isChecked);
                    editor.apply();
                }
            });
            layout.addView(modelSwitch);
        }

        // Create options for spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.models, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        modelChoiceSpinner.setAdapter(adapter);
        // Find chosen model position - maybe better to store int??? Share your opinion on review
        int position = 0;
        for (int i = 0; i < models.length; i++) {
            if (userSettings.getString(getString(R.string.user_Settings_user_model_key), models[0]).equals(models[i])) {
                position = i;
                break;
            }
        }

        // Setup spinner
        modelChoiceSpinner.setSelection(position);
        modelChoiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putString(getString(R.string.user_Settings_user_model_key), models[position]);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
