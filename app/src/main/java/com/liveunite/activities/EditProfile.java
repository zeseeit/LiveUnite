package com.liveunite.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.chat.gcm.LiveReciever;
import com.liveunite.infoContainer.OpenCameraType;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.services.UploadService;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.liveunite.utils.UpdateLocations;
import com.liveunite.opencamera.CameraActivity;

import java.io.File;

public class EditProfile extends AppCompatActivity implements View.OnClickListener {

    Toolbar toolbar;
    ImageView ivPhoto;
    EditText etBio;
    TextView tvChangeDP;
    Context context;
    static EditProfile editProfile;
    int type;
    String caption,filename;
    boolean autodelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = EditProfile.this;
        editProfile = this;
        setContentView(R.layout.activity_edit_profile);
        setUpToolbar();
        init();
    }

    public interface onDPchange{
        public void onDPchange();
    }

    public static EditProfile getInstance(){
        return editProfile;
    }

    private void init() {
        ivPhoto = (ImageView)findViewById(R.id.ivPhoto);
        tvChangeDP = (TextView)findViewById(R.id.tvChangeDP);
        tvChangeDP.setOnClickListener(this);
        etBio = (EditText)findViewById(R.id.etBio);
        etBio.setText(LiveUnite.getInstance().getPreferenceManager().getBio());

        setDP(Singleton.getInstance().getUserDetails().getBitmap());
    }

    private void setDP(Bitmap bitmap) {

        if (bitmap!=null)
        {
            ivPhoto.setImageBitmap(bitmap);
        }else
        {
            ivPhoto.setImageResource(R.drawable.fbavatat);
        }
    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Edit Profile");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_edit_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.iSave:
                saveData();
                break;
        }
        return  true;
    }

    private void saveData() {
        caption = etBio.getText().toString();
        Singleton.getInstance().getUserDetails().setBio(caption);
        Singleton.getInstance().getUserDetails().setBitmap(getBitmap(filename));
        ((onDPchange)HomeActivity.getInstance()).onDPchange();
        ((onDPchange)ViewProfile.getInstance()).onDPchange();
        OpenCameraType.getInstance().setOpenPostCamera(false);

        UpdateLocations locations = new UpdateLocations(context);
        locations.initiateLocationFetch();

        if (locations.getLocationStatus()) {
            upload();
        }else
        {
            Toast.makeText(context,"Can't get your location.Please try again later",Toast.LENGTH_SHORT).show();
        }
    }

    private void upload() {
        Intent intent = new Intent(context,UploadService.class);
        intent.putExtra(Constant.UPLOAD_FILENAME,filename);
        intent.putExtra(Constant.UPLOAD_CAPTION,etBio.getText().toString().trim());
        intent.putExtra(Constant.UPLOAD_AUTODELETE,autodelete);
        intent.putExtra(Constant.UPLOAD_TYPE,Constant.TYPE_PROFILE_CATION_PHOTO);
        startService(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.tvChangeDP:
                OpenCameraType.getInstance().setOpenPostCamera(false);
                new ChangeActivity().change(context, CameraActivity.class);
                break;
        }
    }

    public void changeDP(String filename, int type, boolean autodelete) {
        setDP(getBitmap(filename));
        this.caption = caption;
        this.type = type;
        this.autodelete = autodelete;
        this.filename = filename;
       // Picasso.with(context).load(new File(filename)).fit().into(ivPhoto);
    }

    private void setBio(String caption) {
        etBio.setText(caption);
    }

    private Bitmap getBitmap(String filename) {
        if (filename!=null) {
            Bitmap bitmap = BitmapFactory.decodeFile(new File(filename).getAbsolutePath());
            bitmap = Bitmap.createScaledBitmap(bitmap, getScreenWidth(), getScreenWidth(), true);
            return bitmap;
        }else
        {
            return Singleton.getInstance().getUserDetails().getBitmap();
        }
    }

    private int getScreenWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
}
