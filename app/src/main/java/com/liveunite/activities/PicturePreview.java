package com.liveunite.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.liveunite.infoContainer.OpenCameraType;
import com.liveunite.R;
import com.liveunite.services.UploadService;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.liveunite.utils.UpdateLocations;

import java.io.File;

/**
 * Created by arunkr on 12/10/16.
 */

public class PicturePreview extends AppCompatActivity implements View.OnClickListener
{
    ImageView imgPreview,btnOk, btnCancel,play_btn;
    EditText txtCaption;

    String filename;
    int type;
    private Context context;

    boolean autodelete = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context = PicturePreview.this;
        setContentView(R.layout.activity_preview_picture);

        imgPreview = (ImageView)findViewById(R.id.imgPreview);
        btnOk = (ImageView)findViewById(R.id.btnOk);
        btnCancel = (ImageView)findViewById(R.id.btnCancel);
        txtCaption = (EditText)findViewById(R.id.preview_caption);
        play_btn = (ImageView)findViewById(R.id.play_btn);

        btnOk.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        play_btn.setOnClickListener(this);

        filename = getIntent().getStringExtra(Constant.PREVIEW_FILENAME);
        type = getIntent().getIntExtra(Constant.UPLOAD_TYPE,-1);
        autodelete = getIntent().getBooleanExtra(Constant.UPLOAD_AUTODELETE,false);

        if(filename !=null || type !=-1)
        {
            if(type == Constant.TYPE_PICTURE)
            {
                if (OpenCameraType.getInstance().isOpenPostCamera())
                {
                    txtCaption.setHint(R.string.preview_textbox_hint_image);

                    txtCaption.setVisibility(View.VISIBLE);
                }else
                {
                    txtCaption.setVisibility(View.GONE);
                }
                play_btn.setVisibility(View.GONE);
                Bitmap bmp = BitmapFactory.decodeFile(filename);
                if (bmp == null) {
                    finish();
                }
                imgPreview.setImageBitmap(bmp);
            }
            else if(type == Constant.TYPE_VIDEO)
            {
                txtCaption.setHint(R.string.preview_textbox_hint_video);
                Bitmap b =ThumbnailUtils.createVideoThumbnail(filename,
                        MediaStore.Video.Thumbnails.MINI_KIND);
                imgPreview.setImageBitmap(b);
            }
        }
        else
        {
            finish();
        }
    }



    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.play_btn:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filename));
                intent.setDataAndType(Uri.parse(filename), "video/mp4");
                startActivity(intent);
                break;
            case R.id.btnOk:
                if (OpenCameraType.getInstance().isOpenPostCamera())
                {
                    UpdateLocations locations = new UpdateLocations(context);
                    locations.initiateLocationFetch();

                    if (locations.getLocationStatus()) {

                        String text = String.valueOf(txtCaption.getText().toString()).trim().replace("\"","&quot;");
                        Log.d("UploadTest"," text "+text);
                        uploadFile(filename, type, text, autodelete);

                        new ChangeActivity().change(context, HomeActivity.class);
                    }
                }else
                {
                    EditProfile.getInstance().changeDP(filename,type,autodelete);
                }
                finish();
                break;

            case R.id.btnCancel:
                File f = new File(filename);
                if(autodelete && f.exists())
                {
                    f.delete();
                }
                finish();
                break;
        }
    }


    void uploadFile(String filename,int type, String caption, boolean auto_delete)
    {
        Intent intent = new Intent(this,UploadService.class);
        intent.putExtra(Constant.UPLOAD_FILENAME,filename);
        intent.putExtra(Constant.UPLOAD_CAPTION,caption.trim());
        intent.putExtra(Constant.UPLOAD_AUTODELETE,auto_delete);
        intent.putExtra(Constant.UPLOAD_TYPE,type);
        startService(intent);
    }
}
