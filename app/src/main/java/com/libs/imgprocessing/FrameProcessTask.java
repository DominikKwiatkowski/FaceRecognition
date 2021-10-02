package com.libs.imgprocessing;

import android.content.Context;

import com.libs.globaldata.ModelType;
import com.libs.facerecognition.NeuralModel;
import com.libs.globaldata.GlobalData;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.tensorflow.lite.support.image.TensorImage;

import java.util.ArrayList;

public class FrameProcessTask implements Runnable {
    private final NeuralModel model;
    private MatOfRect faceMat = null;
    private String[] names = null;
    private Mat lastFrame = null;
    private boolean stop = false;

    public FrameProcessTask(Context context) {
        // Neural model load.
        // TODO: change after model choice functionality will be added
        model = GlobalData.getModel(ModelType.FACENET_USER, context).neuralModel;
    }

    /**
     * Get newest frame and proceed it. It will end only if user call setStop(true).
     */
    @Override
    public void run() {
        // On beginning change stop value to false
        setStop(false);

        while (true) {
            // Check if thread should stop.
            if (stop)
                return;

            // Get frame. Set frame to null to avoid doing same operation twice.
            Mat inputFrame = getFrame();
            // setFrame(null);

            if (inputFrame != null) {
                MatOfRect faces = model.detectAllFaces(inputFrame);
                // TODO: Some way to track face, make some noise detection,
                // TODO: maybe some number of frames with similar object?

                // TODO: we have to proceed this input and put some number to face, and apply
                // TODO: face detection for frame operation.

                //Pre process all images
                ArrayList<Mat> faceImages = model.preProcessAllFaces(inputFrame, faces);

                // Calculate vector for each face
                for (Mat face : faceImages) {
                    TensorImage image = model.changeImageRes(face);
                    model.processImage(image);
                }

                // Set result synchronized.
                setFaces(faces);
            } else {
                // In case of no ready frame, wait some time
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Thread safe function to get last frame.
     *
     * @return last frame of video
     */
    public synchronized Mat getFrame() {
        return lastFrame;
    }

    /**
     * Thread safe function to set last frame.
     *
     * @param lastFrame last frame recieved from video
     */
    public synchronized void setFrame(Mat lastFrame) {
        this.lastFrame = lastFrame;
    }

    /**
     * Thread safe function to get detected faces.
     *
     * @return Array of Rect, which determines face position
     */
    public synchronized MatOfRect getFaces() {
        return faceMat;
    }

    /**
     * Thread safe function to set all detected faces.
     *
     * @param newFaces Array of Rect, which determines face position
     */
    public synchronized void setFaces(MatOfRect newFaces) {
        faceMat = newFaces;
    }

    /**
     * Thread safe function to get all names of detected faces.
     *
     * @return names of people on image
     */
    public synchronized String[] getNames() {
        return names;
    }

    /**
     * Thread safe function to set all names of detected faces.
     *
     * @param names names of people on image
     */
    public synchronized void setNames(String[] names) {
        this.names = names;
    }

    /**
     * Set if thread should stop.
     *
     * @param value false if thread should run, false otherwise
     */
    public synchronized void setStop(boolean value) {
        stop = value;
    }
}
