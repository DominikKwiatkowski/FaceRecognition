package com;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.UserDatabase.UserDatabase;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class FaceRecognition extends AppCompatActivity {
    private static final int PICK_PHOTO = 100;
    private NeuralModel model = null;
    private UserDatabase userDatabase = null;
    private Uri fileUri = null;
    private final int numOfPhotos = 3;

    // Result from neural network is 2-dimension array, so we create numOfPhotos of them.
    float [][][] result = new float[numOfPhotos][][];

    Mat[] photos = new Mat[numOfPhotos];
    final ArrayList<String> permissions = new ArrayList<>();

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Check if permission is already given - if not, ask for it.
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED)
        {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED)
        {
            permissions.add(Manifest.permission.CAMERA);
        }

        if(!permissions.isEmpty())
        {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[permissions.size()]), 0);
        }

        // Load OpenCv
        if(OpenCVLoader.initDebug())
        {
            Log.d("OPENCV", "OpenCv loaded succesfully");
        }

        // Load NeuralModel
        model = NeuralModel.getInstance(getApplicationContext(),"Facenet-optimized.tflite");
        // model = new NeuralModel(this, "Facenet-optimized.tflite");
        // Load test images from resources
        try {
            photos[0] = Utils.loadResource(this.getApplicationContext(), R.drawable.kwiaciu1);
            photos[1] = Utils.loadResource(this.getApplicationContext(), R.drawable.macius);
            photos[2] = Utils.loadResource(this.getApplicationContext(), R.drawable.kwiaciu2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize database
        // TODO: Temporary. Later on it should be moved to the model selection menu
        //  (database will be defined per model).
        userDatabase = UserDatabase.getInstance(
                getApplicationContext(),        // App specific internal storage location
                "Facenet",        // Model name TODO: temporary
                128                // Vector size TODO: temporary
        );

    }

    /**
     *
     * @param first array of floats from neural model, on which norm will be calculated.
     * @param second array of floats from neural model, on which norm will be calculated.
     * @return difference between this 2 arrases.
     */
    private Double norm(float [] first, float [] second)
    {
        float ans = 0;
        for(int i = 0; i < first.length; i++)
        {
            ans += pow(first[i] - second[i], 2);
        }
        return sqrt(ans);
    }

    /**
     *
     * @param requestCode code of request, each request should have unique code.
     * @param resultCode result code for operation, defined by android api.
     * @param data returned data from intent.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Result for browsing storage made in getFile function.
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if(data == null)
                return;

        }

        super.onActivityResult(requestCode,resultCode,data);
    }



    /**
     * Function to menage user response within granting permissions.
     * @param requestCode code of permission request.
     * @param permissions name of permissions, which was requested.
     * @param grantResults result value, it might be for fiew permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if(requestCode == 0)
        {
            for(int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    adb.setTitle("Crucial permission not granted, application will be closed");
                    adb.setPositiveButton("Tak",
                            (dialog, which) -> FaceRecognition.super.finish());
                    adb.create().show();
                }
            }
        }
    }

    /**
     * Function call intent to pick file from disk. It will only allow photos to be picked
     * @param pickerInitialUri uri to begin picking file from
     */
    private void getPhoto(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, PICK_PHOTO);
    }

    /**
     *
     * @param menu class of menu to be used. It should be some resource within menu directory.
     * @return true if successful.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Options menu callback.
     *
     * @param item - item chosen by user
     * @return true if successful.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.cameraScreen:
                Intent i = new Intent(this, CameraActivity.class);
                startActivity(i);
                break;
            case R.id.sampleDatabase:
                userDatabase.loadSampleDatabase();
                break;
            case R.id.loadDatabase:
                userDatabase.loadDatabase();
                break;
            case R.id.saveDatabase:
                userDatabase.saveDatabase();
                break;
            case R.id.addUser:
                Intent addFaceIntent = new Intent(this, AddFace.class);
                startActivity(addFaceIntent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * calculateButton onClick callback.
     *
     * @param view - application's view
     */
    public void calculateButtonOnClick(View view) {
        // Process and proceed all test photos
        for(int i = 0; i < photos.length; i++) {
            MatOfRect faces = model.detectAllFaces(photos[i]);
            ArrayList<Mat> faceImages = model.preProcessAllFaces(photos[i], faces);
            TensorImage image = model.changeImageRes(faceImages.get(0));
            result[i] = model.processImage(image);
        }

        // Print difference result
        Log.i("score1-3", norm(result[0][0], result[2][0]).toString());
        Log.i("score2-3", norm(result[1][0], result[2][0]).toString());
    }

    /**
     *  fileButton onClick callback.
     *
     * @param view - application's view
     */
    public void fileButtonOnClick(View view) {
        getPhoto(Uri.fromFile(Environment.getExternalStorageDirectory()));
    }
}
