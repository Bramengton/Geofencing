package com.test.gof;

import com.google.android.gms.maps.model.LatLng;

/**
 * @author Konstantin on 22.05.19.
 */
public class Haversine {
    private static final double EARTH_RADIUS = 6371; //meters

    public static int haversineInM(LatLng first, LatLng second) {
        return (int) (1000 * haversineInKm(first, second));
    }

    public static double haversineInKm(LatLng first, LatLng second) {
        double dLat = Math.toRadians(second.latitude-first.latitude);
        double dLng = Math.toRadians(second.longitude - first.longitude);
        double a = Math.sin(dLat/2)
                * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(first.latitude))
                * Math.cos(Math.toRadians(second.latitude))
                * Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * c;
    }
}
