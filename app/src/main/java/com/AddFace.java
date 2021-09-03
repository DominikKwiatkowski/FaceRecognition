package com;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import org.opencv.android.Utils;
import org.opencv.imgcodecs.Imgcodecs;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.UserDatabase.UserDatabase;
import com.UserDatabase.UserRecord;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class AddFace extends AppCompatActivity {

    private static final int PICK_PHOTO = 1;
    boolean filePickerMode;
    ImageView currentFaceImage = null;
    Button addButton = null;
    private Imgcodecs imageCodecs = null;
    NeuralModel model = null;
    UserDatabase userDatabase = null;
    float[] currentFaceVector = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);

        currentFaceImage = findViewById(R.id.selectedImage);
        addButton = findViewById(R.id.addUser);
        //addButton.setClickable(false);
        //addButton.setAlpha(0.5f);

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
        // Process picked image
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
        InputStream stream = null;
        try {
            stream = getContentResolver().openInputStream(photo);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
        Mat image = new Mat();
        Utils.bitmapToMat(bmp, image);
        Mat face = model.preProcessOneFace(image);
        bmp = Bitmap.createBitmap(face.cols(), face.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(face, bmp);
        currentFaceImage.setImageBitmap(bmp);
        currentFaceVector = model.resizeAndProcess(face)[0];
        //addButton.setClickable(true);
        //addButton.setAlpha(1f);
    }

}