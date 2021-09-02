package com;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.opencv.imgcodecs.Imgcodecs;
import android.view.View;
import android.widget.EditText;

import com.UserDatabase.UserDatabase;
import com.UserDatabase.UserRecord;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class AddFace extends AppCompatActivity {

    private static final int PICK_PHOTO = 1;
    boolean filePickerMode;
    private Imgcodecs imageCodecs = null;
    NeuralModel model = null;
    UserDatabase userDatabase = null;
    float[] currentFaceVector = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);

        // Initialize Imgcodecs class
        imageCodecs = new Imgcodecs();
        // Get network model instance
        model = NeuralModel.getInstance(getApplicationContext(),
                "Facenet-optimized.tflite");
        // Get database instance
        userDatabase = UserDatabase.getInstance(
                getApplicationContext(),        // App specific internal storage location
                "Facenet",        // Model name TODO: temporary
                128                // Vector size TODO: temporary
        );             // Vector size TODO: temporary);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
            processImage(data.getData());
        }
    }

    public void choosePhoto(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_PHOTO);
    }

    public void cancel(View view) {
        finish();
    }

    public void add(View view) {
        EditText usernameInput = findViewById(R.id.usernameInput);
        String username = usernameInput.getText().toString();
        if(username.isEmpty() || currentFaceVector == null)
            return;
        UserRecord userRecord = new UserRecord(username, currentFaceVector);
        userDatabase.addUserRecord(userRecord);
        finish();
    }

    /**
     * TODO
     * ...
     */
    private void processImage(Uri photo) {
        Mat image = imageCodecs.imread(photo.toString());
        Mat face = model.preProcessOneFace(image);
        currentFaceVector = model.resizeAndProcess(face)[0];
    }

}

