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
        private final int inputSize = 160;
        private final int outputSize = 128;
        private final String nameOfModel =  "Facenet-optimized.tflite";
        private Interpreter model;

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                        .add(new NormalizeOp( 127.5f , 127.5f ) )
                        .build();

        /**
         * Class constructor.
         */
        NeuralModel(Context context)
        {
            try{
                model = new Interpreter(FileUtil.loadMappedFile(context,
                        nameOfModel));
            } catch (IOException e){
                Log.e("tfliteSupport", "Error reading model", e);
            }
        }

        /**
         * Prepare image to be putted inside neural network. It will change resolution and format of
         * given image to resolution needed by neural network.
         *
         * @param image image to be prepared.
         * @return tImage image processed and ready to be putted by neural network.
         */
        public TensorImage prepareImage(Bitmap image)
        {
            if(image == null)
            {
                throw new NullPointerException("image can't be null");
            }
            TensorImage tImage = new TensorImage(DataType.UINT8);
            tImage.load(image);
            tImage = imageProcessor.process(tImage);
            return tImage;
        }

        /**
         * Processing image in neural network.
         *
         * @param tImage image to be processed by network. It must be preprocessed.
         * @return probabilityBuffer Buffer of face properties. Sized of buffer must be specified.
         */
        public float[][] processImage(TensorImage tImage)
        {
            if(tImage == null)
            {
                throw new NullPointerException("image can't be null");
            }
            float[][] probabilityBuffer = new float[1][outputSize];
            model.run(tImage.getBuffer(), probabilityBuffer);
            return probabilityBuffer;
        }
    }
