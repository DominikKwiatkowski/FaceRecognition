package com.activities;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.common.ToastWrapper;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;
import com.libs.globaldata.userdatabase.UserDatabase;

import java.util.ArrayList;
import java.util.List;

public class DeleteUserActivity extends AppCompatActivity {
    private final int textSize = 18;
    private List<ModelObject> models = new ArrayList<>();
    private ToastWrapper toastWrapper;
    private TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_user);
        toastWrapper = new ToastWrapper(this);
        tableLayout = this.findViewById(R.id.deleteUserTableLayout);

        // Get all models.
        ArrayList<String> requestedModels = (ArrayList<String>) getIntent().
                getSerializableExtra(getString(R.string.addFace_ChooseModelName_intentValue));
        for (int i = 0; i < requestedModels.size() / 2; i++) {
            ModelObject modelObject = GlobalData.getModel(
                    getApplicationContext(),
                    requestedModels.get(2 * i),
                    requestedModels.get(2 * i + 1));
            if (!models.contains(modelObject)) {
                models.add(modelObject);
            }
        }

        // Check if any model was given.
        if (models.size() == 0) {
            toastWrapper.showToast(
                    this.getString(R.string.BenchmarkMode_NoPhoto_Toast), Toast.LENGTH_LONG);
            this.finish();
        }

        // Even with many models, they database users have to be same.
        UserDatabase database = models.get(0).userDatabase;
        // Count padding size( in dp).
        int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());

        for (String user : database.getUsersArray()) {
            // Create views for user.
            TableRow row = new TableRow(this);
            TextView userTextView = new TextView(this);
            TextView numberOfPhotosView = new TextView(this);
            Button deleteButton = new Button(this);

            // Set up user name view.
            userTextView.setText(user);
            userTextView.setTextSize(textSize);
            userTextView.setPadding(pixels, pixels, pixels, pixels);

            // Set up number of photos view.
            numberOfPhotosView.setText(String.valueOf(database.getUserRecord(user).getWeight()));
            numberOfPhotosView.setTextSize(textSize);
            numberOfPhotosView.setPadding(pixels, pixels, pixels, pixels);

            // Set up delete button.
            deleteButton.setOnClickListener(v -> removeUser(user, v));
            deleteButton.setText(R.string.DeleteUser_DeleteUserButton_Text);
            deleteButton.setPadding(pixels, pixels, pixels, pixels);

            // Add all views to table.
            row.addView(userTextView);
            row.addView(numberOfPhotosView);
            row.addView(deleteButton);
            tableLayout.addView(row);
        }
    }

    /**
     * Remove user from all given databases
     *
     * @param user name of user
     * @param v    Button, which trigger on click event
     */
    private void removeUser(String user, View v) {
        for (ModelObject model : models) {
            model.userDatabase.removeUserRecord(user);
        }
        tableLayout.removeView((View) v.getParent());
    }
}