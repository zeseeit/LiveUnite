package com.liveunite.LiveUniteMains;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.gcm.LiveUniteGCMIntentService;
import com.liveunite.chat.gcm.LiveUnitePreferenceManager;
import com.liveunite.chat.helper.HomeLaucherSetup;
import com.liveunite.chat.helper.Optimizer;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.chat.model.LiveUniteGcmRegistration;
import com.liveunite.chat.service.AppForegroundCheckService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Ankit on 12/7/2016.
 */

public class LiveUnite extends Application {

    public static final String TAG = LiveUnite.class.getSimpleName();
    private static LiveUnite mInstance;
    private LiveUnitePreferenceManager preferenceManager;

    @Override
    public void onCreate() {
        super.onCreate();

        new Optimizer().logTime("LiveUnite: onCreate()");

        mInstance = this;

        registerGCM();

        startLiveReporterService();

        new Optimizer().logTime("LiveUnite: onCreate() > ");

    }

    private void startLiveReporterService() {

        new Optimizer().logTime("LiveUnite: > startLiveReporterService() ");

        Intent i = new Intent(this, AppForegroundCheckService.class);
        startService(i);

        new Optimizer().logTime("LiveUnite: startLiveReporterService() >");


    }

    public static synchronized LiveUnite getInstance(){
        return mInstance;
    }

    public LiveUnitePreferenceManager getPreferenceManager(){
        if(preferenceManager==null){
            preferenceManager = new LiveUnitePreferenceManager(mInstance);
            if(!preferenceManager.homeIconCreated()){
                new HomeLaucherSetup().setHome(this);

            }
        }
        return preferenceManager;
    }

    public void registerGCM() {


        new Optimizer().logTime("LiveUnite: > registerGCM() ");

        if (checkPlayServices()) {
            Intent intent = new Intent(LiveUnite.getInstance().getApplicationContext(), LiveUniteGCMIntentService.class);
            intent.putExtra("key", "register");
            LiveUnite.getInstance().getApplicationContext().startService(intent);
        }


        new Optimizer().logTime("LiveUnite: registerGCM() >");


    }

    private boolean checkPlayServices() {


        new Optimizer().logTime("LiveUnite: > checkPlayServices() ");


        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(LiveUnite.getInstance().getApplicationContext());
        if (resultCode != ConnectionResult.SUCCESS) {

            Log.d("Singleton", "This device is not supported. Google Play Services not installed!");

            new Optimizer().logTime("LiveUnite: checkPlayServices() >");

            return false;
        }

        new Optimizer().logTime("LiveUnite: checkPlayServices() >");

        return true;

    }


}
