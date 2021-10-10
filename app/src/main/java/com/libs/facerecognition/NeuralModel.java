package com.libs.facerecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.R;
import com.common.FaceProcessingException;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static java.lang.Math.atan2;
import static org.junit.Assert.assertEquals;
import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;
import static org.opencv.imgproc.Imgproc.warpAffine;

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
            if(compatList.isDelegateSupportedOnThisDevice()){
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
     * @throws NullPointerException in case of null image
     */
    public TensorImage changeImageRes(Mat image) {
        if (image == null) {
            throw new NullPointerException("Image can't be null");
        }

        TensorImage tImage = new TensorImage(DataType.UINT8);
        Bitmap bitmap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        tImage.load(bitmap);

        return imageProcessor.process(tImage);
    }

    /**
     * Resize and process image
     *
     * @param image image to be prepared
     * @return probabilityBuffer Buffer of face properties
     */
    public float[][] resizeAndProcess(Mat image) {
        return processImage(changeImageRes(image));
    }

    /**
     * Receive target image and serialize it to image's vector using neural network model.
     *
     * @param tImage image to be processed by network. It must be preprocessed
     * @return probabilityBuffer Buffer of face properties. Sized of buffer must be specified
     * @throws NullPointerException in case of null image
     */
    public float[][] processImage(TensorImage tImage) {
        if (tImage == null) {
            throw new NullPointerException("Image can't be null");
        }

        float[][] probabilityBuffer = new float[1][outputSize];
        model.run(tImage.getBuffer(), probabilityBuffer);
        Log.i(Tag + modelFilename, "Processed image successfully");

        return probabilityBuffer;
    }


}
