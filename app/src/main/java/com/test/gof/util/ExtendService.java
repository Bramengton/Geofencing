package com.test.gof.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;

public abstract class ExtendService extends Service{
    protected static String TAG = "PositionService";

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
}
