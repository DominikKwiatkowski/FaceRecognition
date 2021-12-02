package com.libs.facerecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.IOException;
import java.util.HashMap;

/**
 * Represents neural model. User can define which one will be used.
 */
public class NeuralModel {
    public final String Tag;

    protected final String modelFilename;
    private final int imageHeight;
    private final int imageWidth;
    private final int outputSize;

    private final ImageProcessor imageProcessor;
    private Interpreter model;

    /**
     * Class constructor.
     *
     * @param modelFilename name of the model's file to be used. All models must be inside ml folder
     */
    protected NeuralModel(String Tag, String modelFilename, Context context) {
        this.Tag = Tag;
        this.modelFilename = modelFilename;

        try {
            Interpreter.Options options = new Interpreter.Options();
            CompatibilityList compatList = new CompatibilityList();
            if (compatList.isDelegateSupportedOnThisDevice()) {
                // if the device has a supported GPU, add the GPU delegate
                GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
                options.addDelegate(gpuDelegate);
            } else {
                // if the GPU is not supported, run on 4 threads
                options.setNumThreads(4);
            }

            model = new Interpreter(FileUtil.loadMappedFile(context, this.modelFilename), options);
        } catch (IOException e) {
            Log.e(this.Tag, "Error reading model", e);
        }

        imageHeight = model.getInputTensor(0).shape()[1];
        imageWidth = model.getInputTensor(0).shape()[2];
        outputSize = model.getOutputTensor(0).shape()[1];

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(imageHeight, imageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(127.5f, 127.5f))
                .build();
    }

    /**
     * Get model's output size.
     *
     * @return size of the output of the model
     */
    public int getOutputSize() {
        return outputSize;
    }

    /**
     * Prepare image to be putted inside neural network. It will change resolution and format of
     * given image to resolution needed by neural network.
     *
     * @param image image to be prepared
     * @return tImage image processed and ready to be putted by neural network.
     */
    public TensorImage changeImageRes(Bitmap image) {
        if (image == null) {
            return null;
        }

        TensorImage tImage = new TensorImage(DataType.UINT8);
        tImage.load(image);

        return imageProcessor.process(tImage);
    }

    /**
     * Resize and process image
     *
     * @param image image to be prepared
     * @return probabilityBuffer Buffer of face properties
     */
    public float[] resizeAndProcess(Bitmap image) {
        return processImage(changeImageRes(image));
    }

    /**
     * Receive target image and serialize it to image's vector using neural network model.
     *
     * @param tImage image to be processed by network. It must be preprocessed
     * @return probabilityBuffer Buffer of face properties. Sized of buffer must be specified
     * @throws NullPointerException in case of null image
     */
    public synchronized float[] processImage(TensorImage tImage) {
        if (tImage == null) {
            return null;
        }

        float[][] probabilityBuffer = new float[1][outputSize];
        model.run(tImage.getBuffer(), probabilityBuffer);
        Log.i(Tag + modelFilename, "Processed image successfully");

        return probabilityBuffer[0];
    }


}
