package com.test.gof;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppComponentFactory;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, LocationListener {

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOC_PERM_REQ_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCurrentLocationOnMap();
                    Toast.makeText(getApplicationContext(),
                            "Location access permission granted, you try " +
                                    "add or remove location allerts",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    //meters
    private static final int GEOFENCE_RADIUS = 500;

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationManager mLocationManager;
    private GeofencingClient geofencingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.g_map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        geofencingClient = LocationServices.getGeofencingClient(this);
    }

    private void setupTestProvider() {
        mLocationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, false, //requiresNetwork,
                false, // requiresSatellite,
                false, // requiresCell,
                false, // hasMonetaryCost,
                false, // supportsAltitude,
                false, // supportsSpeed, s
                false, // upportsBearing,
                Criteria.POWER_LOW, // powerRequirement
                Criteria.ACCURACY_FINE); // accuracy
        mLocationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        mLocationManager.setTestProviderStatus(LocationManager.NETWORK_PROVIDER,
                LocationProvider.AVAILABLE,
                null,
                System.currentTimeMillis());
    }


    public boolean isMockLocationEnabled() {
        // Starting with API level >= 18 we can (partially) rely on .isFromMockProvider()
        // (http://developer.android.com/reference/android/location/Location.html#isFromMockProvider%28%29)
        // For API level < 18 we have to check the Settings.Secure flag
        return Build.VERSION.SDK_INT < 18 &&
                !android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings
                        .Secure.ALLOW_MOCK_LOCATION).equals("0");
    }

    public boolean isMockSettingsON() {
        // returns true if mock location enabled, false if not enabled.
        return !Settings.Secure.getString(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(false);//отключаем компас
        mMap.setMinZoomPreference(15);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {

                if(isMockSettingsON()) {
                    setupTestProvider();
                    Location mockLocation = new Location(LocationManager.NETWORK_PROVIDER);
                    mockLocation.setLatitude(point.latitude);
                    mockLocation.setLongitude(point.longitude);
                    mockLocation.setTime(System.currentTimeMillis());
                    mockLocation.setAccuracy(1);
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    }
                    mLocationManager.setTestProviderLocation(mockLocation.getProvider(), mockLocation);
                }else{
                    moveCameraToPosition(point);
                }
            }
        });
        showCurrentLocationOnMap();
    }

    @SuppressLint("MissingPermission")
    private void showCurrentLocationOnMap() {
        if (requestLocationAccessPermission() && mMap != null){
            mMap.setMyLocationEnabled(true);
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
                        moveCameraToPosition(myPosition);
                        addLocationAlert(myPosition);
                    }
                }
            });

            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 50, this);

        }
    }

    private void moveCameraToPosition(final LatLng latLng){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)     // Позиция на карты
                .zoom(15)               // Зумм карты
                .bearing(0)             // Направление на север в градусах
                .tilt(0)                // Градус на карту
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @SuppressLint("MissingPermission")
    private void addLocationAlert(final LatLng latLng){
        geofencingClient.addGeofences(getGeofencingRequest(latLng), getGeofencePendingIntent())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            CircleOptions circleOptions = new CircleOptions()
                                    .strokeColor(Color.RED) //Outer black border
                                    .fillColor(Color.TRANSPARENT) //inside of the geofence will be transparent, change to whatever color you prefer like 0x88ff0000 for mid-transparent red
                                    .center(latLng) // the LatLng Object of your geofence location
                                    .zIndex(20000)
                                    .strokeWidth(5)
                                    .radius(GEOFENCE_RADIUS); // The radius (in meters) of your geofence

                            mMap.addCircle(circleOptions);
                        }

                        Toast.makeText(getApplicationContext(), (task.isSuccessful()
                                        ? "Location alter has been added"
                                        : "Location alter could not be added"),
                                Toast.LENGTH_SHORT).
                                show();
                    }
                });
    }

    private void removeLocationAlert(){
        if (requestLocationAccessPermission()) {
            geofencingClient.removeGeofences(getGeofencePendingIntent())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) mMap.clear();
                            Toast.makeText(getApplicationContext(), (task.isSuccessful()
                                                ? "Location alters have been removed"
                                                : "Location alters could not be removed"),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        return PendingIntent.getService(this, 0,
                new Intent(this, LocationAlertIntentService.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private GeofencingRequest getGeofencingRequest(LatLng latLng) {
        Geofence geofence =  new Geofence.Builder()
                .setRequestId(String.format("key-%s-%s", latLng.latitude, latLng.longitude))
                .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(10000)
                .build();

        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.remove_loc_alert) {
            removeLocationAlert();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
            moveCameraToPosition(myPosition);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
