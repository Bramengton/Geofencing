package com.test.gof.util;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public abstract class ServiceConnectionManager extends ContextWrapper implements ServiceConnection {
    private final Class<? extends Service> service;
    private boolean attemptingToBind = true;
    private boolean bound = false;

    public ServiceConnectionManager(Context context, Class<? extends Service> service) {
        super(context);
        this.service = service;
    }

    public void bindToService() {
        if (attemptingToBind) {
            attemptingToBind = false;
            bindService(new Intent(this, service), this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        attemptingToBind = true;
        bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        bound = false;
    }

    public void unbindFromService() {
        attemptingToBind = true;
        if (bound) {
            unbindService(this);
            bound = false;
        }
    }

    public void ignoreBind(){
        attemptingToBind = false;
    }
}

