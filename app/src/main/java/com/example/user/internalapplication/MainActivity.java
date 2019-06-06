package com.example.user.internalapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.internalapplication.utils.NetworkUtils;
import com.google.android.gms.location.LocationRequest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private Button start, pause;
    private static final String TAG = MainActivity.class.getSimpleName();
    private Timer timer;
    private LocationRequest locationRequest;
    private LocationManager locationManager;
    private double latitude, longitude;
    private Location location;
    private String[] permissions;
    private static final int PERMISSION_ALL = 1;
    private String FILE_NAME = "internalApp.txt";
    private FileWriter out;
    private LinearLayout latlngParentView;
    private ScrollView latlngScrollView;
    private boolean isPermissionGranted;
    private File file;
    private static final int GMAIL_REQ = 1001;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = findViewById(R.id.start_btn);
        pause = findViewById(R.id.pause_btn);
        latlngParentView = findViewById(R.id.latlngParentView);
        latlngScrollView = findViewById(R.id.latlngScrollView);
        file = new File(Environment.getExternalStorageDirectory(), FILE_NAME);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL);
        } else {
            isPermissionGranted = true;
        }

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (start.getText().toString().contains("Start")) {
                    timer = new Timer();
                    if (pause != null) {
                        if (!pause.isEnabled())
                            pause.setEnabled(true);
                        pause.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                    }
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    start.setText("Stop");
                                }
                            });
                            saveDataToFile();
                        }
                    }, 0, 1000);
                } else if (start.getText().toString().contains("Stop")) {
                    pause.setEnabled(false);
                    pause.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    Toast.makeText(MainActivity.this, "saved to file", Toast.LENGTH_LONG).show();
                    timer.cancel();
                }
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Paused", Toast.LENGTH_SHORT).show();
                start.setText("Start");
                timer.cancel();
            }
        });
    }

    private boolean hasPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        NetworkUtils.isGpsEnabled(this);
        isPermissionGranted = true;
        if (!NetworkUtils.isConnectingToInternet(this)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        } else {
            locationManager.removeUpdates(this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_mail) {

            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            final PackageManager pm = getPackageManager();
            final List<ResolveInfo> matches = pm.queryIntentActivities(emailIntent, 0);
            ResolveInfo best = null;
            for (final ResolveInfo info : matches)
                if (info.activityInfo.packageName.endsWith(".gm") || info.activityInfo.name.toLowerCase().contains("gmail"))
                    best = info;
            if (best != null)
                emailIntent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
//            emailIntent.setData(Uri.parse("mailto:"));
//            String[] to = {"ravikumar.badavath@gmail.com"};
//            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            File filelocation = new File(Environment.getExternalStorageDirectory(), FILE_NAME);
            Uri uri = Uri.fromFile(filelocation);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Edit text here!");
            if (!file.exists() || !file.canRead()) {
                Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
            } else {
                emailIntent.setType("plain/text");
                startActivityForResult(emailIntent, GMAIL_REQ);
            }
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    private void saveDataToFile() {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = new TextView(MainActivity.this);
                textView.setText("\t\t" + latitude + "\t\t\t" + longitude);
                latlngParentView.addView(textView);
            }
        });
        if (latitude != 0.0 && longitude != 0.0) {
            writeStringAsFile(System.currentTimeMillis() + ", " + latitude + ", " + longitude
                    + ", " + location.getAltitude() + ", " + location.getAccuracy() + ", " + location.getBearing() + "\n");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GMAIL_REQ && resultCode == RESULT_CANCELED) {
            try {
                if (file != null && file.exists()) {
                    file.delete();
                    if (out != null) {
                        out.close();
                        out = null;
                    }
                    start.setText("Start");
                    latlngParentView.removeAllViews();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void writeStringAsFile(final String fileContents) {
        try {
            if (out == null) {
                out = new FileWriter(file, true);
            }
            if (out != null) {
                out.write(fileContents);
                out.flush();
            }

        } catch (IOException e) {
            Log.e("WriteError", e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
            this.location = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();


        NetworkUtils.isGpsEnabled(this);
        if (isPermissionGranted) {
            if (!NetworkUtils.isConnectingToInternet(this)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            } else {
                locationManager.removeUpdates(this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
            }
        }

      /*  if (latlngParentView != null) {
            latlngParentView.removeAllViews();
            start.setText("Start");
        }
        if (file != null && file.exists()) {
            file.delete();/
        }
        if (out != null) {
            try {
                out.close();
                out = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (timer != null)
            timer.cancel();
        if (start != null)
            start.setText("Start");*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}