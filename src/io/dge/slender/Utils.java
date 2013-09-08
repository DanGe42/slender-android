package io.dge.slender;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void ltoast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void stoast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
