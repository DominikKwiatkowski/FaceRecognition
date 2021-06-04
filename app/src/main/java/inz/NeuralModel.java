package inz;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import java.io.IOException;
import java.lang.reflect.Array;

    public class NeuralModel {
        private int inputSize = 160;
        private int outputSize = 128;
        private Interpreter model;

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                        .add(new NormalizeOp( 127.5f , 127.5f ) )
                        .build();

        NeuralModel(Context context)
        {
            try{
                model = new Interpreter(FileUtil.loadMappedFile(context,
                        "Facenet-optimized.tflite"));
            } catch (IOException e){
                Log.e("tfliteSupport", "Error reading model", e);
            }
        }

        public TensorImage imageLoad(Bitmap image)
        {
            TensorImage tImage = new TensorImage(DataType.UINT8);
            tImage.load(image);
            tImage = imageProcessor.process(tImage);
            return tImage;
        }

        public float[][] processImage(TensorImage tImage)
        {
            float[][] probabilityBuffer = new float[1][outputSize];
            model.run(tImage.getBuffer(), probabilityBuffer);
            return probabilityBuffer;
        }

    }
