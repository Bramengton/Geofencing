package com.test.gof;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
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
import com.test.gof.util.ServiceConnectionManager;

import java.util.ArrayList;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener {

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOC_PERM_REQ_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCurrentLocationOnMap();
                    myService.startLocation();
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
    private PositionService myService;
    private ServiceConnectionManager mServiceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        MapsInitializer.initialize(this);
        PositionService.loadService(this, PositionService.class);
        mServiceManager = getService();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mServiceManager.bindToService();
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
        Geofences geofence = new Geofences(point);
        myService.setGeofences(geofence);
        geofence.setName(String.format("%s radius", myService.getGeofences().size()));
        updateMap(geofence.getCenter());
    }

    private void updateMap(final LatLng point){
        if(!myService.getGeofences().isEmpty()){
            mMap.clear();
            for(Geofences geofences : myService.getGeofences()){
                geofences.drawCircle(mMap);
            }
            mMap.addMarker(new MarkerOptions().position(point));
        }
    }

    @Override
    public void onCameraIdle() {
        LatLng position = mMap.getCameraPosition().target;
        StringBuilder message = new StringBuilder();
        for (Geofences geofences : myService.getGeofences()){
            int meters = geofences.howFaraway(position);
            if(geofences.isInRadius(meters)){
                message.append("We are outside: ").append(geofences.getName());
            }else {
                message.append("We are in: ").append(geofences.getName());
            }
            if(message.length()>0) message.append(String.format(" on %s m", meters)).append("\n");
        }
        ((TextView)findViewById(R.id.message)).setText(message);
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
                if(myService.getGeofences().size()>1){
                    mMap.clear();
                    Geofences val = myService.getGeofences().get(0);
                    ArrayList<Geofences> temp = new ArrayList<>();
                    temp.add(val);
                    myService.setGeofences(temp);
                    val.drawCircle(mMap);
                    mMap.addMarker(new MarkerOptions().position(val.getCenter()));
                    moveCameraToPosition(val.getCenter());
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected ServiceConnectionManager getService(){
        return new ServiceConnectionManager(this, PositionService.class) {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                super.onServiceConnected(componentName, iBinder);
                Log.e("TEST", "onServiceConnected");
                myService = ((PositionService.LocalBinder) iBinder).getService(getServiceListener());
                myService.checkGeofences();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                super.onServiceDisconnected(componentName);
                Log.e("TEST", "onServiceDisconnected");
            }
        };
    }


    private OnServiceListener getServiceListener(){
        return new OnServiceListener(){
            @Override
            public void onChange(LatLng point, boolean geofences) {
                if(geofences){
                    if (point != null) {
                        moveCameraToPosition(point);
                        addNewRadius(point);
                    }
                }
            }

            @Override
            public void onServiceInAction(LatLng point, ArrayList<Geofences> list) {
            if(list.isEmpty()) {
                if (point != null) {
                    moveCameraToPosition(point);
                    addNewRadius(point);
                }
            }else{
                LatLng pos = null;
                for(Geofences val : list){
                    val.drawCircle(mMap);
                    pos = val.getCenter();
                }
                if(pos!=null) {
                    mMap.addMarker(new MarkerOptions().position(point));
                    moveCameraToPosition(pos);
                }
            }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mServiceManager.bindToService();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(myService!=null) myService.removeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mServiceManager.unbindFromService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mServiceManager.unbindFromService();
    }
}
