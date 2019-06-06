package com.example.user.internalapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.user.internalapplication.utils.NetworkUtils;
import com.example.user.internalapplication.utils.Utils;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerOptions;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.github.yavski.fabspeeddial.FabSpeedDial;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, LocationEngineListener {
    private static final int PERMISSION_ALL = 1;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    public MapboxMap map;
    private MapView mapView;
    private String[] permissions;
    private Timer timer;
    private boolean addMarker, initialLocation;
    private String FILE_NAME = "internalApp.txt";
    private FileWriter out;
    private File file;
    private double latitude, longitude;
    private Location location;
    private static final int GMAIL_REQ = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_map);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        file = new File(Environment.getExternalStorageDirectory(), FILE_NAME);
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
            mapView.getMapAsync(this);
        }

        FabSpeedDial fabAll = findViewById(R.id.fab_all);
        fabAll.setMenuListener(new FabSpeedDial.MenuListener() {
            @Override
            public boolean onPrepareMenu(NavigationMenu navigationMenu) {
                return true;
            }

            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                int id = menuItem.getItemId();
                switch (id) {
                    case R.id.start:
                        timer = new Timer();
                        addMarker = true;
                        try {
                            if (file != null && file.exists()) {
                                file.delete();
                                if (out != null) {
                                    out.close();
                                    out = null;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                saveDataToFile();
                            }
                        }, 0, 1000);
                        Utils.showToast(MapActivity.this, "Started");
                        break;
                    case R.id.stop:
                        addMarker = false;
                        if (timer != null) {
                            timer.cancel();
                            timer = null;
                            Utils.showToast(MapActivity.this, "Stopped");
                        }
                        break;
                    case R.id.mail:
                        sendEmail();
                        Utils.showToast(MapActivity.this, "email");
                        break;
                }
                return true;
            }

            @Override
            public void onMenuClosed() {

            }
        });
    }

    private void sendEmail() {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> matches = pm.queryIntentActivities(emailIntent, 0);
        ResolveInfo best = null;
        for (final ResolveInfo info : matches)
            if (info.activityInfo.packageName.endsWith(".gm") || info.activityInfo.name.toLowerCase().contains("gmail"))
                best = info;
        if (best != null)
            emailIntent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GMAIL_REQ && resultCode == RESULT_CANCELED) {
            List<Marker> markers = map.getMarkers();
            for (Marker marker : markers) {
                map.removeMarker(marker);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void saveDataToFile() {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            setCameraPosition(location);
/*
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.showToast(MapActivity.this, String.valueOf(latitude));
                }
            });
*/
        }
        if (latitude != 0.0 && longitude != 0.0) {
            writeStringAsFile(System.currentTimeMillis() + ", " + latitude + ", " + longitude
                    + ", " + location.getAltitude() + ", " + location.getAccuracy() + ", " + location.getBearing() + "\n");
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

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map = mapboxMap;
        enableLocationPlugin();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mapView.getMapAsync(this);
        NetworkUtils.isGpsEnabled(this);
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            locationPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
            locationPlugin.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.setFastestInterval(1000);
        locationEngine.activate();
        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
//            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (map != null) {
                    Drawable drawable = ContextCompat.getDrawable(MapActivity.this, R.drawable.icon);
                    Bitmap icon = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(icon);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16), 2000);
                    if (addMarker) {
                        map.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .icon(IconFactory.getInstance(MapActivity.this).fromBitmap(icon)));
                    }
                }
            }
        });
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            this.location = location;
            if (addMarker)
                setCameraPosition(location);
            if (!initialLocation) {
                initialLocation = true;
                setCameraPosition(location);
            }
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationEngineListener(this);
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        if (locationEngine != null) {
            locationEngine.addLocationEngineListener(this);
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onResume();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onPause() {
        super.onPause();
        if (locationEngine != null) {
            locationEngine.addLocationEngineListener(this);
            locationEngine.requestLocationUpdates();
        }
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        }
    }
}