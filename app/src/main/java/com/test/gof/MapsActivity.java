package com.test.gof;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener, LocationListener {

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

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationManager mLocationManager;
    private ArrayList<GeofenceVal> mGeofences = new ArrayList<>();
    private boolean mFusedLocationFlag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        MapsInitializer.initialize(this);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if(savedInstanceState!=null && savedInstanceState.containsKey("GEOFENCES"))
            mGeofences = savedInstanceState.getParcelableArrayList("GEOFENCES");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMinZoomPreference(15);
        googleMap.setOnCameraMoveListener(this);
        googleMap.setOnCameraIdleListener(this);
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                updateMap(point);
                moveCameraToPosition(point);
            }
        });
        mMap = googleMap;
        showCurrentLocationOnMap();
    }

    @SuppressLint("MissingPermission")
    private void showCurrentLocationOnMap() {
        if (requestLocationAccessPermission() && mMap != null){
            mMap.setMyLocationEnabled(true);
            if(mGeofences.isEmpty()) {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
                            moveCameraToPosition(position);
                            addNewRadius(position);
                            mFusedLocationFlag = false;
                        }
                    }
                });
            }else{
                LatLng point = null;
                for(GeofenceVal geofenceVal : mGeofences){
                    geofenceVal.drawCircle(mMap);
                    point = geofenceVal.getCenter();
                }
                if(point!=null) mMap.addMarker(new MarkerOptions().position(point));
            }
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

    private void addNewRadius(final LatLng point){
        GeofenceVal geofence = new GeofenceVal(point);
        mGeofences.add(geofence);
        geofence.setName(String.format("%s radius", mGeofences.size()));
        updateMap(geofence.getCenter());
    }

    private void updateMap(final LatLng point){
        if(!mGeofences.isEmpty()){
            mMap.clear();
            for(GeofenceVal geofenceVal : mGeofences){
                geofenceVal.drawCircle(mMap);
            }
            mMap.addMarker(new MarkerOptions().position(point));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && mFusedLocationFlag) {
            LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
            moveCameraToPosition(myPosition);
        }
        mFusedLocationFlag = true;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onCameraIdle() {
        LatLng position = mMap.getCameraPosition().target;
        StringBuilder message = new StringBuilder();
        for (GeofenceVal geofenceVal : mGeofences){
            int meters = geofenceVal.howFaraway(position);
            if(geofenceVal.isInRadius(meters)){
                message.append("We are outside: ").append(geofenceVal.getName());
            }else {
                message.append("We are in: ").append(geofenceVal.getName());
            }
            if(message.length()>0) message.append(String.format(" on %s m", meters)).append("\n");
        }
        Toasty.info(this, message, Toasty.LENGTH_LONG).show();
    }

    @Override
    public void onCameraMove() {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_add:
                addNewRadius(mMap.getCameraPosition().target);
                break;
            case R.id.menu_restore:
                if(mGeofences.size()>1){
                    mMap.clear();
                    GeofenceVal val = mGeofences.get(0);
                    mGeofences = new ArrayList<>();
                    mGeofences.add(val);
                    val.drawCircle(mMap);
                    mMap.addMarker(new MarkerOptions().position(val.getCenter()));
                    moveCameraToPosition(val.getCenter());
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("GEOFENCES", mGeofences);
    }
}
