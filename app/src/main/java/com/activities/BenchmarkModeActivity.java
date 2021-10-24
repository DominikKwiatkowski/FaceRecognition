package com.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.R;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.libs.facerecognition.BenchmarkResult;
import com.libs.facerecognition.FacePreprocessor;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;
import com.libs.globaldata.userdatabase.UserRecord;

import org.junit.Assert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.common.BitmapOperations.resolveContentToBitmap;


public class BenchmarkModeActivity extends AppCompatActivity {
    // main benchmark layout
    private ArrayList<Pair<String, String>> supportedModels = new ArrayList<>();
    private Button addUserButton;
    private Button addPhotoButton;
    private Button testButton;

    // add photo benchmark layout
    private ArrayList<Bitmap> testPhotos = new ArrayList<>();
    private ArrayList<Bitmap> tempPhotos = new ArrayList<>();

    private Button addPhoto_MakePhoto;
    private Button addPhoto_ChosePhoto;
    private Button addPhoto_Cancel;
    private Button addPhoto_Add;
    private ImageView addPhoto_ImageView;

    private ActivityResultLauncher<Intent> choosePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Process picked image
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // show picked image
                    Bitmap photo = resolveContentToBitmap(result.getData().getData(), this);
                    addNewTempPhoto(photo);
                }
            });

    private ActivityResultLauncher<Intent> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Process photo taken from camera
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Get filename from activity result, read photo from internal app storage and process it
                    try {
                        String filename = result.getData().getStringExtra(CameraPreviewActivity.CAPTURED_FRAME_KEY);
                        FileInputStream fis = getApplicationContext().openFileInput(filename);
                        Bitmap photo = BitmapFactory.decodeStream(fis);
                        addNewTempPhoto(photo);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });

    // display results layout
    private ArrayList<Pair<String, Long>> benchmarkTimeResults = new ArrayList<>();
    private ArrayList<BenchmarkResult> testResults= new ArrayList<>();
    private Button result_ReturnButton;
    private LinearLayout result_ModelLegend;
    private ProgressBar result_ProgressBar;
    private LinearLayout result_ShowResultLayout;
    private ScrollView result_ScrollView;
    private FacePreprocessor facePreProcessor;
    private final int faceSize = 300;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBenchmarkLayout();

        facePreProcessor = GlobalData.getFacePreProcessor();

        SharedPreferences userSettings = GlobalData.getUserSettings(this);

        // Read supported models.
        for(String model : getResources().getStringArray(R.array.models)){
            if(userSettings.getBoolean(model + getString(R.string.settings_benchModel_suffix),true)) {
                supportedModels.add(new Pair<>(model,model + getString(R.string.benchmarkMode_modelDatabaseName_suffix)));
            }
        }
    }

    private void setupBenchmarkLayout(){
        setContentView(R.layout.activity_benchmark_mode);
        addUserButton = findViewById(R.id.BenchAddFace);
        addUserButton.setOnClickListener(v -> addUser());

        addPhotoButton = findViewById(R.id.BenchMakePhoto);
        addPhotoButton.setOnClickListener(v -> addPhoto());

        testButton = findViewById(R.id.BenchTest);
        testButton.setOnClickListener(v->test());
    }

    private void addUser() {
        Intent addFaceIntent = new Intent(this, AddFaceActivity.class);
        ArrayList<String> chosenModels= new ArrayList<>();
        for(Pair<String, String> model :supportedModels){
            chosenModels.add(model.first);
            chosenModels.add(model.second);
        }
        addFaceIntent.putExtra(getString(R.string.addFace_ChooseModelName_intentValue),chosenModels);
        startActivity(addFaceIntent);
    }

    private void addPhoto(){
        setupAddPhotoLayout();
    }


    private void test(){
        setupResultDisplay();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> processPhotos());
    }

    // Add photo layouts methods
    private void setupAddPhotoLayout(){
        setContentView(R.layout.activity_benchmark_add_photo);
        addPhoto_Cancel = findViewById(R.id.benchCancel);
        addPhoto_Cancel.setOnClickListener(v -> cancel());

        addPhoto_Add = findViewById(R.id.benchAdd);
        addPhoto_Add.setOnClickListener(v -> addTestPhotos());

        addPhoto_ChosePhoto = findViewById(R.id.benchChoosePhoto);
        addPhoto_ChosePhoto.setOnClickListener(v -> chosePhoto());

        addPhoto_MakePhoto = findViewById(R.id.benchMakePhoto);
        addPhoto_MakePhoto.setOnClickListener(v -> makePhoto());

        addPhoto_ImageView = findViewById(R.id.benchSelectedImage);
    }

    private void cancel(){
        if(tempPhotos.size()==0) {
            setupBenchmarkLayout();
        }
        else{
            tempPhotos.remove(tempPhotos.size()-1);
            displayPhoto();
        }
    }

    private void addTestPhotos(){
        testPhotos.addAll(tempPhotos);
        tempPhotos.clear();
        setupBenchmarkLayout();
    }

    private void makePhoto(){
        Intent takePhotoIntent = new Intent(this, CameraPreviewActivity.class);
        takePhotoIntent.putExtra(CameraPreviewActivity.CAMERA_MODE_KEY,
                CameraPreviewActivity.CameraPreviewMode.CAPTURE);
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

    private void setupResultDisplay(){
        setContentView(R.layout.activity_benchmark_result);
        result_ModelLegend = findViewById(R.id.showResultModelLegendTable);
        result_ProgressBar = findViewById(R.id.showResultProgressBar);
        result_ReturnButton = findViewById(R.id.showResultReturnButton);
        result_ReturnButton.setOnClickListener(v->finish());
        result_ShowResultLayout = findViewById(R.id.showResultLayout);
        result_ScrollView = findViewById(R.id.showResultView);
    }

    private void processPhotos(){
        ArrayList<Task<List<Face>>> faceDetectionTasks = new ArrayList<>();
        for(Bitmap photo : testPhotos){
            faceDetectionTasks.add(facePreProcessor.detectAllFacesUsingML(photo));
        }
        Assert.assertEquals(faceDetectionTasks.size(),testPhotos.size());
        ArrayList<Bitmap> preProcessedFaces = new ArrayList<>();
        for(int i = 0; i< faceDetectionTasks.size();i++){
            facePreProcessor.waitForTask(faceDetectionTasks.get(i));
            preProcessedFaces.addAll(facePreProcessor.preProcessAllFaces(
                    testPhotos.get(i),
                    faceDetectionTasks.get(i).getResult()));
        }
        for(Bitmap face : preProcessedFaces){
            testResults.add(new BenchmarkResult(face));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(6);
        ArrayList<Pair<String, Future<Long>>> results = new ArrayList<>();
        for(Pair<String,String> model : supportedModels){
            results.add(new Pair(
                    model.first,
                    executorService.submit(() ->processModel(model.first,model.second))
            ));
        }

        for(Pair<String, Future<Long>> result : results){
            try {
                Long time = result.second.get();
                benchmarkTimeResults.add(new Pair(result.first,time));
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        runOnUiThread(this::displayResults);
    }

    private Long processModel(String modelName, String databaseName){
        long startTime = System.nanoTime();
        ModelObject model = GlobalData.getModel(this,modelName, databaseName);
        for(BenchmarkResult processingResult : testResults){
            UserRecord record = model.userDatabase.findClosestRecord(
                    model.neuralModel.resizeAndProcess(processingResult.getPhoto())[0]);
            processingResult.addResult(modelName, record.username);
        }
        long endTime = System.nanoTime();

       return (endTime - startTime)/1000000;
    }

    private void displayResults(){
        result_ProgressBar.setVisibility(View.INVISIBLE);
        displayResults_showTimeResult();
        displayResults_showModelsResults();
        result_ReturnButton.setVisibility(View.VISIBLE);
    }

    private void displayResults_showTimeResult(){
        for (Pair<String, Long> result : benchmarkTimeResults){

            TextView timeResultView = new TextView(this);
            timeResultView.setText(result.first + ": " +
                    String.format(Locale.getDefault(),
                    "%02d.%d sec",
                    TimeUnit.MILLISECONDS.toSeconds(result.second),
                        result.second - TimeUnit.SECONDS.toMillis(
                                TimeUnit.MILLISECONDS.toSeconds(result.second))
            ));
            timeResultView.setGravity(Gravity.CENTER_HORIZONTAL);

            result_ModelLegend.addView(timeResultView);
        }
        result_ModelLegend.setVisibility(View.VISIBLE);
    }

    private void displayResults_showModelsResults(){
        result_ScrollView.setVisibility(View.VISIBLE);
        result_ShowResultLayout.setVisibility(View.VISIBLE);

        for(BenchmarkResult result :testResults){
            ImageView faceMini = new ImageView(this);
            faceMini.setImageBitmap(result.getPhoto());
            faceMini.setForegroundGravity(Gravity.CENTER);
            faceMini.setMaxHeight(faceSize);
            faceMini.setMaxWidth(faceSize);
            result_ShowResultLayout.addView(faceMini);
            result.getResults().forEach((model,name) ->{
                TextView modelResultView = new TextView(this);
                modelResultView.setGravity(Gravity.CENTER);
                modelResultView.setText(model + ": " + name);
                result_ShowResultLayout.addView(modelResultView);
            });
        }
    }
}