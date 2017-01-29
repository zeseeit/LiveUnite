package com.liveunite.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.liveunite.interfaces.LiveUniteApi;
import com.liveunite.models.DeletePostRequest;
import com.liveunite.models.DeletePostResponce;
import com.liveunite.models.FeedsResponse;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.network.Urls;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Vishwesh on 29-10-2016.
 */

public class SinglePostFragment extends BottomSheetDialogFragment implements View.OnClickListener {
    TextView tvDiatance, tvTime, tvName, tvCaption, tvAge,tvChat;
    ImageView ivPhoto,ivDismiss, ivDownload, ivDelete;
    private Context context;
    FeedsResponse mFeedsResponse;
    Target target;
    Dialog dialog;

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        context = getContext();
        View contentView = View.inflate(getContext(), R.layout.fragment_single_post, null);
        dialog.setContentView(contentView);
        this.mFeedsResponse = Singleton.getInstance().getFeedsResponse();
        initView(contentView);
        this.dialog = dialog;
        ivDismiss.setOnClickListener(this);
        ivDelete.setOnClickListener(this);
        ivDownload.setOnClickListener(this);
        setData();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // In a previous life I used this method to get handles to the positive and negative buttons
                // of a dialog in order to change their Typeface. Good ol' days.

                BottomSheetDialog d = (BottomSheetDialog) dialog;

                // This is gotten directly from the source of BottomSheetDialog
                // in the wrapInBottomSheet() method
                FrameLayout bottomSheet = (FrameLayout) d.findViewById(android.support.design.R.id.design_bottom_sheet);

                // Right here!
                BottomSheetBehavior.from(bottomSheet)
                        .setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    private void setData() {
        String width = mFeedsResponse.getImageWidth();
        String height = mFeedsResponse.getImageHeight();
        int w = Singleton.getInstance().getScreenWidth();
        int h = Singleton.getInstance().getScreenWidth();
        if (!width.isEmpty())
        {
            w = Integer.parseInt(mFeedsResponse.getImageWidth());
        }
        if (!height.isEmpty())
        {
            h = Integer.parseInt(mFeedsResponse.getImageHeight());
        }
        ivPhoto.getLayoutParams().height =h;
        ivPhoto.getLayoutParams().width = w;
        Picasso.with(context).load(mFeedsResponse.getUrl()).resize(w,h).placeholder(R.drawable.fbavatat).into(ivPhoto);

        tvTime.setText(mFeedsResponse.getTime());
        tvCaption.setText(mFeedsResponse.getCaption());
        tvDiatance.setText(mFeedsResponse.getDistance());

    }

    private void initView(View view) {
        tvDiatance = (TextView) view.findViewById(R.id.tvDistance);
        tvTime = (TextView) view.findViewById(R.id.tvTime);
        tvCaption = (TextView) view.findViewById(R.id.tvCaption);
        tvName = (TextView) view.findViewById(R.id.tvName);
        tvName.setVisibility(View.GONE);
        ivPhoto = (ImageView) view.findViewById(R.id.ivPhoto);
        tvChat = (TextView) view.findViewById(R.id.tvChat);
        tvAge = (TextView) view.findViewById(R.id.tvAge);
        tvAge.setVisibility(View.GONE);
        ivDelete = (ImageView) view.findViewById(R.id.ivDelete);
        ivDismiss = (ImageView) view.findViewById(R.id.ivDismiss);
        ivDownload = (ImageView) view.findViewById(R.id.ivDownload);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivDelete:
                delete();
                break;
            case R.id.ivDownload:
                downloadImage();
                break;
            case R.id.ivDismiss:
                dialog.dismiss();
                break;
        }
    }

    public interface onDelete{
        public void onDelete(int position);
    }

    private void delete() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Urls.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        int position = Singleton.getInstance().getTempPosition();
        if (position!=-99)
        {
            ((onDelete)context).onDelete(position);
            dialog.dismiss();;
        }

        LiveUniteApi liveUniteApi = retrofit.create(LiveUniteApi.class);


        DeletePostRequest deletePostRequest = new DeletePostRequest();
        deletePostRequest.setAndroidAppToken(Singleton.getInstance().getUserDetails().getAndroidAppToken());
        deletePostRequest.setFbId(Singleton.getInstance().getUserDetails().getFbId());
        deletePostRequest.setId(Singleton.getInstance().getUserDetails().getId());
        deletePostRequest.setPostId(mFeedsResponse.getId());
        Call<ArrayList<DeletePostResponce>> deletePostResponceCall = liveUniteApi.deletePost(deletePostRequest);
        deletePostResponceCall.enqueue(new Callback<ArrayList<DeletePostResponce>>() {
            @Override
            public void onResponse(Call<ArrayList<DeletePostResponce>> call, Response<ArrayList<DeletePostResponce>> response) {
                Log.e("Responce delete Post",response.body().toString());

            }

            @Override
            public void onFailure(Call<ArrayList<DeletePostResponce>> call, Throwable t) {
                Log.e("ResponcedeletePost fail",t.toString());
            }
        });
    }

    private void downloadImage() {

        target = new Target() {

            @Override
            public void onPrepareLoad(Drawable arg0) {
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom arg1) {
                String root = Environment.getExternalStorageDirectory().toString();

                try {
                    File myDir = new File(root + "/liveUnite/media");

                    if (!myDir.exists()) {
                        myDir.mkdirs();
                    }

                    String name = "liveUnite-"+mFeedsResponse.getCaption() + ".png"; // + ".jpg";
                    myDir = new File(myDir, name);
                    FileOutputStream out = new FileOutputStream(myDir);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);

                    out.flush();
                    out.close();
                    Toast.makeText(context,"Successfully downloaded",Toast.LENGTH_LONG).show();
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                            Uri.parse("file://"+ Environment.getExternalStorageDirectory()+ "/liveUnite/media" + name)));
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.parse(root+ "/liveUnite/media" + name)));
                   /* Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri uri = Uri.parse("/storage/emulated/0/liveUnite/media/" + "liveUnite"+mFeedsResponse.getCaption()+".png");
                    intent.setDataAndType(uri, "image*//*");
                    startActivity(intent);*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }
        };

        Picasso.with(context).load(mFeedsResponse.getUrl()).into(target);
    }


}
