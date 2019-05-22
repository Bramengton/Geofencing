package com.test.gof;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

/**
 * @author Konstantin on 22.05.19.
 */
public class GeofenceVal implements Parcelable {
    private static final int GEOFENCE_RADIUS = 500; // in meters
    private LatLng mLatLng;
    private double mRadius;
    private String mName;
    private int mDistance;

    public GeofenceVal(LatLng latLng, double radius){
        this.mLatLng = latLng;
        this.mRadius = radius;
    }

    public GeofenceVal(LatLng latLng){
        this(latLng, GEOFENCE_RADIUS);
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return this.mName;
    }

    public double getRadius() {
        return this.mRadius;
    }

    public LatLng getCenter() {
        return this.mLatLng;
    }

    public boolean isInRadius(LatLng position){
        return (howFaraway(position) - getRadius() > 0);
    }

    public boolean isInRadius(int distance){
        return (distance - getRadius() > 0);
    }

    public int howFaraway(LatLng position){
        return Haversine.haversineInM(getCenter(), position);
    }

    public void drawCircle(GoogleMap map){
        if(map!=null) {
            map.addCircle(new CircleOptions()
                    .strokeColor(Color.RED) //Outer black border
                    .fillColor(Color.TRANSPARENT) //inside of the geofence will be transparent, change to whatever color you prefer like 0x88ff0000 for mid-transparent red
                    .center(this.mLatLng) // the LatLng Object of your geofence location
                    .zIndex(20000)  // draw over tiles
                    .strokeWidth(5) // line thickness
                    .radius(this.mRadius)); // The radius (in meters) of your geofence
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mLatLng, flags);
        dest.writeDouble(this.mRadius);
        dest.writeString(this.mName);
    }

    protected GeofenceVal(Parcel in) {
        this.mLatLng = in.readParcelable(LatLng.class.getClassLoader());
        this.mRadius = in.readDouble();
        this.mName = in.readString();
    }

    public static final Creator<GeofenceVal> CREATOR = new Creator<GeofenceVal>() {
        @Override
        public GeofenceVal createFromParcel(Parcel source) {
            return new GeofenceVal(source);
        }

        @Override
        public GeofenceVal[] newArray(int size) {
            return new GeofenceVal[size];
        }
    };
}
