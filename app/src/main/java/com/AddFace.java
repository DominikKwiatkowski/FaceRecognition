package com;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.UserDatabase.UserDatabase;
import com.UserDatabase.UserRecord;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import common.FaceProcessingException;
import common.ToastWrapper;

public class AddFace extends AppCompatActivity {

    private static final int PICK_PHOTO = 1;
    private static final int TAKE_PHOTO = 2;
    ImageView currentFaceImage = null;
    Button addButton = null;
    // NeuralModel singleton reference
    NeuralModel model = null;
    // UserDatabase singleton reference
    UserDatabase userDatabase = null;
    // Vector representation of face found on selected photo
    float[] currentFaceVector = null;
    // ToastWrapper Instance
    ToastWrapper toastWrapper = null;
    private Imgcodecs imageCodecs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);
        currentFaceImage = findViewById(R.id.selectedImage);
        addButton = findViewById(R.id.addUser);
        // Disable add button before photo selected
        addButton.setClickable(false);
        addButton.setAlpha(0.5f);
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
        );

        //  Create ToastWrapper Instance
        toastWrapper = new ToastWrapper(getApplicationContext());

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Process picked image
        if (requestCode == PICK_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
            processPhoto(resolveContentToBitmap(data.getData()));
        }
        // Process photo taken from camera
        else if (requestCode == TAKE_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
            // Get filename from activity result, read photo from internal app storage and process it
            try {
                String filename = data.getStringExtra("UserPhoto");
                FileInputStream fis = getApplicationContext().openFileInput(filename);
                Bitmap photo = BitmapFactory.decodeStream(fis);
                processPhoto(photo);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Start file chooser activity with image constraint.
     *
     * @param view - current view.
     */
    public void choosePhoto(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_PHOTO);
    }

    /**
     * Start CameraActivity with in photo taking mode.
     *
     * @param view - current view.
     */
    public void takePhoto(View view) {
        Intent takePhotoIntent = new Intent(this, CameraActivity.class);
        takePhotoIntent.putExtra("TakePhotoMode", true);
        startActivityForResult(takePhotoIntent, TAKE_PHOTO);
    }

    /**
     * Exit activity without adding user.
     *
     * @param view - current view.
     */
    public void cancel(View view) {
        finish();
    }

    /**
     * Create UserRecord with data from last processed image and user input.
     *
     * @param view - current view.
     */
    public void addUser(View view) {
        EditText usernameInput = findViewById(R.id.usernameInput);
        String username = usernameInput.getText().toString();
        if (username.isEmpty() || currentFaceVector == null) {
            toastWrapper.showToast("Nie wprowadzono nazwy!", Toast.LENGTH_SHORT);
            return;
        }

        UserRecord userRecord = new UserRecord(username, currentFaceVector);
        userDatabase.addUserRecord(userRecord);
        toastWrapper.showToast(String.format("Dodano użytkownika %s.", username), Toast.LENGTH_SHORT);
        finish();
    }

    /**
     * Resolve photo uri to bitmap
     *
     * @param photo - uri to image.
     * @return Bitmap of resolved image.
     */
    private Bitmap resolveContentToBitmap(Uri photo) {
        InputStream stream = null;
        try {
            // Open file in stream
            stream = getContentResolver().openInputStream(photo);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Decode photo to Bitmap
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
        return bmp;
    }


    /**
     * Process chosen photo using NeuralModel, display found face, save face vector.
     *
     * @param photo Bitmap of face which will be preprocessed.
     */
    private void processPhoto(Bitmap photo) {
        Bitmap bmp = photo;
        // Convert Bitmap to Mat
        Mat image = new Mat();
        Utils.bitmapToMat(bmp, image);
        // Find face in photo
        Mat face;
        try {
            face = model.preProcessOneFace(image);
        } catch (FaceProcessingException fpe) {
            fpe.printStackTrace();
            toastWrapper.showToast("Brak lub więcej niż jedna twarz na zdjęciu.", Toast.LENGTH_SHORT);
            return;
        }
        // Convert face with Math to bitmap for ImageView
        bmp = Bitmap.createBitmap(face.cols(), face.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(face, bmp);
        // Display found face on screen in ImageView
        currentFaceImage.setImageBitmap(bmp);
        currentFaceVector = model.resizeAndProcess(face)[0];
        // Unlock "add" button
        addButton.setClickable(true);
        addButton.setAlpha(1f);
    }

}