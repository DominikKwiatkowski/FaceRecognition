package common;

import android.content.Context;
import android.widget.Toast;

public final class ToastWrapper {
    // App or activity context
    private Context context = null;

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
