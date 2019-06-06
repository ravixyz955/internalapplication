package com.example.user.internalapplication.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

public class NetworkUtils {
    private static boolean gps_enabled;
    private static LocationManager lm;

    public static boolean isConnectingToInternet(Context _context) {
        ConnectivityManager connectivity = (ConnectivityManager) _context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo anInfo : info)
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    public static void isGpsEnabled(Context mContext) {
        lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gps_enabled) {
            gps_location_enable_dialog(mContext);
        }
    }

    private static void gps_location_enable_dialog(final Context mContext) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle("Enable Location");
        alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
        alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mContext.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.show();
    }
}
