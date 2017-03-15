package com.liveunite.adapter;

/**
 * Created by vishwesh on 25/9/16.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.Retry.MomentsCache;
import com.liveunite.activities.MediaActivity;
import com.liveunite.chat.activities.ChatRoom;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.FontManager;
import com.liveunite.chat.gcm.LiveUnitePreferenceManager;
import com.liveunite.chat.helper.LiveUniteTime;
import com.liveunite.chat.helper.Segmentor;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.models.FeedsResponse;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.CheckInternetConnection;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ExtraMethods;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyViewHolder> {

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private Context context;

    private static ArrayList<FeedsResponse> postList;
    OnLoadMoreListener loadMoreListener;
    boolean isLoading = false, isMoreDataAvailable = true;
    Uri uri;
    int size;
    FeedsResponse mFeedsResponse;

    public abstract void distanceClick(FeedsResponse mFeedsResponse);
    public abstract  void onRetry(FeedsResponse mFeedsResponse, int position);

    public AdapterPosts(Context context, ArrayList<FeedsResponse> postList,int size) {
        this.postList = postList;
        this.context = context;
        this.size =size;

        //  mMediaController = new MediaController(context);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == VIEW_TYPE_ITEM) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_posts, parent, false);
            return new MyViewHolder(itemView, true);
        } else if (viewType == VIEW_TYPE_LOADING) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.loading_feeds_progress, parent, false);
            return new MyViewHolder(itemView, false);
        }
        return null;


    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        if ((position+(int)(0.3*Constant.LOAD_FEEDS_LIMITS)) >= getItemCount() - 1 && isMoreDataAvailable &&
                !isLoading && loadMoreListener != null) {
            isLoading = true;
            loadMoreListener.onLoadMore();
        } else if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            if (postList.get(position)!=null) {
                ((MyViewHolder) holder).bindData(context, postList.get(position),size);
                holder.tvDiatance.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        distanceClick(postList.get(position));
                    }
                });
                holder.ivError.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (holder.ivError.getVisibility() == View.VISIBLE)
                        {
                            onRetry(postList.get(position),position);
                        }
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return postList == null ? 0 : postList.size();
        //return 10;
    }

    @Override
    public int getItemViewType(int position) {
        return postList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiatance, tvTime, tvName, tvCaption, tvAge ,tvChat , tvReport;
        ImageView ivPhoto, ivError;
        ProgressBar progressBar;
        VideoView vvVideo;
        private Uri uri;
        private Context context;
        private Typeface typeface;
        private LiveUnitePreferenceManager preferenceManager;

        MyViewHolder(View view, boolean b) {

            super(view);

            if (b) {
                typeface = FontManager.getInstance(context).getTypeFace();

                tvDiatance = (TextView) view.findViewById(R.id.tvDistance);
                tvTime = (TextView) view.findViewById(R.id.tvTime);
                tvCaption = (TextView) view.findViewById(R.id.tvCaption);
                tvName = (TextView) view.findViewById(R.id.tvName);
                tvReport = (TextView) view.findViewById(R.id.tvReport);
                ivPhoto = (ImageView) view.findViewById(R.id.ivPhoto);
                ivPhoto.getLayoutParams().height = Singleton.getInstance().getScreenWidth();
                ivPhoto.getLayoutParams().width = Singleton.getInstance().getScreenWidth();
                tvChat = (TextView) view.findViewById(R.id.tvChat);
                tvAge = (TextView) view.findViewById(R.id.tvAge);
                vvVideo = (VideoView) view.findViewById(R.id.vvVideo);
                progressBar = (ProgressBar) itemView.findViewById(R.id.pbUploading);
                ivError = (ImageView)itemView.findViewById(R.id.ivError);
                tvChat.setTypeface(typeface);
                tvReport.setTypeface(typeface);

                tvReport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        FeedsResponse feed = postList.get(position);
                        confirmReporting(view,feed);
                    }
                });

                tvChat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if( LiveUnite.getInstance().getPreferenceManager().getGcmToken().length()==0)
                        {
                            Toast.makeText(context, "Not Registered For Chat! Plz Restart LiveUnite.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int position = getAdapterPosition();
                        FeedsResponse feed = postList.get(position);
                        Intent chatIntent = new Intent(context,ChatRoom.class);
                        chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Bundle bundle = new Bundle();
                        //Log.d("FeedsAdapter"," fbId - "+feed.getId()+" dpUrl - "+feed.getDpUrl() + "title "+feed.getFirst_name()+" "+feed.getLast_name());
                        bundle.putString("fbId",getFbIdFromDpUrl(feed.getDpUrl()));
                        bundle.putString("dpUrl",feed.getDpUrl());
                        bundle.putString("title",feed.getFirst_name()+" "+feed.getLast_name());
                        chatIntent.putExtras(bundle);
                        context.startActivity(chatIntent);

                    }
                });

            } else {
                progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar1);
            }
        }

        private void confirmReporting(final View view,final FeedsResponse feed){

            DialogInterface.OnClickListener downloaDialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            reportPost(view,feed.getPostId(),LiveUnite.getInstance().getPreferenceManager().getFbId(),getFbIdFromDpUrl(feed.getDpUrl()));
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //dismiss dialog
                            dialog.dismiss();
                            break;
                    }
                }
            };

            AlertDialog.Builder builderDownloadAlert = new AlertDialog.Builder(context);
            builderDownloadAlert.setTitle("Report Moment");
            builderDownloadAlert.setMessage("Do Want To Report This Moment ? ").setPositiveButton("Report", downloaDialogClickListener)
                    .setNegativeButton("Cancel", downloaDialogClickListener).show();

        }


        private void reportPost(View view, final String pid , final String selfFbId , final String cmpFbId){
            // check internet connection and disable the button after click
            StringRequest reportRequest = new StringRequest(Request.Method.POST, Constants.SERVER.URL_REPORT_POST, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
//{"error":false,"message":"Record Success"}

                    try {
                        JSONObject resObj = new JSONObject(response);
                        if(!resObj.getBoolean("error")){
                            addReportItemToSharedPref(pid);
                            Toast.makeText(context,"Reported Post",Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                        // show error toast
                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String ,String> mp = new HashMap<>();
                    mp.put("userId", selfFbId);
                    mp.put("cmpId", cmpFbId);
                    mp.put("time", LiveUniteTime.getInstance().getDateTime());
                    mp.put("postId",pid);
                    return mp;
                }
            };

            if(new CheckInternetConnection(context).isConnectedToInternet()){

                VolleyUtils.getInstance().addToRequestQueue(reportRequest,"reportPost",context);
                view.setVisibility(View.GONE);

            }else{
                Toast.makeText(context, "No Connectivity ", Toast.LENGTH_SHORT).show();
            }

        }

        private void addReportItemToSharedPref(String item){
            preferenceManager = new LiveUnitePreferenceManager(context);
            //Log.d("ReportTest"," adding to sp "+item);
            preferenceManager.setReportedPost(preferenceManager.getReportedPosts()+"#"+item);
        }

        private boolean amIReportedThisPost(String pid){
            Log.d("ReportTest"," check for pid "+pid);
            preferenceManager = new LiveUnitePreferenceManager(context);
            ArrayList<String> r_pids = new Segmentor().getParts(preferenceManager.getReportedPosts(),'#');
            Log.d("ReportTest"," reported posts ids "+r_pids.toString());
                    for(String p:r_pids){
                       // Log.d("ReportTest"," checking pid  "+p+" against "+pid);
                        if(p.equals(pid))
                            return true;
                    }
            return false;
        }

        private String getFbIdFromDpUrl(String dpUrl) {

            String fbId = dpUrl.replaceAll("\\D+","");
            //Log.d("FeedsAdapter","FbId "+fbId);
            return fbId;
        }

        public void bindData(final Context context, final FeedsResponse mFeedsResponse, int size) {
            this.context = context;
            if (mFeedsResponse.getType()
                    .equals(Constant.MEDIA_PHOTO_TYPE)) {
                vvVideo.setVisibility(View.GONE);
                ivPhoto.setVisibility(View.VISIBLE);

               // ivPhoto.setImageBitmap(stringToBitMap(mFeedsResponse.getBase_64()));
                if (mFeedsResponse.getFile()==null) {
                    if (mFeedsResponse.getImageHeight() !=null &&!mFeedsResponse.getImageHeight().isEmpty()
                            && mFeedsResponse.getImageWidth()!=null && !mFeedsResponse.getImageWidth().isEmpty()) {
                        ivPhoto.requestLayout();
                        Picasso.with(context).load(mFeedsResponse.getUrl()).resize(size, size)
                                .centerCrop().placeholder(R.drawable.fbavatat).into(ivPhoto);
                    } else {
                        Picasso.with(context).load(mFeedsResponse.getUrl()).
                                fit().placeholder(R.drawable.fbavatat).into(ivPhoto);
                    }
                }else
                {
                    Uri uri = Uri.fromFile(mFeedsResponse.getFile());
                    ivPhoto.setImageURI(uri);

                }



            } else if (mFeedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE)) {
                ivPhoto.setVisibility(View.VISIBLE);
                vvVideo.setVisibility(View.GONE);
                try {
                    Drawable[] layers = new Drawable[2];
                    layers[0] = new BitmapDrawable(ExtraMethods.retriveVideoFrameFromVideo(mFeedsResponse.getUrl()));
                    layers[1] = ContextCompat.getDrawable(context, R.drawable.ic_play_circle_outline_black_24dp);
                    LayerDrawable layerDrawable = new LayerDrawable(layers);
                    ivPhoto.setImageDrawable(layerDrawable);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
               /* mMediaController.setAnchorView(holder.vvVideo);
                mMediaController.setVisibility(View.GONE);*/
                uri = Uri.parse(mFeedsResponse.getUrl());
                // holder.vvVideo.setMediaController(mMediaController);
                vvVideo.setVideoURI(uri);
                vvVideo.start();
            }


            if (mFeedsResponse.getFile()!=null)
            {
                tvDiatance.setText("0m");
                tvName.setText("You");

                if (mFeedsResponse.getAge()!=null)
                {
                    tvAge.setText("," +mFeedsResponse.getAge());
                }else
                {
                    tvAge.setVisibility(View.GONE);
                }

                if (mFeedsResponse.isUploaded() )
                {
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText("0m");
                    progressBar.setVisibility(View.GONE);
                    ivError.setVisibility(View.GONE);
                }else
                {
                    tvTime.setVisibility(View.GONE);
                    if (mFeedsResponse.isUploading())
                    {
                        progressBar.setVisibility(View.VISIBLE);
                        ivError.setVisibility(View.GONE);
                    }else
                    {
                        progressBar.setVisibility(View.GONE);
                        ivError.setVisibility(View.VISIBLE);

                    }
                }

            }else
            {
                tvName.setText(mFeedsResponse.getFirst_name() );
              //  tvAge.setVisibility(View.GONE);
                if (!mFeedsResponse.getAge().isEmpty())
                {
                    tvAge.setText(", " +mFeedsResponse.getAge());
                }else
                {
                    tvAge.setVisibility(View.GONE);
                }


                if(Singleton.getInstance().getUserLocationModal().getLatitude()==0.0){
                    tvDiatance.setText("---m");
                }else{
                    tvDiatance.setText(mFeedsResponse.getDistance());
                }
                tvTime.setText(mFeedsResponse.getTime());
                tvTime.setVisibility(View.VISIBLE);
                ivError.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }
            if (!mFeedsResponse.getCaption().isEmpty())
            {
                String f_cap = "";
                try {
                     f_cap = new String(mFeedsResponse.getCaption().getBytes(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                tvCaption.setText(f_cap);

                tvCaption.setVisibility(View.VISIBLE);
            }else
            {
                tvCaption.setVisibility(View.GONE);
            }
            tvCaption.setText(mFeedsResponse.getCaption());


            if(amIReportedThisPost(mFeedsResponse.getPostId())){
                Log.d("ReportTest"," checking for report post "+mFeedsResponse.getId());
                tvReport.setVisibility(View.GONE);
            }else{
                tvReport.setVisibility(View.VISIBLE);
            }

            ivPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFeedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE)) {
                        ivPhoto.setVisibility(View.GONE);
                        vvVideo.setVisibility(View.VISIBLE);
                        vvVideo.start();
                    }else if(mFeedsResponse.getType().equals(Constant.MEDIA_PHOTO_TYPE))
                    {
                        Singleton.getInstance().setFeedsResponse(mFeedsResponse);
                        new ChangeActivity().change(context, MediaActivity.class);
                    }
                }
            });

            vvVideo.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mFeedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE) && vvVideo.isPlaying()) {
                        ivPhoto.setVisibility(View.VISIBLE);
                        vvVideo.setVisibility(View.GONE);
                        vvVideo.pause();
                    } else if (mFeedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE)) {
                        vvVideo.setVisibility(View.GONE);
                        ivPhoto.setVisibility(View.VISIBLE);
                    }
                    return true;
                }
            });

            vvVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mFeedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE)) {
                        ivPhoto.setVisibility(View.VISIBLE);
                        vvVideo.setVisibility(View.GONE);
                    }
                }
            });

            vvVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return true;
                }
            });
        }

        public Bitmap stringToBitMap(String encodedString){
            try{
                byte [] encodeByte=Base64.decode(encodedString, Base64.DEFAULT);
                Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
                return bitmap;
            }catch(Exception e){
                e.getMessage();
                return null;
            }
        }
    }

    static class LoadHolder extends RecyclerView.ViewHolder {
        public LoadHolder(View itemView) {
            super(itemView);
        }
    }

    public void setMoreDataAvailable(boolean moreDataAvailable) {
        isMoreDataAvailable = moreDataAvailable;
    }

    /* notifyDataSetChanged is final method so we can't override it
         call adapter.notifyDataChanged(); after update the list
         */
    public void notifyDataChanged() {
        notifyDataSetChanged();
        isLoading = false;
    }


    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }


}
