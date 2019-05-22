package com.test.gof;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.test.gof.util.ServiceConnectionManager;

abstract class BaseActivity extends AppCompatActivity {
    protected static final int LOC_PERM_REQ_CODE = 1;
    protected boolean requestLocationAccessPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOC_PERM_REQ_CODE);
            return false;
        }else return true;
    }



}
