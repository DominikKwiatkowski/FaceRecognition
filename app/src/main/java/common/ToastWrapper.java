package common;

import android.content.Context;
import android.widget.Toast;

/**
 * Toast class wrapper for showing toast messages.
 * Get's rid of context parameter needed to be passed every time.
 */
public final class ToastWrapper {
    // App or activity context
    private Context context = null;

    /**
     * Class constructor.
     *
     * @param context app or activity context.
     */
    public ToastWrapper(Context context){
        this.context = context;
    }

    /**
     * Show toast.
     *
     * @param text Toast text.
     * @param duration duration of toast
     */
    public void showToast(String text, int duration){
        Toast.makeText(context, text, duration).show();
    }

}
