package inz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button pickButton;
    Button countButton;

    Uri fileUri = null;
    float [][] result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        pickButton = findViewById(R.id.FileButton);
        countButton = findViewById(R.id.countButton);

        NeuralModel model = new NeuralModel(this);

        pickButton.setOnClickListener(v -> getFile(Uri.fromFile(Environment.getExternalStorageDirectory())));

        countButton.setOnClickListener(v -> {
            if(fileUri != null)
            {
                Bitmap photo = null;
                try {
                    photo = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), fileUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(photo != null) {
                    TensorImage image = model.prepareImage(photo);
                    result = model.processImage(image);
                }
            }
        });
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
}