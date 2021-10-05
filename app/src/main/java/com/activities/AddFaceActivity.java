package com.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.common.FaceProcessingException;
import com.common.ToastWrapper;
import com.libs.globaldata.ModelObject;
import com.libs.facerecognition.NeuralModel;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.userdatabase.UserDatabase;
import com.libs.globaldata.userdatabase.UserRecord;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AddFaceActivity extends AppCompatActivity {

    private Imgcodecs imageCodecs = null;
    private ImageView currentFaceImage = null;
    private Button addButton = null;
    private EditText usernameEditText = null;

    // NeuralModel singleton reference
    private NeuralModel model = null;

    // UserDatabase singleton reference
    private UserDatabase userDatabase = null;

    // Vector representation of face found on selected photo
    private float[] currentFaceVector = null;

    // ToastWrapper Instance
    private ToastWrapper toastWrapper = null;

    // ChoosePhoto Intent launcher
    ActivityResultLauncher<Intent> choosePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Process picked image
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    processPhoto(resolveContentToBitmap(result.getData().getData()));
                }
            });

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Process photo taken from camera
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Get filename from activity result, read photo from internal app storage and process it
                    try {
                        String filename = result.getData().getStringExtra("UserPhoto");
                        FileInputStream fis = getApplicationContext().openFileInput(filename);
                        Bitmap photo = BitmapFactory.decodeStream(fis);
                        processPhoto(photo);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);
        currentFaceImage = findViewById(R.id.selectedImage);
        addButton = findViewById(R.id.addUser);
        usernameEditText = findViewById(R.id.usernameInput);

        usernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
                if (action == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        usernameEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View textView, int key, KeyEvent event) {
                if (key == KeyEvent.KEYCODE_ENTER) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        // Disable add button before photo selected
        addButton.setClickable(false);
        addButton.setAlpha(0.5f);

        // Initialize Imgcodecs class
        imageCodecs = new Imgcodecs();

        ModelObject modelObject = GlobalData.getModel(getApplicationContext(),
                getResources().getString(R.string.model_Facenet));

        // Get network model instance
        model = modelObject.neuralModel;

        // Get database instance
        userDatabase = modelObject.userDatabase;

        // Create ToastWrapper Instance
        toastWrapper = new ToastWrapper(getApplicationContext());
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
        choosePhotoLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    /**
     * Start CameraActivity with in photo taking mode.
     *
     * @param view - current view.
     */
    public void takePhoto(View view) {
        Intent takePhotoIntent = new Intent(this, CameraActivity.class);
        takePhotoIntent.putExtra("TakePhotoMode", true);
        takePhotoLauncher.launch(takePhotoIntent);
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
        Resources res = getResources();

        if (username.isEmpty() || currentFaceVector == null) {
            toastWrapper.showToast(res.getString(R.string.addface_UsernameNotGiven_toast), Toast.LENGTH_SHORT);
            return;
        }

        UserRecord userRecord = new UserRecord(username, currentFaceVector);
        userDatabase.addUserRecord(userRecord);
        toastWrapper.showToast(String.format(res.getString(R.string.addface_UserAdded_toast), username), Toast.LENGTH_SHORT);
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
        return BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
    }


    /**
     * Process chosen photo using NeuralModel, display found face, save face vector.
     *
     * @param photo Bitmap of face which will be preprocessed.
     */
    private void processPhoto(Bitmap photo) {
        Resources res = getResources();
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
            toastWrapper.showToast(res.getString(R.string.addface_NotOneFaceFound_toast), Toast.LENGTH_SHORT);
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