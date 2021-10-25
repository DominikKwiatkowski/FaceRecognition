package com.libs.benchmark;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.activities.CameraActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import static com.common.BitmapOperations.resolveContentToBitmap;

public class AddPhotoLayout implements LayoutClassInterface{
    AppCompatActivity caller;
    LayoutClassInterface benchmarkLayout;
    public ArrayList<Bitmap> testPhotos = new ArrayList<>();
    private ArrayList<Bitmap> tempPhotos = new ArrayList<>();

    private ImageView addPhoto_ImageView;

    private final ActivityResultLauncher<Intent> choosePhotoLauncher;

    private final ActivityResultLauncher<Intent> takePhotoLauncher;

    public AddPhotoLayout(AppCompatActivity caller, LayoutClassInterface benchmarkLayout){
        this.caller = caller;
        this.benchmarkLayout = benchmarkLayout;

        choosePhotoLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Process picked image
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // show picked image
                        Bitmap photo = resolveContentToBitmap(result.getData().getData(), caller);
                        addNewTempPhoto(photo);
                    }
                });

        takePhotoLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Process photo taken from camera
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Get filename from activity result, read photo from internal app storage and process it
                        try {
                            String filename = result.getData().getStringExtra("UserPhoto");
                            FileInputStream fis = caller.getApplicationContext().openFileInput(filename);
                            Bitmap photo = BitmapFactory.decodeStream(fis);
                            addNewTempPhoto(photo);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
    @Override
    public void makeActive() {
        caller.setContentView(R.layout.activity_benchmark_add_photo);
        Button cancel = caller.findViewById(R.id.benchCancel);
        cancel.setOnClickListener(v -> cancel());

        Button add = caller.findViewById(R.id.benchAdd);
        add.setOnClickListener(v -> addTestPhotos());

        Button chosePhoto = caller.findViewById(R.id.benchChoosePhoto);
        chosePhoto.setOnClickListener(v -> chosePhoto());

        Button makePhoto = caller.findViewById(R.id.benchMakePhoto);
        makePhoto.setOnClickListener(v -> makePhoto());

        addPhoto_ImageView = caller.findViewById(R.id.benchSelectedImage);
    }

    private void cancel(){
        if(tempPhotos.size()==0) {
            benchmarkLayout.makeActive();
        }
        else{
            tempPhotos.remove(tempPhotos.size()-1);
            displayPhoto();
        }
    }

    private void addTestPhotos(){
        testPhotos.addAll(tempPhotos);
        tempPhotos.clear();
        benchmarkLayout.makeActive();
    }

    private void makePhoto(){
        Intent takePhotoIntent = new Intent(caller, CameraActivity.class);
        takePhotoIntent.putExtra("TakePhotoMode", true);
        takePhotoLauncher.launch(takePhotoIntent);
    }

    private void chosePhoto(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        choosePhotoLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void addNewTempPhoto(Bitmap image){
        tempPhotos.add(image);
        displayPhoto();
    }

    private void displayPhoto(){
        if(tempPhotos.size()>0) {
            addPhoto_ImageView.setImageBitmap(tempPhotos.get(tempPhotos.size() - 1));
        }
        else{
            addPhoto_ImageView.setImageResource(0);
        }
    }
}
