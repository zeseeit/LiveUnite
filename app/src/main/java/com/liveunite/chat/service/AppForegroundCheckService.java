package com.liveunite.chat.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.helper.ForegroundCheckTask;
import com.liveunite.chat.helper.LiveUniteTime;
import com.liveunite.chat.helper.VolleyUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ankit on 12/24/2016.
 */

public class AppForegroundCheckService extends Service {

    private static AppForegroundCheckService mInstance;

    public AppForegroundCheckService() {

    }

    public static AppForegroundCheckService getInstance() {
        if (mInstance == null) {
            mInstance = new AppForegroundCheckService();
        }
        return mInstance;
    }

    private Timer mTimer;
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {

            boolean isForeground = new ForegroundCheckTask().isForeground(getApplicationContext());

            String notifiedType = LiveUnite.getInstance().getPreferenceManager().getNotifiedType();
            Log.d("AppForegroundCheck", " isForeground " + isForeground);
            if (isForeground) {

                if (notifiedType.equals(Constants.USER.KEY_REPORT_TYPE_LAST_SEEN)) {
                    //send online status
                    Log.d("AppForegroundCheck", "reporting Online");
                    reportOnline();
                }

            } else {
                //send time and finish the task
                if (notifiedType.equals(Constants.USER.KEY_REPORT_TYPE_ONLINE)) {
                    Log.d("AppForegroundCheck", "reporting Last Seen");
                    reportLastSeen();

                }

            }

        }
    };
    private int SYNC_LAST_SEEN_TIMEOUT_MS = 1 * 60 * 1000;

    private void reportLastSeen() {

        StringRequest reportLastSeenReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_UPDATE_LAST_SEEN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        LiveUnite.getInstance().getPreferenceManager().setNotifiedType(Constants.USER.KEY_REPORT_TYPE_LAST_SEEN);
                        Log.d("AppForegroundCheck", "Last Seen Rep:res-" + response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("AppForegroundCheck", "Last Seen" + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                final String cmpId = LiveUnite.getInstance().getPreferenceManager().getFbId();
                final String lastSeenTime = LiveUniteTime.getInstance().getDateTime();

                HashMap<String, String> map = new HashMap<>();
                map.put("user_id", cmpId);
                map.put("status", lastSeenTime);

                return map;
            }
        };

        reportLastSeenReq.setRetryPolicy(new DefaultRetryPolicy(
                SYNC_LAST_SEEN_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(reportLastSeenReq, "onlineReporterTask", this);

    }

    private void reportOnline() {

        StringRequest reportOnlineReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_UPDATE_LAST_SEEN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        LiveUnite.getInstance().getPreferenceManager().setNotifiedType(Constants.USER.KEY_REPORT_TYPE_ONLINE);
                        Log.d("AppForegroundService", "On Rep:- " + response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("AppForegroundService", "online:" + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                final String cmpId = LiveUnite.getInstance().getPreferenceManager().getFbId();

                HashMap<String, String> map = new HashMap<>();
                map.put("user_id", cmpId);
                map.put("status", "Online");
                return map;
            }
        };

        reportOnlineReq.setRetryPolicy(new DefaultRetryPolicy(
                SYNC_LAST_SEEN_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(reportOnlineReq, "lastSeenReporterTask", this);

    }


    @Override
    public void onCreate() {
        super.onCreate();

        if (mTimer == null) {
            Log.d("AppForeGroundService","starting Reporter....");
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(timerTask, 0, 4000);
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
