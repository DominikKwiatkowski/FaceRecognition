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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.R;
import com.common.TransitionsLibrary;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.libs.facerecognition.FacePreprocessor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DisplayResultsLayout implements LayoutClassInterface {
    private final AppCompatActivity caller;
    private final AddPhotoLayout addPhotoLayout;

    private final List<Pair<String, Long>> benchmarkTimeResults = new ArrayList<>();
    private final FacePreprocessor facePreProcessor;
    private final List<Pair<String, String>> supportedModels;
    private ArrayList<BenchmarkResult> testResults = new ArrayList<>();
    private Button returnButton;
    private LinearLayout modelLegend;
    private ProgressBar progressBar;
    private LinearLayout showResultLayout;
    private ScrollView scrollView;

    public DisplayResultsLayout(AppCompatActivity caller, AddPhotoLayout addPhotoLayout, List<Pair<String, String>> supportedModels) {
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
        returnButton.setOnClickListener(v -> {
            for (Pair<String, String> supportedModel : supportedModels) {
                GlobalData.clearModel(supportedModel.first, supportedModel.second);
            }
            caller.finish();
            TransitionsLibrary.executeToRightTransition(caller);
        });
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
    private void processPhotos() {
        // 1. Pro process all photos
        ArrayList<Task<List<Face>>> faceDetectionTasks = new ArrayList<>();
        ArrayList<Bitmap> testPhotos = addPhotoLayout.testPhotos;
        for (Bitmap photo : testPhotos) {
            faceDetectionTasks.add(facePreProcessor.detectAllFacesUsingML(photo));
        }

        // 2. Gather all returned data
        Assert.assertEquals(faceDetectionTasks.size(), testPhotos.size());
        ArrayList<Bitmap> preProcessedFaces = new ArrayList<>();
        for (int i = 0; i < faceDetectionTasks.size(); i++) {
            facePreProcessor.waitForTask(faceDetectionTasks.get(i));
            preProcessedFaces.addAll(facePreProcessor.preProcessAllFaces(
                    testPhotos.get(i),
                    faceDetectionTasks.get(i).getResult()));
        }

        // 3. Creates result structure
        for (Bitmap face : preProcessedFaces) {
            if (face != null) {
                testResults.add(new BenchmarkResult(face));
            }
        }

        // 4. Run process models. Running on seprate threads affects result, so it will be run in
        // single one.
        for (Pair<String, String> model : supportedModels) {
            benchmarkTimeResults.add(new Pair(model.first, processModel(model.first, model.second)));
        }

        caller.runOnUiThread(this::displayResults);
    }

    /**
     * Process all faces in given model.
     *
     * @param modelName    - name of model on which we want to process data
     * @param databaseName - name of database which we want to use in face prediction
     * @return Time taken to proceed
     */
    private Long processModel(String modelName, String databaseName) {
        long startTime = System.nanoTime();
        ModelObject model = GlobalData.getModel(caller, modelName, databaseName);
        for (BenchmarkResult processingResult : testResults) {
            UserRecord record = model.userDatabase.findClosestRecordBelowThreshold(
                    model.neuralModel.resizeAndProcess(processingResult.getPhoto()));
            processingResult.addResult(modelName, record.username);
        }
        long endTime = System.nanoTime();

        return (endTime - startTime) / 1000000;
    }

    /**
     * Display all gathered processing results on screen
     */
    private void displayResults() {
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
    private void showTimeResult() {
        for (Pair<String, Long> result : benchmarkTimeResults) {

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
    private void showModelsResults() {
        int pixels = 100;
        AtomicInteger id = new AtomicInteger(1);
        for (BenchmarkResult result : testResults) {

            // Create horizontal layout to collect all data for one face.
            RelativeLayout objectLayout = new RelativeLayout(caller);

            // Create and setup image view
            ImageView faceMini = new ImageView(caller);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(3 * pixels, 3 * pixels);
            layoutParams.setMargins(pixels / 2, 0, pixels / 2, pixels);
            faceMini.setImageBitmap(result.getPhoto());

            faceMini.setLayoutParams(layoutParams);
            faceMini.setId(id.getAndIncrement());
            objectLayout.addView(faceMini);

            AtomicReference<TextView> previous = new AtomicReference<>();
            previous.set(null);
            // Add textView for every result model.
            result.getResults().forEach((model, name) -> {
                // Create and setup text view for model output
                TextView modelResultView = new TextView(caller);
                modelResultView.setText(model + ": " + name);
                modelResultView.setId(id.getAndIncrement());

                RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                viewParams.addRule(RelativeLayout.RIGHT_OF, faceMini.getId());
                if (previous.get() != null) {
                    viewParams.addRule(RelativeLayout.BELOW, previous.get().getId());
                }

                modelResultView.setPadding(pixels / 10, pixels / 10, pixels / 10, pixels / 10);
                objectLayout.addView(modelResultView, viewParams);
                previous.set(modelResultView);
            });

            showResultLayout.addView(objectLayout);
        }
    }
}
