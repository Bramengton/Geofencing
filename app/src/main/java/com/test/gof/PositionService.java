package com.test.gof;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;
import com.test.gof.util.ExtendService;

import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class PositionService extends ExtendService {
    private LocationManager mLocationManager;
    private IBinder binder = new LocalBinder();
    private ArrayList<Geofences> mGeofences = new ArrayList<>();
    private LatLng mCurrentPosition;

    private OnServiceListener mListener;
    public void removeListener(){
        mListener = null;
    }

    class LocalBinder extends Binder {
        PositionService getService(OnServiceListener listener){
            mListener = listener;
            return PositionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service task started onCreate");
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        startLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification(MapsActivity.class);
        return START_STICKY;
    }

    public void checkGeofences(){
        if(mListener!=null) mListener.onServiceInAction(mCurrentPosition, mGeofences);
    }

    public void setGeofences(final ArrayList<Geofences> val){
        mGeofences = new ArrayList<>(val);
    }

    public void setGeofences(final Geofences val){
        mGeofences.add(val);
    }

    public ArrayList<Geofences> getGeofences(){
        return mGeofences;
    }

    public void startLocation(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300, 50, getListener());
            Toasty.warning(getApplicationContext(), "Wait GPS is on update..", Toasty.LENGTH_LONG).show();
        }
    }

    private LocationListener getListener(){
        return new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.e("TEST", "onLocationChanged");

                if (location != null) {
                    mCurrentPosition = new LatLng(location.getLatitude(), location.getLongitude());

                    StringBuilder message = new StringBuilder();
                    for (Geofences geofences : mGeofences){
                        int meters = geofences.howFaraway(mCurrentPosition);
                        if(geofences.isInRadius(meters)){
                            message.append("We are outside: ").append(geofences.getName());
                        }else {
                            message.append("We are in: ").append(geofences.getName());
                        }
                        if(message.length()>0) message.append(String.format(" on %s m", meters)).append("\n");
                    }
                    if(message.length()>0)
                        Toasty.success(getApplicationContext(), message, Toasty.LENGTH_LONG).show();
                    if(mListener!=null) mListener.onChange(mCurrentPosition, mGeofences.isEmpty());
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }
}
