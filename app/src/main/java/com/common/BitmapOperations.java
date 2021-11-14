package com.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapOperations {
    /**
     * Resolve photo uri to bitmap
     *
     * @param photo - uri to image.
     * @return Bitmap of resolved image.
     */
    public static Bitmap resolveContentToBitmap(Uri photo, Context context) {
        InputStream stream = null;
        try {
            // Open file in stream
            stream = context.getContentResolver().openInputStream(photo);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Decode photo to Bitmap
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
    }

    /**
     * Load image from disk. Due to orientations issue we have to rotate it first.
     * @param uri uri to image to be loaded
     * @param context current app context
     * @return
     */
    public static Bitmap loadBitmapFromUri(Uri uri, Context context)
    {
        Bitmap bitmap = resolveContentToBitmap(uri, context);
        // Due to image rotating, we have to fix it before use.
        Bitmap correctImage = null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            ExifInterface exif = new ExifInterface(inputStream);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = BitmapOperations.exifToDegrees(rotation);

            Matrix matrix = new Matrix();
            if (rotation != 0f) {matrix.preRotate(rotationInDegrees);}

            correctImage = Bitmap.createBitmap(bitmap,0,0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }catch(IOException e){
            Log.e("IMAGE_LOAD", "Failed to load image", e);
        }
        return correctImage;
    }

    /**
     * Gets the Amount of Degress of rotation using the exif integer to determine how much
     * we should rotate the image.
     * @param exifOrientation - the Exif data for Image Orientation
     * @return - how much to rotate in degrees
     */
    public static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }
}
