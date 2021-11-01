package com.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.R;
import com.common.FaceProcessingException;
import com.common.ToastWrapper;
import com.libs.facerecognition.FacePreprocessor;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;
import com.libs.globaldata.userdatabase.UserRecord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.common.BitmapOperations.resolveContentToBitmap;

public class AddFaceActivity extends AppCompatActivity {

    // Contains all models given by user.
    private final List<Pair<ModelObject, float[]>> models = new ArrayList<>();
    private ImageView currentFaceImage = null;
    private Button addButton = null;
    private Button addFromPhotoButton = null;
    private Button addFromCameraButton = null;
    private EditText usernameEditText = null;
    private ProgressBar progressBar = null;
    // ToastWrapper Instance
    private ToastWrapper toastWrapper = null;

    private FacePreprocessor facePreProcessor = null;

    // ChoosePhoto Intent launcher
    ActivityResultLauncher<Intent> choosePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Process picked image
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    processPhoto(resolveContentToBitmap(result.getData().getData(), this));
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
                        String filename = result.getData().getStringExtra(CameraPreviewActivity.CAPTURED_FRAME_KEY);
                        FileInputStream fis = getApplicationContext().openFileInput(filename);
                        Bitmap photo = BitmapFactory.decodeStream(fis);
                        fis.close();
                        processPhoto(photo);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    ActivityResultLauncher<Intent> chooseCatalogLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Load data from picked catalog
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    loadTestDirectory(result.getData().getData());
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);
        currentFaceImage = findViewById(R.id.selectedImage);
        addButton = findViewById(R.id.addUser);
        addFromPhotoButton = findViewById(R.id.addFromPhoto);
        addFromCameraButton = findViewById(R.id.addFromCamera);
        usernameEditText = findViewById(R.id.usernameInput);

        usernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
                if (action == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        progressBar = findViewById(R.id.progressBar);

        // Hide progress bar
        progressBar.setVisibility(View.INVISIBLE);

        // Disable add button before photo selected
        setAddButtonState(false);

        ArrayList<String> requestedModels = (ArrayList<String>) getIntent().
                getSerializableExtra(getString(R.string.addFace_ChooseModelName_intentValue));

        for (int i = 0; i < requestedModels.size() / 2; i++) {
            ModelObject modelObject = GlobalData.getModel(
                    getApplicationContext(),
                    requestedModels.get(2 * i),
                    requestedModels.get(2 * i + 1));
            if (!models.contains(modelObject)) {
                models.add(new Pair<>(modelObject, null));
            }

        }

        // Create ToastWrapper Instance
        toastWrapper = new ToastWrapper(getApplicationContext());

        facePreProcessor = GlobalData.getFacePreProcessor();
    }

    /**
     * Unlock button for adding user if true passed, lock otherwise.
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
     * Exit activity without adding user.
     *
     * @param view - current view.
     */
    public void cancel(View view) {
        finish();
    }

    /**
     * Toggle state of buttons for adding users
     *
     * @param state desired state of button
     */
    void setAddButtonState(boolean state) {
        if (state) {
            addButton.setClickable(true);
            addButton.setAlpha(1f);
        } else {
            addButton.setClickable(false);
            addButton.setAlpha(0.5f);
        }
    }

    /**
     * Create UserRecord with data from last processed image and user input.
     *
     * @param view current View
     */
    public void addUser(View view) {
        EditText usernameInput = findViewById(R.id.usernameInput);
        String username = usernameInput.getText().toString();
        Resources res = getResources();

        if (username.isEmpty()) {
            toastWrapper.showToast(res.getString(R.string.addFace_UsernameNotGiven_toast), Toast.LENGTH_SHORT);
            return;
        }

        for (Pair<ModelObject, float[]> model : models) {
            if (model.second == null) {
                toastWrapper.showToast(res.getString(R.string.addFace_calculationNotFinished_toast), Toast.LENGTH_SHORT);
                return;
            }
        }

        for (Pair<ModelObject, float[]> model : models) {
            UserRecord userRecord = new UserRecord(username, model.second);
            model.first.userDatabase.addUserRecord(userRecord);
        }

        toastWrapper.showToast(String.format(res.getString(R.string.addFace_UserAdded_toast), username), Toast.LENGTH_SHORT);
        finish();
    }

    /**
     * Process chosen photo using NeuralModel, display found face, save face vector.
     *
     * @param image Bitmap of face which will be preprocessed.
     */
    private void processPhoto(Bitmap image) {
        photoLoading(true);
        setAddButtonState(false);
        CompletableFuture.supplyAsync(() -> preProcessFace(image))
                .thenAccept(result -> {
                            CompletableFuture.runAsync(() -> displayFace(result));
                            CompletableFuture.runAsync(() -> processFace(result));
                        }
                );
    }

    /**
     * Hide face image and show loading animation if true passed,
     * show face image and hide loading animation if false passed.
     *
     * @param state desired state of loading animation.
     */
    void photoLoading(boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state) {
                    currentFaceImage.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    addFromCameraButton.setAlpha(0.5f);
                    addFromPhotoButton.setAlpha(0.5f);
                    addFromCameraButton.setClickable(false);
                    addFromPhotoButton.setClickable(false);
                } else {
                    currentFaceImage.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    addFromCameraButton.setAlpha(1);
                    addFromPhotoButton.setAlpha(1);
                    addFromCameraButton.setClickable(true);
                    addFromPhotoButton.setClickable(true);
                }
            }
        });
    }

    /**
     * Pre-process selected image or camera frame. Returns crop face image.
     *
     * @param image image or camera frame.
     * @return cropped face image.
     */
    private Bitmap preProcessFace(Bitmap image) {
        Resources res = getResources();
        try {
            return facePreProcessor.detectAndPreProcessOneFace(image);
        } catch (FaceProcessingException e) {
            e.printStackTrace();
            toastWrapper.showToast(res.getString(R.string.addFace_NotOneFaceFound_toast), Toast.LENGTH_SHORT);
            throw new CompletionException(e);
        }
    }

    /**
     * Convert detected face to bitmap and display it on face image View.
     *
     * @param face - Detected face.
     */
    private void displayFace(Bitmap face) {
        // Display found face on screen in ImageView
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentFaceImage.setImageBitmap(face);
                photoLoading(false);
            }
        });
    }

    /**
     * Process face image using neural model and enable add user button when done.
     *
     * @param face - Face image.
     */
    private void processFace(Bitmap face) {
        for (int i = 0; i < models.size(); i++) {
            models.set(i, new Pair<>(models.get(i).first, models.get(i).first.neuralModel.resizeAndProcess(face)));
        }
        // Unlock "add" button
        setAddButtonState(true);
    }

    /**
     * Start CameraActivity with in photo taking mode.
     *
     * @param view - Current view.
     */
    public void takePhoto(View view) {
        Intent takePhotoIntent = new Intent(this, CameraPreviewActivity.class);
        takePhotoIntent.putExtra(CameraPreviewActivity.CAMERA_MODE_KEY,
                CameraPreviewActivity.CameraPreviewMode.CAPTURE);
        takePhotoLauncher.launch(takePhotoIntent);
    }

    /**
     * Choose directory containing photos for bulk adding users.
     *
     * @param view - Current view.
     */
    public void chooseTestDirectory(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        chooseCatalogLauncher.launch(Intent.createChooser(intent, "Select Directory"));
    }

    /**
     * Add faces from each directory in passed root directory to database.
     *
     * @param directory - Root of directory with photos for bulk adding.
     */
    private void loadTestDirectory(Uri directory) {
        DocumentFile dir = DocumentFile.fromTreeUri(this, directory);
        DocumentFile[] files = dir.listFiles();
        if (files == null)
            return;
        for (DocumentFile file : files) {
            if (file.isDirectory() && !file.getName().matches("\\..*")) {
                loadFacesFromDirectory(file);
            }
        }
        finish();
    }

    /**
     * Process all photos in directory and add them to database with name of passed directory.
     *
     * @param directory - Directory containing photos of single person.
     */
    private void loadFacesFromDirectory(DocumentFile directory) {
        DocumentFile[] files = directory.listFiles();
        if (files == null)
            return;
        String name = directory.getName();
        for (DocumentFile file : files) {
            if (file.isFile()) {
                Bitmap photo = resolveContentToBitmap(file.getUri(), this);
                if (photo == null)
                    continue;
                processFace(preProcessFace(photo));
                for (Pair<ModelObject, float[]> model : models) {
                    UserRecord userRecord = new UserRecord(name, model.second);
                    model.first.userDatabase.addUserRecord(userRecord);
                }
            }
        }
        Log.d("Bulk add", "Added user: " + name);
    }
}
