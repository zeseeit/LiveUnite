package com.liveunite.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.R;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.gcm.Config;
import com.liveunite.chat.helper.HomeLaucherSetup;
import com.liveunite.chat.helper.Optimizer;
import com.liveunite.gps.GPSTracker;
import com.liveunite.infoContainer.OpenCameraType;
import com.liveunite.models.UserDetails;
import com.liveunite.network.Register;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.opencamera.CameraActivity;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.CheckInternetConnection;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ExtraMethods;
import com.liveunite.utils.UpdateLocations;
import com.facebook.FacebookSdk;

import static java.lang.Thread.sleep;

public class Splash extends AppCompatActivity {

    private static final int REQUEST_LOCATION = 123;
    Context context;
    int result;
    int myVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_splash);
        context = Splash.this;

        Singleton.getInstance().setScrrenWidth(getScreenWidth());
        myVersion = Build.VERSION.SDK_INT;
        Log.d("Splash", " onCreate init..");
        new Optimizer().logTime("Splash: onCreate() > fbInit");
        init();
        new Optimizer().logTime("Splash: onCreate() > fbInit done ");

    }

    private void checkPreRequis(){

        CheckInternetConnection checkInternetConnection = new CheckInternetConnection(this);
        if(!checkInternetConnection.isConnectedToInternet()){
            checkInternetConnection.showDialog();
            return;
        }

        final UpdateLocations locations = new UpdateLocations(this);
        locations.initiateLocationFetch();
        if(!locations.getLocationStatus()){
            new GPSTracker(this).showSettingsAlert();
            return;
        }


    }

    private int getScreenWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    private void init() {
        FacebookSdk.sdkInitialize(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPreRequis();

        if(LiveUnite.getInstance().getPreferenceManager().getFbId().length()>0) {

            Log.d("AppOptimization"," Starting Task on Worker Thread...................");

            new Thread() {
                @Override
                public void run() {
                    startTask();
                }
            }.start();

            OpenCameraType.getInstance().setOpenPostCamera(true);
            new ChangeActivity().change(context, HomeActivity.class);
            new ChangeActivity().change(context, CameraActivity.class);

        }else{

            Log.d("AppOptimization"," Starting Task on Main Thread");
            startTask();
        }

    }

    private void startTask(){

        if (myVersion <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d("Splash", " onResume() procceding..");
            procced();
        } else {
            if (isLocationPermissionAllowed()) {
                procced();
            } else {
                requestPermission();
            }
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Allow location permission to get your location", Toast.LENGTH_SHORT).show();
                }
            });

        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ActivityCompat.requestPermissions(Splash.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            }
        });

    }

    private boolean isLocationPermissionAllowed() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                procced();
            } else {
                // requestPermission();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void procced() {


        new Optimizer().logTime("Splash:  > proceed()>checkNetwork");

        if (new CheckInternetConnection(context).isConnectedToInternet()) {

            new Optimizer().logTime("Splash:  > proceed() < checkNetwork ");
            new Optimizer().logTime("Splash:  > proceed() > ExtraMethodCheck:login() ");

            if (LiveUnite.getInstance().getPreferenceManager().getFbId().length()>0) {

                new Optimizer().logTime("Splash:  > proceed() < ExtraMethodCheck:login() ");
                // check if GPS enabled
                new Optimizer().logTime("Splash:  > proceed() > fetchLocations() ");
                Log.d("Splash", "LoggedIn");

                final UpdateLocations locations = new UpdateLocations(this);
                locations.initiateLocationFetch();

                if (locations.getLocationStatus()) {
                    Log.d("Splash", "Getting details from fb");

                    new Optimizer().logTime("Splash:  > proceed() > getFbDetails() ");
                    new Register().getDetailsFromFB(context, false);
                    new Optimizer().logTime("Splash:  > proceed() < getFbDetails() ");

                }
            } else {

                new Optimizer().logTime("Splash:  > proceed() > sleep");
                new ChangeActivity().change(context, LoginActivity.class);
                finish();
            }

        } else {
            new CheckInternetConnection(context).showDialog();
        }

    }
}