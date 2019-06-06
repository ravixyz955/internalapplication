package com.example.user.internalapplication.utils;

import android.content.Context;
import android.widget.Toast;

public class Utils {

    public static void showToast(Context context, String res) {
        Toast toast = Toast.makeText(context, res, Toast.LENGTH_SHORT);
        toast.show();
    }
}
