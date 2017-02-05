package com.liveunite.chat.activities;

import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.R;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.helper.VolleyUtils;

import java.util.HashMap;
import java.util.Map;

public class ReportUserThemed extends AppCompatActivity {

    FrameLayout msg;
    FrameLayout photo;
    FrameLayout cloud;
    FrameLayout spam;
    FrameLayout other;
    String cmpId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.report_user_themed);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        cmpId = getIntent().getExtras().getString("cmpId");


        msg = (FrameLayout) findViewById(R.id.repo_inappropriate);
        photo = (FrameLayout) findViewById(R.id.repo_inappropriate_photo);
        spam = (FrameLayout) findViewById(R.id.spam);
        cloud = (FrameLayout) findViewById(R.id.bad_offline_behaviour);
        other = (FrameLayout) findViewById(R.id.other);

        final String selfId = LiveUnite.getInstance().getPreferenceManager().getFbId();

        msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reportUser(selfId,cmpId,"Inappropriate Message");
                showToast();
                finish();
            }
        });

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reportUser(selfId,cmpId,"Inappropriate Photo");
                showToast();
                finish();

            }
        });


        cloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reportUser(selfId,cmpId,"Offline Behaviour");
                showToast();
                finish();

            }
        });



        spam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reportUser(selfId,cmpId,"Spam");
                showToast();
                finish();
            }
        });



        other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reportUser(selfId,cmpId,"Other");
                showToast();
                finish();

            }
        });

    }

    private void showToast(){
        Toast.makeText(this,"User Reported ",Toast.LENGTH_LONG).show();
    }

    private void reportUser(final String userId,final String cmpId, final String reportType) {


        StringRequest syncGCMIdsReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_USER_REPORT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("ChatRoom:Report user",response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.d("Reporting user", "error " + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                map.put("userId", userId);
                map.put("cmpId",cmpId);
                map.put("report_type",reportType);
                return map;
            }
        };

        syncGCMIdsReq.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(syncGCMIdsReq, "syncLastSeen", this);

    }

}
