package com.test.gof;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class LocationAlertIntentService extends IntentService {
    private static final String IDENTIFIER = "LocationAlertIS";

    public LocationAlertIntentService() {
        super(IDENTIFIER);
    }

    private String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE: return "Geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES: return "geofence too many_geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS: return "geofence too many pending_intents";
            default: return "geofence error";
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(IDENTIFIER, "" + getErrorString(geofencingEvent.getErrorCode()));
            return;
        }

        int transition = geofencingEvent.getGeofenceTransition();
        if (transition == Geofence.GEOFENCE_TRANSITION_DWELL ||
                transition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            List<Geofence> trigger = geofencingEvent.getTriggeringGeofences();
            String details = getTransitionInfo(trigger);
            String type = getTransitionString(transition);
            notifyLocationAlert(type, details);
        }
    }

    private String getTransitionInfo(List<Geofence> triggeringGeofences) {
        ArrayList<String> locationNames = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            locationNames.add(geofence.getRequestId());
        }
        return TextUtils.join(", ", locationNames);
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER: return "location entered";
            case Geofence.GEOFENCE_TRANSITION_EXIT: return "location exited";
            default: return "location transition";
        }
    }

    private void notifyLocationAlert(String locTransitionType, String locationDetails) {

        String CHANNEL_ID = "Zoftino";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_mapmode)
                        .setColor(Color.RED)
                        .setAutoCancel(true)
                        .setContentTitle(locTransitionType)
                        .setContentText(locationDetails);

        builder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, builder.build());
    }

}
