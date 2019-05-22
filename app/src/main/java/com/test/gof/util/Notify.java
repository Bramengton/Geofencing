package com.test.gof.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.test.gof.R;

public class Notify extends ContextWrapper {
    private static final int REQUEST_CODE = 1;
    private static final int NOTIFICATION_ID = 6578;

    private int mNotification_Id = NOTIFICATION_ID;

    private static final @DrawableRes int APP_ICON = android.R.drawable.ic_dialog_map;

    private NotificationManager mManager;
    private NotificationCompat.Builder mBuilder;

    private Service mService;

    public Notify(Context base) {
        super(base);
        this.mManager = (NotificationManager) base.getSystemService(NOTIFICATION_SERVICE);

        String CHANNEL_ID = getApplicationInfo().packageName;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel
                    (CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if(this.mManager!=null) this.mManager.createNotificationChannel(channel);
        }

        this.mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(APP_ICON)
                .setAutoCancel(false);
    }

    public Notify(Context base, @Nullable Service service) {
        this(base);
        this.mBuilder.setOngoing(true);
        this.mService = service;
    }

    public Notify addContent(String title, String message){
        this.mBuilder.setContentTitle(title).setContentText(message);
        return this;
    }

    public Notify addContent(@StringRes int title, @StringRes int message){
        this.mBuilder.setContentTitle(getString(title)).setContentText(getString(message));
        return this;
    }

    public Notify addAction(NotificationCompat.Action action){
        this.mBuilder.addAction(action);
        return this;
    }

    public Notify addNotifyId(int id){
        this.mNotification_Id = id;
        return this;
    }

    private Notification constructor(Intent intent){
        if(intent!=null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(),
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            this.mBuilder.setContentIntent(pendingIntent);
        }
        return this.mBuilder.build();
    }

    public void show() {
        this.show(null);
    }

    public void show(Intent intent) {
        if(this.mService!=null){
            this.mService.startForeground(this.mNotification_Id, constructor(intent));
        }else update(intent);
    }

    public void update() {
        this.update(null);
    }

    public void update(Intent intent){
        if(this.mManager != null){
            Notification notification = constructor(intent);
            notification.defaults |= Notification.DEFAULT_VIBRATE;  //TODO: FIX THIS  - METHOD IS DEPRECATED
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            this.mManager.notify(this.mNotification_Id, notification);
        }
    }

    public void cancel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this.mService!=null){
            this.mService.stopForeground(true);
        }else if(mManager != null) mManager.cancel(this.mNotification_Id);
    }
}
