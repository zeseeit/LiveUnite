package com.liveunite.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActivityChooserView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.JsonArray;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.adapter.DrawerListAdapter;
import com.liveunite.chat.activities.ChatWall;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.FontManager;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.gcm.LiveUnitePreferenceManager;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.chat.service.AppForegroundCheckService;
import com.liveunite.fragments.MomentsFragment;
import com.liveunite.infoContainer.OpenCameraType;
import com.liveunite.models.NavItem;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.ExtraMethods;
import com.liveunite.opencamera.CameraActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, EditProfile.onDPchange {

    public static Toolbar mToolbar;
    DrawerLayout mDrawerLayout;
    NavigationView navigationView;
    TextView tvCamera, tvNavigation,tvChat,tvMoments;
    Context context;
    public static HomeActivity homeActivity;


    ListView mDrawerList;
    RelativeLayout navigationTop;

    ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();
    private TextView tvNameHeader;
    private TextView tvViewProfileHeader;
    private ImageView ivDPHeader;
    private TextView tvUnReadCounter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.d("Optimization"," HomeActivity onCreate()");
        setContentView(R.layout.activity_home);
        context = HomeActivity.this;
        homeActivity = this;
        setUpToolbar();
        setArrayForNaviGation();
        initView();
        setNavigation();
        syncReportedItems();
    }

    private void syncReportedItems() {
        StringRequest syncReportedReq = new StringRequest(Request.Method.POST, Constants.SERVER.SYNC_REPORTED_PIDS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //todo: write back the reported ids
                handleSyncResponse(response);
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> mp = new HashMap<>();
                mp.put("fbId",LiveUnite.getInstance().getPreferenceManager().getFbId());
                return mp;
            }
        };

        VolleyUtils.getInstance().addToRequestQueue(syncReportedReq,"syncReq",context);
    }

    private void handleSyncResponse(String response) {
        Log.d("ReportTest"," response "+response);
        try {
            JSONObject jo = new JSONObject(response);
            if(jo.getBoolean("hasResult")){
                LiveUnitePreferenceManager preferenceManager = new LiveUnitePreferenceManager(context);
                preferenceManager.setReportedPost("#");
                JSONArray jr = jo.getJSONArray("results");
                for(int i = 0 ;i<jr.length();i++){
                    JSONObject _ro = (JSONObject) jr.get(i);
                    preferenceManager.setReportedPost(preferenceManager.getReportedPosts()+_ro.getString("post_id")+"#");
                }

            }

        } catch (JSONException e) {

        }
    }

    @Override
    protected void onResume() {
        startService(new Intent(this, AppForegroundCheckService.class));
        updateToolbar();//Toolbar();
        super.onResume();
    }

    public static HomeActivity getInstance() {
        return homeActivity;
    }

    private void setNavigation() {
        DrawerListAdapter adapter = new DrawerListAdapter(this, mNavItems);
        mDrawerList.setAdapter(adapter);

        // Drawer Item click listeners
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //selectItemFromDrawer(position);
                openPage(position);
                TextView subTitle, title;
                ImageView icon;

            }
        });

        getProfileDetails();
    }

    private void openPage(int position) {
        switch (position) {
            case 0:
                new ChangeActivity().change(context, DiscoverySetting.class);
                break;
            case 1:
                new ChangeActivity().change(context, AppSetting.class);
                break;
            case 2:
                startEmailIntent();
                break;
            case 3:
                rateLiveUnite();
                break;
        }
    }

    private void startEmailIntent() {

        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"tech@liveunite.com"});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Help From LiveUnite");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "May I Help You");
       startActivity(Intent.createChooser(emailIntent, "Send Mail"));

    }

    private void rateLiveUnite() {

        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
    }

    private void getProfileDetails() {
        if (!new ExtraMethods().isLogedIn(context)) {
            new ExtraMethods().completeLogOut(context);
            new ChangeActivity().change(context, LoginActivity.class);
            finish();
        } else {
            setProfileDetails();
        }
    }

    public void setProfileDetails() {

        String first_name = LiveUnite.getInstance().getPreferenceManager().getFirstname();
        String last_name = LiveUnite.getInstance().getPreferenceManager().getLastname();


        tvNameHeader.setText(first_name
                + " " + last_name);
        setDp();
        setFragment(R.id.tvMoments);
    }


    private void setArrayForNaviGation() {
        mNavItems.add(new NavItem("Discovery setting", "Distance,age and more", "\uE880"));
        mNavItems.add(new NavItem("App setting", "Notification and more", "\uE8B8"));
        mNavItems.add(new NavItem("Help \u0026 support", "Contact and more", "\uE887"));
        mNavItems.add(new NavItem("Rate Us", "We'll love you forever", "\uE8D0"));
    }

    private void setUpToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(null);
        setSupportActionBar(mToolbar);
        tvCamera = (TextView) findViewById(R.id.tvCamera);
        tvCamera.setOnClickListener(this);
        tvNavigation = (TextView) findViewById(R.id.tvNavigation);
        tvNavigation.setOnClickListener(this);

    }

    private void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.navList);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        tvNameHeader = (TextView) findViewById(R.id.tvNameHeader);
        tvViewProfileHeader = (TextView) findViewById(R.id.tvViewProfileHeader);
        tvViewProfileHeader.setOnClickListener(this);
        ivDPHeader = (ImageView) findViewById(R.id.ivDPHeader);
        navigationTop = (RelativeLayout) findViewById(R.id.navigationTop);
        navigationTop.setOnClickListener(this);

        tvMoments = (TextView) findViewById(R.id.tvMoments);
        tvChat= (TextView) findViewById(R.id.tvChat);
        tvUnReadCounter = (TextView) findViewById(R.id.unReadCountTextToolbar);

        tvChat.setOnClickListener(this);

        //set typeface
        Typeface typeface  = FontManager.getInstance(this).getTypeFace();
        tvMoments.setTypeface(typeface);
        tvCamera.setTypeface(typeface);
        tvChat.setTypeface(typeface);
        tvNavigation.setTypeface(typeface);

        updateToolbar();


    }

    public void updateToolbar() {

        int unreadCount = DatabaseHelper.getInstance(context).getUnreadCount();
        if(unreadCount>0){
            tvUnReadCounter.setVisibility(View.VISIBLE);
            tvUnReadCounter.setText(""+unreadCount);
        }else{
            tvUnReadCounter.setVisibility(View.GONE);
        }

    }


    public void selectDrawerItem(MenuItem menuItem) {
        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        setFragment(menuItem.getItemId());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvNavigation:
                if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    tvNavigation.setTextColor(Color.parseColor("#004444"));
                    mDrawerLayout.closeDrawer(Gravity.LEFT);
                } else {
                    tvNavigation.setTextColor(Color.parseColor("#77b530"));
                    mDrawerLayout.openDrawer(Gravity.LEFT);
                }
                break;
            case R.id.tvCamera:
                OpenCameraType.getInstance().setOpenPostCamera(true);
                new ChangeActivity().change(context, CameraActivity.class);
                break;
            case R.id.navigationTop:
                 new ChangeActivity().change(context, ViewProfile.class);
                break;
            case R.id.tvChat:

                //navigate to chat wall activity
                Intent chatWallIntent = new Intent(HomeActivity.this, ChatWall.class);
                chatWallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chatWallIntent);

                break;
        }
    }


    private void setFragment(int id) {
        switch (id) {
            case R.id.tvMoments:
                switchFragment(MomentsFragment.class);
                break;
            case R.id.iDiscovery:
                Toast.makeText(context, "Discovery Setting will be open", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    private void switchFragment(Class fragmentClass) {
        Fragment fragment = null;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_fragment, fragment).commit();
    }

    @Override
    public void onBackPressed() {

        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            tvNavigation.setTextColor(Color.parseColor("#004444"));
        } else {
            super.onBackPressed();
        }
    }

    public void setDp() {
        {

            String fbId = LiveUnite.getInstance().getPreferenceManager().getFbId();
            int width = getScreenWidth();
            String dpUrl = "https://graph.facebook.com/" + fbId+ "/picture?width="+width+"&height="+width;

            if (!fbId.isEmpty()) {
                Picasso.with(context).load(dpUrl).into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        Singleton.getInstance().getUserDetails().setBitmap(bitmap);
                        ivDPHeader.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
            }

        }
    }

    private int getScreenWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    @Override
    public void onDPchange() {
        Bitmap bitmap = Singleton.getInstance().getUserDetails().getBitmap();
        if (bitmap!=null)
        {
            ivDPHeader.setImageBitmap(bitmap);
        }
    }
}
