package com.test.gof.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.test.gof.R;

public abstract class ExtendService extends Service{
    protected static String TAG = "PositionService";
    private static final @DrawableRes int APP_ICON = android.R.drawable.ic_dialog_map;

    public static void loadService(Context context, Class service) {
        try {
            Intent intent = new Intent(context, service);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            }else {
                context.startService(intent);
            }
        } catch (Throwable e) {
            e.fillInStackTrace();
        }
    }


    protected void createNotification(Class openClass) {
        Intent intent = new Intent(getApplicationContext(), openClass)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        String channelId = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("driver_service", "Taxi Service");
        }

        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(APP_ICON)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service ONLINE")
                .setContentIntent(pIntent)
                .setAutoCancel(false)
                .setOngoing(true);

        Notification notification = builder.build();
        startForeground(1337, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager notify = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notify!=null) notify.createNotificationChannel(chan);
        return channelId;
    }
}
