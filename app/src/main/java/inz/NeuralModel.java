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

    /**
     * Represent neural model. User can defined which one will be used
     */
    public class NeuralModel {
        private final int inputSize = 160;
        private final int outputSize = 128;
        private final String nameOfModel;
        private Interpreter model;

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                        .add(new NormalizeOp( 127.5f , 127.5f ) )
                        .build();

        /**
         * Class constructor.
         * @param context actual Activity
         * @param nameOfModel name of model to be used. All models must be inside ml folder.
         */
        NeuralModel(Context context, String nameOfModel)
        {
            this.nameOfModel = nameOfModel;
            try{
                model = new Interpreter(FileUtil.loadMappedFile(context,
                        this.nameOfModel));
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
         * @exception NullPointerException in case of null image
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
         * Recive target image and serialize it to image's vector using neural network model.
         *
         * @param tImage image to be processed by network. It must be preprocessed.
         * @return probabilityBuffer Buffer of face properties. Sized of buffer must be specified.
         * @exception NullPointerException in case of null image
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

        /*public void detectFaces(CvCameraViewFrame image)
        {
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        }*/
    }
