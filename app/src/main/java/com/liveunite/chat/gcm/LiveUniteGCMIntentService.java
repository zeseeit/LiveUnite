package com.liveunite.chat.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.R;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.infoContainer.Singleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ankit on 12/7/2016.
 */

public class LiveUniteGCMIntentService extends IntentService {

    private static final String TAG = LiveUniteGCMIntentService.class.getSimpleName();
    private int TIMEOUT_GCM_SEND = 60 * 1000;

    public LiveUniteGCMIntentService() {
        super(TAG);
    }

    public static final String KEY = "key";
    public static final String TOPIC = "topic";
    public static final String SUBSCRIBE = "subscribe";
    public static final String UNSUBSCRIBE = "unsubscribe";

    @Override
    protected void onHandleIntent(Intent intent) {
        registerGCM();
    }

    /**
     * Registering with GCM and obtaining the gcm registration id
     */
    private void registerGCM() {

        String token = null;

        try {
            InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            if (token.length() > 0) {
                LiveUnite.getInstance().getPreferenceManager().setGcmToken(token);
                updateGcmId();
            }

            Log.e(TAG, "GCM Registration Token: " + token);
            Intent registrationComplete = new Intent(Config.REGISTRATION_COMPLETE);
            registrationComplete.putExtra("token", token);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(registrationComplete);

        } catch (Exception e) {
            Log.e(TAG, "Failed to complete token refresh", e);
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
    }

    private void updateGcmId() {

        String url = Constants.SERVER.URL_SERVER_REGISTER_GCM;
        final String id = LiveUnite.getInstance().getPreferenceManager().getFbId();
        final String token = LiveUnite.getInstance().getPreferenceManager().getGcmToken();

        if(id.length()==0){
            Log.d(TAG,"user not registered with facebook");
            return;
        }

        StringRequest registerGcmDeviceReq = new StringRequest(Request.Method.POST, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject object = new JSONObject(response);
                            Log.d("LiveUniteGCMIntent", response);
                            if (!object.getBoolean("error")) {
                                LiveUnite.getInstance().getPreferenceManager().setDeviceRegistered(true);
                                ChatCentre.getInstance(getApplicationContext()).submitAction(ChatCentre.FLAG_SYNC_TOKEN);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG,"volley error " +error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                Log.e(TAG," user - "+id+" token - "+token);
                map.put("user_id", id);
                map.put("gcm_id", token);
                return map;
            }
        };

        registerGcmDeviceReq.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(registerGcmDeviceReq, "gcmRegister", getApplicationContext());

    }

}