package com.liveunite.adapter;

/**
 * Created by vishwesh on 25/9/16.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liveunite.models.FeedsResponse;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ExtraMethods;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public abstract class AdapterProfileMedia extends RecyclerView.Adapter<AdapterProfileMedia.MyViewHolder> {

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private final int VIEW_TYPE_TOP = 2;
    private final int size;
    private Context context;

    private ArrayList<FeedsResponse> postList;
    AdapterPosts.OnLoadMoreListener loadMoreListener;
    boolean isLoading = false, isMoreDataAvailable = true;
    Uri uri;
    FeedsResponse mFeedsResponse;

    public abstract void postClick(int position, FeedsResponse feedsResponse);

    public AdapterProfileMedia(Context context, ArrayList<FeedsResponse> postList, int i) {
        this.postList = postList;
        this.context = context;
        this.size = i;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == VIEW_TYPE_ITEM) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_profile_media, parent, false);
            return new MyViewHolder(itemView, VIEW_TYPE_ITEM);
        } else if (viewType == VIEW_TYPE_LOADING) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.loading_feeds_progress, parent, false);
            return new MyViewHolder(itemView, VIEW_TYPE_LOADING);
        } else if (viewType == VIEW_TYPE_TOP) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_view_profile_top, parent, false);
            return new MyViewHolder(itemView, VIEW_TYPE_TOP);
        }
        return null;


    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        if ((position - 12) >= getItemCount() - 1 && isMoreDataAvailable &&
                !isLoading && loadMoreListener != null) {
            isLoading = true;
            loadMoreListener.onLoadMore();
        } else if (getItemViewType(position) == VIEW_TYPE_ITEM || getItemViewType(position) == VIEW_TYPE_TOP) {

            ((AdapterProfileMedia.MyViewHolder) holder).bindData(context, postList.get(position), getItemViewType(position));
            if (position != 0)
                holder.ivPhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postClick(position, postList.get(position));
                    }
                });

        }
    }

    @Override
    public int getItemCount() {
        return postList == null ? 0 : postList.size();
        //return 10;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_TOP;
        }
        return postList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }


    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView ivPhoto;
        ProgressBar progressBar;
        private Context context;
        TextView tvAge, tvName, tvBio;
        ImageView ivDP;
        LinearLayout llContainer;

        MyViewHolder(View view, int b) {
            super(view);
            if (b == VIEW_TYPE_ITEM) {
                ivPhoto = (ImageView) view.findViewById(R.id.ivPhoto);
                ivPhoto.requestLayout();
                ivPhoto.getLayoutParams().height = size;
                ivPhoto.getLayoutParams().width = size;
                ivPhoto.setClickable(true);
                llContainer = (LinearLayout) view.findViewById(R.id.llContainer);
            } else if (b == VIEW_TYPE_LOADING) {
                progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar1);
            } else if (b == VIEW_TYPE_TOP) {
                tvAge = (TextView) itemView.findViewById(R.id.tvAge);
                tvName = (TextView) itemView.findViewById(R.id.tvName);
                tvBio = (TextView) itemView.findViewById(R.id.tvBio);
                ivDP = (ImageView) itemView.findViewById(R.id.ivDP);
            }
        }

        private void bindData(final Context context, final FeedsResponse mFeedsResponse, int viewType) {
            this.context = context;
            if (viewType == VIEW_TYPE_TOP && mFeedsResponse!=null) {

                updateBio(mFeedsResponse.getBio());
                tvAge.setText(" ," + mFeedsResponse.getAge());
                tvName.setText(mFeedsResponse.getFirst_name()
                        + " " + mFeedsResponse.getLast_name());

                updateDP(mFeedsResponse.getDpUrl());

            } else if (mFeedsResponse != null) {
                if (viewType == VIEW_TYPE_ITEM) {
                    if (mFeedsResponse.getType().equals(Constant.MEDIA_PHOTO_TYPE)) {
                        Picasso.with(context).load(mFeedsResponse.getUrl()).resize(Singleton.getInstance().getScreenWidth()
                                , Singleton.getInstance().getScreenWidth()).centerCrop()
                                .placeholder(R.color.colorPrimaryDark).into(ivPhoto);

                    } else if (mFeedsResponse.getType().equals(Constant.MEDIA_VIDEO_TYPE)) {
                        Drawable[] layers = new Drawable[2];
                        try {
                            layers[0] = new BitmapDrawable(ExtraMethods.retriveVideoFrameFromVideo(mFeedsResponse.getUrl()));
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
                            //    new ChangeActivity().sendToFullScreenMedia(context, po, ivPhoto);

                        }
                    });
                }
            }
        }

        public void updateDP(String url) {

            Picasso.with(context).load(url).resize(Singleton.getInstance().getScreenWidth()
                    , Singleton.getInstance().getScreenWidth()).centerCrop()
                    .placeholder(R.drawable.fbavatat).into(ivDP);


        }

        private void updateBio(String bio) {
            tvBio.setText(bio);
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

    public void setLoadMoreListener(AdapterPosts.OnLoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }


}
