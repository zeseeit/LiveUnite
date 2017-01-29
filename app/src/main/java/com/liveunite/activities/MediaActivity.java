package com.liveunite.activities;


import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;

import com.liveunite.models.FeedsResponse;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.views.TouchImageView;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ExtraMethods;
import com.squareup.picasso.Picasso;

/**
 * A simple {@link Fragment} subclass.
 */
public class MediaActivity extends Activity {

    FeedsResponse feedsResponse = new FeedsResponse();
    TouchImageView ivPhoto;
    Context context;
    TextView tvDetails;
    VideoView vvVideo;


    public MediaActivity() {
        // Required empty public constructor
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context =MediaActivity.this;
        // Inflate the layout for this fragment
        setContentView(R.layout.activity_media);
        setFeed();
        init();
    }


    private void init() {
        ivPhoto = (TouchImageView)findViewById(R.id.ivPhoto);
        vvVideo = (VideoView)findViewById(R.id.vvVideo);
        vvVideo.setVisibility(View.GONE);
        tvDetails = (TextView)findViewById(R.id.tvDetails);
        if (feedsResponse.getType().equals(Constant.MEDIA_PHOTO_TYPE))
        {
            ivPhoto.setVisibility(View.VISIBLE);
            if (feedsResponse.getFile()==null) {
                String width = feedsResponse.getImageWidth();
                String height = feedsResponse.getImageHeight();
                int w = Singleton.getInstance().getScreenWidth();
                int h = Singleton.getInstance().getScreenWidth();
                if (!width.isEmpty()) {
                    w = Integer.parseInt(feedsResponse.getImageWidth());
                }
                if (!height.isEmpty()) {
                    h = Integer.parseInt(feedsResponse.getImageHeight());
                }
                Picasso.with(context).load(feedsResponse.getUrl()).resize(w, h).placeholder(R.drawable.fbavatat).into(ivPhoto);
            }else
            {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(feedsResponse.getFile().getName(), options);
                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;
                if (imageHeight==0 ) {
                    imageHeight = Singleton.getInstance().getScreenWidth();
                }
                if (imageWidth==0) {
                    imageWidth = Singleton.getInstance().getScreenWidth();
                }
                Picasso.with(context).load(feedsResponse.getFile()).
                        resize(imageWidth,imageHeight).centerCrop().placeholder(R.drawable.fbavatat).into(ivPhoto);
            }
        }else if (feedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE))
        {
            vvVideo.setVideoURI(Uri.parse(feedsResponse.getUrl()));
            try {
            Drawable[] layers = new Drawable[2];
            layers[0] = new BitmapDrawable( ExtraMethods.retriveVideoFrameFromVideo(feedsResponse.getUrl()));
            layers[1] = ContextCompat.getDrawable(context, R.drawable.ic_play_circle_outline_black_24dp);
            LayerDrawable layerDrawable = new LayerDrawable(layers);
            ivPhoto.setImageDrawable(layerDrawable);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        ivPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (feedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE))
                {
                    if (vvVideo.isPlaying())
                    {
                        vvVideo.pause();
                        vvVideo.setVisibility(View.GONE);
                        ivPhoto.setVisibility(View.VISIBLE);
                    }else
                    {
                        vvVideo.start();
                        vvVideo.setVisibility(View.VISIBLE);
                        ivPhoto.setVisibility(View.GONE);
                    }
                }
            }
        });

        vvVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (feedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE) && vvVideo.isPlaying()) {
                    ivPhoto.setVisibility(View.VISIBLE);
                    vvVideo.setVisibility(View.GONE);
                    vvVideo.pause();
                }else if (feedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE))
                {
                    vvVideo.setVisibility(View.GONE);
                    ivPhoto.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
        tvDetails.setText(feedsResponse.getCaption()+"\n"
                +new ExtraMethods().getDateBefore(context,feedsResponse.getDays(),feedsResponse.getMin(),
                feedsResponse.getHrs()));

        vvVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                ivPhoto.setVisibility(View.VISIBLE);
                vvVideo.setVisibility(View.GONE);
            }
        });
    }

    public void setFeed() {
        this.feedsResponse = Singleton.getInstance().getFeedsResponse();
        if (feedsResponse==null)
        {
            finish();
        }
    }
}
