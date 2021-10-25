package com.libs.benchmark;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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

    private void processPhotos(){
        ArrayList<Task<List<Face>>> faceDetectionTasks = new ArrayList<>();
        ArrayList<Bitmap> testPhotos = addPhotoLayout.testPhotos;
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
        caller.runOnUiThread(this::displayResults);
    }

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

    private void displayResults(){
        progressBar.setVisibility(View.INVISIBLE);
        showTimeResult();
        showModelsResults();
        returnButton.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.VISIBLE);
        showResultLayout.setVisibility(View.VISIBLE);
    }

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

    private void showModelsResults(){
        for(BenchmarkResult result :testResults){
            ImageView faceMini = new ImageView(caller);
            faceMini.setImageBitmap(result.getPhoto());
            faceMini.setForegroundGravity(Gravity.CENTER);
            faceMini.setMaxHeight(faceSize);
            faceMini.setMaxWidth(faceSize);
            showResultLayout.addView(faceMini);
            result.getResults().forEach((model,name) ->{
                TextView modelResultView = new TextView(caller);
                modelResultView.setGravity(Gravity.CENTER);
                modelResultView.setText(model + ": " + name);
                showResultLayout.addView(modelResultView);
            });
        }
    }
}
