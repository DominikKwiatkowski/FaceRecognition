package com.libs.benchmark;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.libs.facerecognition.FacePreProcessor;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;
import com.libs.globaldata.userdatabase.UserRecord;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DisplayResultsLayout  implements LayoutClassInterface{
    private final AppCompatActivity caller;
    private final AddPhotoLayout addPhotoLayout;

    private final ArrayList<Pair<String, Long>> benchmarkTimeResults = new ArrayList<>();
    private ArrayList<BenchmarkResult> testResults= new ArrayList<>();
    private Button returnButton;
    private LinearLayout modelLegend;
    private ProgressBar progressBar;
    private LinearLayout showResultLayout;
    private ScrollView scrollView;
    private final FacePreProcessor facePreProcessor;
    private final int faceSize = 300;
    private final ArrayList<Pair<String, String>> supportedModels;

    public DisplayResultsLayout(AppCompatActivity caller, AddPhotoLayout addPhotoLayout, ArrayList<Pair<String, String>> supportedModels){
        this.caller = caller;
        this.addPhotoLayout = addPhotoLayout;
        this.supportedModels = supportedModels;
        facePreProcessor = GlobalData.getFacePreProcessor();
    }

    @Override
    public void makeActive() {
        caller.setContentView(R.layout.activity_benchmark_result);
        modelLegend = caller.findViewById(R.id.showResultModelLegendTable);
        progressBar = caller.findViewById(R.id.showResultProgressBar);
        returnButton = caller.findViewById(R.id.showResultReturnButton);
        returnButton.setOnClickListener(v->caller.finish());
        showResultLayout = caller.findViewById(R.id.showResultLayout);
        scrollView = caller.findViewById(R.id.showResultView);
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> processPhotos());
    }

    /**
     * Process all photos. This function should be run in separate thread. It pre process photo,
     * creates results structure. Also creates thread for every model(max 6 threads) and run
     * processModel in this threads.
     */
    private void processPhotos(){
        // 1. Pro process all photos
        ArrayList<Task<List<Face>>> faceDetectionTasks = new ArrayList<>();
        ArrayList<Bitmap> testPhotos = addPhotoLayout.testPhotos;
        for(Bitmap photo : testPhotos){
            faceDetectionTasks.add(facePreProcessor.detectAllFacesUsingML(photo));
        }

        // 2. Gather all returned data
        Assert.assertEquals(faceDetectionTasks.size(),testPhotos.size());
        ArrayList<Bitmap> preProcessedFaces = new ArrayList<>();
        for(int i = 0; i< faceDetectionTasks.size();i++){
            facePreProcessor.waitForTask(faceDetectionTasks.get(i));
            preProcessedFaces.addAll(facePreProcessor.preProcessAllFaces(
                    testPhotos.get(i),
                    faceDetectionTasks.get(i).getResult()));
        }

        // 3. Creates result structure
        for(Bitmap face : preProcessedFaces){
            testResults.add(new BenchmarkResult(face));
        }

        // 4. Run process models in separate threads
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        ArrayList<Pair<String, Future<Long>>> results = new ArrayList<>();
        for(Pair<String,String> model : supportedModels){
            results.add(new Pair(
                    model.first,
                    executorService.submit(() ->processModel(model.first,model.second))
            ));
        }

        // 5. Gather time results from model processing
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
        caller.runOnUiThread(this::displayResults);
    }

    /**
     * Process all faces in given model.
     * @param modelName - name of model on which we want to process data
     * @param databaseName - name of database which we want to use in face prediction
     * @return Time taken to proceed
     */
    private Long processModel(String modelName, String databaseName){
        long startTime = System.nanoTime();
        ModelObject model = GlobalData.getModel(caller,modelName, databaseName);
        for(BenchmarkResult processingResult : testResults){
            UserRecord record = model.userDatabase.findClosestRecord(
                    model.neuralModel.resizeAndProcess(processingResult.getPhoto())[0]);
            processingResult.addResult(modelName, record.username);
        }
        long endTime = System.nanoTime();

        return (endTime - startTime)/1000000;
    }

    /**
     * Display all gathered processing results on screen
     */
    private void displayResults(){
        progressBar.setVisibility(View.INVISIBLE);
        showTimeResult();
        showModelsResults();
        returnButton.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.VISIBLE);
        showResultLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Show time results in headline of activity
     */
    private void showTimeResult(){
        for (Pair<String, Long> result : benchmarkTimeResults){

            TextView timeResultView = new TextView(caller);
            timeResultView.setText(result.first + ": " +
                    String.format(Locale.getDefault(),
                            "%02d.%d sec",
                            TimeUnit.MILLISECONDS.toSeconds(result.second),
                            result.second - TimeUnit.SECONDS.toMillis(
                                    TimeUnit.MILLISECONDS.toSeconds(result.second))
                    ));
            timeResultView.setGravity(Gravity.CENTER_HORIZONTAL);

            modelLegend.addView(timeResultView);
        }
        modelLegend.setVisibility(View.VISIBLE);
    }

    /**
     * Show model result for every proceeded face.
     */
    private void showModelsResults(){
        for(BenchmarkResult result :testResults){
            
            // Create horizontal layout to collect all data for one face.
            LinearLayout horizontalLayout = new LinearLayout(caller);
            horizontalLayout.setGravity(Gravity.CENTER);

            // Create and setup image view
            ImageView faceMini = new ImageView(caller);
            faceMini.setImageBitmap(result.getPhoto());
            faceMini.setForegroundGravity(Gravity.LEFT);
            faceMini.setMaxHeight(faceSize);
            faceMini.setMaxWidth(faceSize);
            faceMini.setMinimumWidth(faceSize);
            faceMini.setMinimumHeight(faceSize);
            horizontalLayout.addView(faceMini);

            // Create vertical layout to gather all models results.
            LinearLayout verticalLayout = new LinearLayout(caller);
            horizontalLayout.addView(verticalLayout);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);

            // Add textView for every result model.
            result.getResults().forEach((model,name) ->{
                TextView modelResultView = new TextView(caller);
                modelResultView.setText(model + ": " + name);
                verticalLayout.addView(modelResultView);
            });

            showResultLayout.addView(horizontalLayout);
            // Add space between each result to make it clearer
            Space space = new Space(caller);
            space.setMinimumHeight(60);
            showResultLayout.addView(space);
        }
    }
}