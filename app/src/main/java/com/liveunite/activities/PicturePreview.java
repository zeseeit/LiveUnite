package com.liveunite.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.liveunite.ColorFilter.ThumbnailCallback;
import com.liveunite.ColorFilter.ThumbnailItem;
import com.liveunite.ColorFilter.ThumbnailsAdapter;
import com.liveunite.ColorFilter.ThumbnailsManager;
import com.liveunite.infoContainer.OpenCameraType;
import com.liveunite.R;
import com.liveunite.services.UploadService;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.liveunite.utils.UpdateLocations;
import com.zomato.photofilters.SampleFilters;
import com.zomato.photofilters.imageprocessors.Filter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by arunkr on 12/10/16.
 */

public class PicturePreview extends AppCompatActivity implements View.OnClickListener , ThumbnailCallback
{
    ImageView imgPreview,btnOk, btnCancel,play_btn;
    EditText txtCaption;
    String filename;
    int type;
    private Context context;
    private RecyclerView thumbnailList;
    boolean autodelete = false;
    private Activity activity;
    private boolean filterPanelExpanded = false;
    private Bitmap selectedBitmap = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context = PicturePreview.this;
        activity = this;
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
                selectedBitmap = BitmapFactory.decodeFile(filename);
                if (selectedBitmap == null) {
                    finish();
                }
                imgPreview.setImageBitmap(selectedBitmap);
            }
        }

        initUIWidgets();

    }


    private void initUIWidgets() {
        thumbnailList = (RecyclerView) findViewById(R.id.thumbnails);
        initHorizontalList();
    }

    private void initHorizontalList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.scrollToPosition(0);
        thumbnailList.setLayoutManager(layoutManager);
        thumbnailList.setHasFixedSize(true);
        bindDataToAdapter();
    }

    private void bindDataToAdapter() {
        final Context context = this.getApplication();
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                Bitmap thumbImage = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filename), 640, 640, false);
                ThumbnailItem t1 = new ThumbnailItem();
                ThumbnailItem t2 = new ThumbnailItem();
                ThumbnailItem t3 = new ThumbnailItem();
                ThumbnailItem t4 = new ThumbnailItem();
                ThumbnailItem t5 = new ThumbnailItem();
                ThumbnailItem t6 = new ThumbnailItem();

                t1.image = thumbImage;
                t2.image = thumbImage;
                t3.image = thumbImage;
                t4.image = thumbImage;
                t5.image = thumbImage;
                t6.image = thumbImage;
                ThumbnailsManager.clearThumbs();
                ThumbnailsManager.addThumb(t1); // Original Image

                t2.filter = SampleFilters.getStarLitFilter();
                ThumbnailsManager.addThumb(t2);

                t3.filter = SampleFilters.getBlueMessFilter();
                ThumbnailsManager.addThumb(t3);

                t4.filter = SampleFilters.getAweStruckVibeFilter();
                ThumbnailsManager.addThumb(t4);

                t5.filter = SampleFilters.getLimeStutterFilter();
                ThumbnailsManager.addThumb(t5);

                t6.filter = SampleFilters.getNightWhisperFilter();
                ThumbnailsManager.addThumb(t6);

                List<ThumbnailItem> thumbs = ThumbnailsManager.processThumbs(context);

                ThumbnailsAdapter adapter = new ThumbnailsAdapter(thumbs, (ThumbnailCallback) activity);
                thumbnailList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        };
        handler.post(r);
    }

    @Override
    public void onThumbnailClick(Filter filter) {
        selectedBitmap = filter.processFilter(Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filename), 640, 640, false));
        imgPreview.setImageBitmap(selectedBitmap);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.play_btn:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filename));
                intent.setDataAndType(Uri.parse(filename), "video/mp4");
                startActivity(intent);
                break;
            case R.id.btnOk:

                new Thread(){
                    @Override
                    public void run() {
                        writeBackMoments(selectedBitmap);
                    }
                }.start();



                if (OpenCameraType.getInstance().isOpenPostCamera())
                {
                    UpdateLocations locations = new UpdateLocations(context);
                    locations.initiateLocationFetch();
                    if (locations.getLocationStatus()) {
                       final String text = String.valueOf(txtCaption.getText().toString()).trim().replace("\"","&quot;");
                        Log.d("UploadTest"," text "+text);
                       final ProgressDialog pd = new ProgressDialog(this);
                        pd.setMessage("Preparing Your Moment...");
                        pd.show();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                new ChangeActivity().change(context, HomeActivity.class);
                                uploadFile(filename, type, text, autodelete);
                                finish();
                            }
                        },10000);

                    }

                }else
                {
                    EditProfile.getInstance().changeDP(filename,type,autodelete);
                }

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

    private void writeBackMoments(Bitmap selectedBitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    void uploadFile(String filename,int type, String caption, boolean auto_delete) {
        Intent intent = new Intent(this,UploadService.class);
        intent.putExtra(Constant.UPLOAD_FILENAME,filename);
        intent.putExtra(Constant.UPLOAD_CAPTION,caption.trim());
        intent.putExtra(Constant.UPLOAD_AUTODELETE,auto_delete);
        intent.putExtra(Constant.UPLOAD_TYPE,type);
        startService(intent);
    }
}
