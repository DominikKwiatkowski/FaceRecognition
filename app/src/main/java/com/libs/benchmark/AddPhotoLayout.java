package com.libs.benchmark;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
    private Button leftButton;
    private Button rightButton;
    private Button deletePhotoButton;
    private TextView numberOfPhotosTextView;
    private final ActivityResultLauncher<Intent> choosePhotoLauncher;

    private final ActivityResultLauncher<Intent> takePhotoLauncher;
    private int indexer = 0;

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
        Button cancel = caller.findViewById(R.id.benchCancelButton);
        cancel.setOnClickListener(v -> cancel());

        Button add = caller.findViewById(R.id.benchAddButton);
        add.setOnClickListener(v -> addTestPhotos());

        Button chosePhoto = caller.findViewById(R.id.benchChoosePhotoButton);
        chosePhoto.setOnClickListener(v -> chosePhoto());

        Button makePhoto = caller.findViewById(R.id.benchMakePhotoButton);
        makePhoto.setOnClickListener(v -> makePhoto());

        leftButton = caller.findViewById(R.id.leftButton);
        leftButton.setOnClickListener(v -> previousPhoto());
        rightButton = caller.findViewById(R.id.rightButton);
        rightButton.setOnClickListener(v -> nextPhoto());
        addPhoto_ImageView = caller.findViewById(R.id.benchSelectedImage);

        deletePhotoButton = caller.findViewById(R.id.deletePhotoButton);
        deletePhotoButton.setOnClickListener(v -> deletePhoto());

        numberOfPhotosTextView = caller.findViewById(R.id.numberOfPhotosTextView);
        updateUI();
    }

    private void cancel(){
        tempPhotos.clear();
        benchmarkLayout.makeActive();
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
        indexer = tempPhotos.size()-1;
        updateUI();
    }

    private void updateUI(){
        if(tempPhotos.size()>0) {
            addPhoto_ImageView.setImageBitmap(tempPhotos.get(indexer));
            deletePhotoButton.setVisibility(View.VISIBLE);
        }
        else{
            addPhoto_ImageView.setImageResource(0);
            deletePhotoButton.setVisibility(View.INVISIBLE);
        }

        if(indexer == 0){
            leftButton.setVisibility(View.INVISIBLE);
        }
        else{
            leftButton.setVisibility(View.VISIBLE);
        }
        if(indexer >= tempPhotos.size() - 1){
            rightButton.setVisibility(View.INVISIBLE);
        }
        else{
            rightButton.setVisibility(View.VISIBLE);
        }

        numberOfPhotosTextView.setText("Liczba zdjęć: " + tempPhotos.size());
    }

    private void nextPhoto(){
        indexer++;
        updateUI();
    }

    private void previousPhoto(){
        indexer--;
        updateUI();
    }

    private void deletePhoto(){
        tempPhotos.remove(indexer);
        if(indexer > 0)
            indexer--;
        updateUI();
    }
}
