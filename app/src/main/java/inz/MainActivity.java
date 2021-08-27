package inz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.imgcodecs.Imgcodecs;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class MainActivity extends AppCompatActivity {

    Button pickButton;
    Button countButton;
    Resources res;
    Uri fileUri = null;
    float [][][] result = new float[3][][];
    Mat photos[] = new Mat[3];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        res = this.res;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        if(OpenCVLoader.initDebug())
        {
            Log.d("OPENCV", "OpenCv loaded succesfully");
        }
        pickButton = findViewById(R.id.FileButton);
        countButton = findViewById(R.id.countButton);

        NeuralModel model = new NeuralModel(this, "Facenet-optimized.tflite");

        pickButton.setOnClickListener(v -> getFile(Uri.fromFile(Environment.getExternalStorageDirectory())));

        try {
            photos[0] = Utils.loadResource(this.getApplicationContext(),R.drawable.kwiaciu1);
            photos[1] = Utils.loadResource(this.getApplicationContext(),R.drawable.macius);
            photos[2] = Utils.loadResource(this.getApplicationContext(),R.drawable.kwiaciu2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        countButton.setOnClickListener(v -> {
            for(int i = 0;i<photos.length;i++) {
                MatOfRect faces = model.detectAllFaces(photos[i]);
                ArrayList<Mat> faceImages = model.preProcessAllFaces(photos[i], faces);
                TensorImage image = model.changeImageRes(faceImages.get(0));
                result[i] = model.processImage(image);
            }

            Log.i("score1-3",norm(result[0][0],result[2][0]).toString());
            Log.i("score2-3",norm(result[1][0],result[2][0]).toString());
        });
    }

    private Double norm(float [] first, float [] second)
    {
        float ans = 0;
        for(int i = 0;i<first.length;i++)
        {
            ans += pow(first[i]-second[i],2);
        }
        return sqrt(ans);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 100)
        {
            if (resultCode == RESULT_OK)
            {
                if( data != null)
                {
                    fileUri = data.getData();
                }
            }
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults)
    {
        if(requestCode == 0)
        {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle("Crucial permission not granted, application will be closed");
                adb.setPositiveButton("Tak",
                        (dialog, which) -> MainActivity.super.finish());
                adb.create().show();
            }
        }
    }

    private void getFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, 100);
    }

    // creates menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Function is responsible for
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent i;
        switch (item.getItemId())
        {

        }
        return super.onOptionsItemSelected(item);
    }
}