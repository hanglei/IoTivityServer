package com.kmk.iotivityserver;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.kmk.iotivityserver.resource.LedResource;

import org.iotivity.base.ModeType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final Activity mActivity = this;

    LedResource ledResource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        startServer();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopServer();
        super.onDestroy();
    }

    private void startServer() {
        PlatformConfig platformConfig = new PlatformConfig(mActivity, ServiceType.IN_PROC, ModeType.SERVER,
                "0.0.0.0", 5683, QualityOfService.LOW);
        OcPlatform.Configure(platformConfig);
        ledResource = new LedResource();
        try {
            ledResource.registerResource();
        } catch (OcException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Server is started~");
    }

    private void stopServer() {
        try {
            ledResource.unregisterResource();
        } catch (OcException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Server is stopped~");
    }
}