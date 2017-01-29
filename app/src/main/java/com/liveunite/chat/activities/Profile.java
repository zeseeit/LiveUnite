package com.liveunite.chat.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.nfc.TagLostException;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.R;
import com.liveunite.activities.ViewProfile;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.FontManager;
import com.liveunite.chat.helper.VolleyUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {


    private ImageView profileImage;
    private TextView email, emailCaption;
    private TextView phone, phoneCaption;
    private TextView gender, genderCaption;
    private TextView bio, bioCaption;
    private TextView age, ageCaption;
    private TextView dob, dobCaption;
    private ProgressBar progressBar;
    private RelativeLayout wrapper;
    private Toolbar toolbar;
    CollapsingToolbarLayout toolbarLayout;
    private Typeface typeface;
    private String profileUserId;
    private String profileUserTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_compat);
        initViews();
        bindValues();

    }

    private void initViews() {

        typeface = FontManager.getInstance(this).getTypeFace();

        profileImage = (ImageView) findViewById(R.id.profileImage);
        toolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingBar);
        email = (TextView) findViewById(R.id.email);
        emailCaption = (TextView) findViewById(R.id.emailCaption);
        emailCaption.setTypeface(typeface);
        gender = (TextView) findViewById(R.id.gender);
        genderCaption = (TextView) findViewById(R.id.genderCaption);
        genderCaption.setTypeface(typeface);
        age = (TextView) findViewById(R.id.age);
        ageCaption = (TextView) findViewById(R.id.ageCaption);
        ageCaption.setTypeface(typeface);
        phone = (TextView) findViewById(R.id.phone);
        phoneCaption = (TextView) findViewById(R.id.phoneCaption);
        phoneCaption.setTypeface(typeface);
        bio = (TextView) findViewById(R.id.bio);
        bioCaption = (TextView) findViewById(R.id.bioCaption);
        bioCaption.setTypeface(typeface);
        dob = (TextView) findViewById(R.id.dob);
        dobCaption = (TextView) findViewById(R.id.dobCaption);
        dobCaption.setTypeface(typeface);

        progressBar = (ProgressBar) findViewById(R.id.progressBarProfile);
        progressBar.setIndeterminate(true);
        wrapper = (RelativeLayout) findViewById(R.id.infoWrapper);
        setSupportActionBar(toolbar);

    }

    private void bindValues() {

        Bundle bundle = getIntent().getExtras();
        profileUserId = bundle.getString("fbId");
        profileUserTitle = bundle.getString("title");

        int width = getScreenWidth();
        int height = (3 / 4) * width;
        String dpUrl = "https://graph.facebook.com/" + profileUserId + "/picture?width=" + width + "&height=" + height;
        Picasso.with(this).load(dpUrl).into(profileImage);
        toolbarLayout.setExpandedTitleColor(Color.WHITE);
        toolbarLayout.setTitle(profileUserTitle);
        toolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
        toolbarLayout.setContentScrimColor(getResources().getColor(R.color.colorPrimary));
        loadInfos(profileUserId);


        toolbarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent profileIntent = new Intent(Profile.this, ViewProfile.class);
                profileIntent.putExtra("fbId", profileUserId);
                startActivity(profileIntent);

            }
        });

    }

    private void loadInfos(final String fbId) {

        StringRequest profileInfoReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_PROFILE_INFO,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseProfileInfo(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Profile", "" + error);
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("fbId", fbId);
                return map;
            }
        };

        profileInfoReq.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(profileInfoReq, "syncGcmIds", this);

    }

    private void parseProfileInfo(String response) {

        /*
        * {
  "error": false,
  "hasInfo": true,
  "data": {
    "email": "ankit.kumar071460@gmail.com",
    "phone": "NA",
    "gender": "male",
    "dob": "04/10/1996",
    "age": "20",
    "bio": "Hey! I am enjoying LiveUnite"
  }
}
        *
        * */

        try {
            JSONObject object = new JSONObject(response);
            if (object.getBoolean("hasInfo")) {
                JSONObject data = object.getJSONObject("data");

                email.setText(data.getString("email"));
                phone.setText(data.getString("phone"));
                gender.setText(data.getString("gender"));
                dob.setText(data.getString("dob"));
                age.setText(data.getString("age"));
                bio.setText(data.getString("bio"));

                progressBar.setVisibility(View.GONE);
                wrapper.setVisibility(View.VISIBLE);

            }


        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private int getScreenWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

}
