package com.liveunite.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.activities.Splash;
import com.liveunite.models.UserLocationModal;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.gps.GPSTracker;

/**
 * Created by Vishwesh on 10-11-2016.
 */

public class UpdateLocations {
    private GPSTracker gps;
    Context context;
    private boolean locationError = true;

    public UpdateLocations(Context context)
    {
        this.context = context;
        gps = new GPSTracker(context);

    }

    public void initiateLocationFetch(){

        if (checkGpsEnabled())
        {
            // Toast.makeText(context, gps.getLatitude() + " " + gps.getLongitude(), Toast.LENGTH_LONG).show();
            if (gps.getLongitude() == 0.0 || gps.getLongitude() ==0.0)
            {
                Log.d("LocationFetch","No location...");
                //Toast.makeText(context,"Make sure your location is on",Toast.LENGTH_LONG).show();
                locationError = true;
                return;

            }

            UserLocationModal userLocationModal = new UserLocationModal();
            userLocationModal.setLatitude(gps.getLatitude());
            userLocationModal.setLongitude(gps.getLongitude());

            // LiveUnite.getInstance().getPreferenceManager().setLat(userLocationModal.getLatitude()+"");
           // LiveUnite.getInstance().getPreferenceManager().setLong(userLocationModal.getLongitude()+"");
            Singleton.getInstance().setUserLocationModal(userLocationModal);

            locationError = false;
            return;

        } else {

            new TextView(LiveUnite.getInstance().getApplicationContext()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d("LocationFetch","showing Dialog");
                    gps.showSettingsAlert();
                }
            });

        }

        locationError = true;

    }

    public boolean getLocationStatus(){
        return !locationError;
    }

    public boolean checkGpsEnabled()
    {
        return (gps.canGetLocation());
    }
}
