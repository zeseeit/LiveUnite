package com.liveunite.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
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
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AppSetting extends AppCompatActivity implements View.OnClickListener {

    Button bLogOut,bShare,bTerms;
    Button deleteAccount;
    Context context;
    ProgressDialog progressDialog;
    SwitchCompat soundSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_setting);
        FacebookSdk.sdkInitialize(getApplicationContext());
        context = AppSetting.this;
        init();
        setUpToolbar();
    }

    private void init() {
        bLogOut = (Button)findViewById(R.id.bLogOut);
        bLogOut.setOnClickListener(this);
        bShare = (Button)findViewById(R.id.bShare);
        bShare.setOnClickListener(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting Account...");
        deleteAccount = (Button) findViewById(R.id.deleteAccount);

        deleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               confirm();
            }
        });

        bTerms = (Button)findViewById(R.id.bTerms);
        bTerms.setOnClickListener(this);
        soundSwitch = (SwitchCompat) findViewById(R.id.scSound);

        if(LiveUnite.getInstance().getPreferenceManager().getNotificationSoundChoice()){
            soundSwitch.setChecked(true);
        }else{
            soundSwitch.setChecked(false);
        }

        soundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean byUser) {

                    if(compoundButton.isChecked()){
                        LiveUnite.getInstance().getPreferenceManager().turnNotificationSound(true);
                    }else{
                        LiveUnite.getInstance().getPreferenceManager().turnNotificationSound(false);
                    }

            }
        });
        buttonEffect(bShare);
    }

    private void deleteMyAccount() {

        // request delete and logout
        StringRequest deleteReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_DELETE_ACCOUNT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("Delete Res",response);
                        handleDeleteResponse(response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ChatRoomLastSeenSync", "syncChat " + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                String myId = LiveUnite.getInstance().getPreferenceManager().getFbId();
                HashMap<String, String> map = new HashMap<>();
                map.put("fbId", myId);
                return map;
            }
        };

        deleteReq.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(deleteReq, "deleteAccountReq", this);
    }

    private void handleDeleteResponse(String response) {

        try {
            JSONObject object = new JSONObject(response);

            boolean success = object.getBoolean("error");
            progressDialog.dismiss();
            if(!

                    success){

                Toast.makeText(AppSetting.this,"Successfully Deleted Your LiveUnite Acccount",Toast.LENGTH_LONG).show();
                logout();
            }else{
                Toast.makeText(AppSetting.this,"Having Problem Deleting Your Acccount",Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(progressDialog.isShowing()){
            progressDialog.dismiss();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id)
        {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("App Setting");
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.bLogOut:
               logout();
                break;
            case R.id.bShare:
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "LiveUnite app");
                try {
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, Constant.SHARING_TEXT_START +
                            getPackageManager().getPackageInfo(getPackageName(),0).packageName + Constant.SHARING_TEXT_END);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
                break;
            case R.id.bTerms:
                Intent intent = new Intent(context,TermsOfUse.class);
                startActivity(intent);
        }
    }

    private void logout() {

        LoginManager.getInstance().logOut();
        new ChangeActivity().change(context,LoginActivity.class);
        LiveUnite.getInstance().getPreferenceManager().clear();
        HomeActivity.getInstance().finish();
        finish();
    }

    private void confirm(){

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Delete Account");
        alertDialogBuilder
                .setMessage("Do You Want To Delete Your Account")
                .setCancelable(false)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        progressDialog.show();
                       deleteMyAccount();
                    }
                });

        new TextView(LiveUnite.getInstance().getApplicationContext()).post(new Runnable() {
            @Override
            public void run() {

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

    }

    public static void buttonEffect(View button){
        button.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.getBackground().setColorFilter(0xe0f47521, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        v.getBackground().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }
}
