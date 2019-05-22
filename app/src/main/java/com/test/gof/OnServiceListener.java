package com.test.gof;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public interface OnServiceListener {
    void onChange(LatLng point, boolean geofences);
    void onServiceInAction(LatLng point, ArrayList<Geofences> list);
}
