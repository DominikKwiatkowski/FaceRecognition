package com.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileNotFoundException;
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
}
