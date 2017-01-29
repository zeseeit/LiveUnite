package com.liveunite.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.R;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;

public class AppSetting extends AppCompatActivity implements View.OnClickListener {

    Button bLogOut,bShare,bTerms;
    Context context;
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
                LoginManager.getInstance().logOut();
                new ChangeActivity().change(context,LoginActivity.class);
                HomeActivity.getInstance().finish();
                finish();
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
