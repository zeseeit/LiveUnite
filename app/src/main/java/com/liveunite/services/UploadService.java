package com.liveunite.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.Retry.MomentCacheModel;
import com.liveunite.Retry.MomentsCache;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.helper.Segmentor;
import com.liveunite.fragments.MomentsFragment;
import com.liveunite.infoContainer.OpenCameraType;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.interfaces.IUploadFile;
import com.liveunite.models.UploadResponse;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ImageOperations;
import com.liveunite.network.ServiceGenerator;
import com.liveunite.utils.UpdateLocations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by arunkr on 11/10/16.
 */

public class UploadService extends IntentService {
    IUploadFile upload = ServiceGenerator.createService(IUploadFile.class);

    int type;
    private MomentsCache momentsCache;
    private String currentUploading = "";
    public UploadService() {
        super(UploadService.class.getName());
        momentsCache = new MomentsCache(LiveUnite.getInstance().getApplicationContext());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String f = intent.getStringExtra(Constant.UPLOAD_FILENAME);
        String caption = intent.getStringExtra(Constant.UPLOAD_CAPTION);
        boolean isRetrying = intent.getBooleanExtra("isRetry",false);
        File file =null;
        if (f!=null){
            String fileName = isRetrying ? Constants.RETRY_MOMENTS.DIR_CACHE_MOMENTS+f:f;
            file = new File(fileName);
            Log.d("MomentsCache"," reading from "+file.getAbsolutePath());
             type = intent.getIntExtra(Constant.UPLOAD_TYPE, Constant.TYPE_PICTURE);

            boolean canDelete = intent.getBooleanExtra(Constant.UPLOAD_AUTODELETE, false);
            if (file.exists()) {
                if (type == Constant.TYPE_PICTURE || type == Constant.TYPE_PROFILE_CATION_PHOTO) {
                    Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
                    int max_image_dimen = (b.getWidth() > b.getHeight()) ? b.getWidth() : b.getHeight();
                    b.recycle();
                    if (max_image_dimen > Constant.IMAGE_MAX_DIMEN) {
                        File newFile = scaleImage(file);
                        if (canDelete) {
                            file.delete();
                        }
                        if (newFile.exists()) {
                            uploadImage(newFile, caption, true);
                        }
                    } else {
                        uploadImage(file, caption, canDelete);
                    }
                } else if (type == Constant.TYPE_VIDEO) {
                    uploadVideo(file, caption, canDelete);
                }

            }
        }else
        {
            uploadImage(file, caption, false);
        }
    }

    void uploadImage(final File file, final String caption, final boolean canDelete) {

        new UpdateLocations(this).initiateLocationFetch();

        if (type!=Constant.TYPE_PROFILE_CATION_PHOTO && MomentsFragment.getMomentsFragment()!=null)
        {
            MomentsFragment.getMomentsFragment().uploadStarted(file,caption);
        }
        MultipartBody.Part body = null;
        if (file!=null) {
            final RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            body = MultipartBody.Part.createFormData("uploaded_files", file.getName(), requestFile);

            // prepare for cache
            currentUploading = file.getName();
            momentsCache.cacheMoment(file);

            momentsCache.setCacheMetaData(new MomentCacheModel(file.getName(),
                    String.valueOf(Singleton.getInstance().getUserLocationModal().getLatitude()),
                    String.valueOf(Singleton.getInstance().getUserLocationModal().getLongitude()),
                    caption));

            Log.d("MomentsCache"," cached Upload...");
        }

        Call<ArrayList<UploadResponse>> call = upload.postImage(
                body,
                caption,
                LiveUnite.getInstance().getPreferenceManager().getLiveUnitId(),
                LiveUnite.getInstance().getPreferenceManager().getFbId(),
                Singleton.getInstance().getUserDetails().getAndroidAppToken(),
                OpenCameraType.getInstance().isOpenPostCamera()
                , String.valueOf(Singleton.getInstance().getUserLocationModal().getLongitude()),
                String.valueOf(Singleton.getInstance().getUserLocationModal().getLatitude()),file!=null);

        Log.e("UploadServiceCameraType","User Id: - "+LiveUnite.getInstance().getPreferenceManager().getLiveUnitId()+" FbId : - " +
                LiveUnite.getInstance().getPreferenceManager().getFbId());

        call.enqueue(new Callback<ArrayList<UploadResponse>>() {
            @Override
            public void onResponse(Call<ArrayList<UploadResponse>> call, Response<ArrayList<UploadResponse>> response) {
                try {

                    if (response.body().size()>0) {

                        Log.d("UploadTest"," response "+response.body().get(0).getMessage());

                        if (response.body().get(0).isSuccess().equals("1")) {

                            if (type!=Constant.TYPE_PROFILE_CATION_PHOTO && MomentsFragment.getMomentsFragment()!=null)
                            {
                                MomentsFragment.getMomentsFragment().uploadCompleted(true);
                            }
                            if (!OpenCameraType.getInstance().isOpenPostCamera()) {
                                Singleton.getInstance().getUserDetails().setDpUrl(response.body().get(0).getUrl());
                            }

                            momentsCache.clearCache(currentUploading);

                        }
                        if (canDelete)
                        {
                            file.delete();
                        }
                    }
                } catch (Exception ex) {

                }
            }


            @Override
            public void onFailure(Call<ArrayList<UploadResponse>> call, Throwable t) {
                if (type!=Constant.TYPE_PROFILE_CATION_PHOTO && MomentsFragment.getMomentsFragment()!=null) {
                    MomentsFragment.getMomentsFragment().uploadCompleted(false);
                }
                Log.e("UPLOAD", "Failed: " + t.toString());
            }

            @Override
            public String toString() {
                Log.e("qwerty",super.toString());
                return super.toString();
            }
        });
    }

    void uploadVideo(final File file, final String caption, final boolean canDelete) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("uploaded_files", file.getName(), requestFile);

        Call<UploadResponse> call = upload.postVideo(body,
                caption,
                Singleton.getInstance().getUserDetails().getId(),
                Singleton.getInstance().getUserDetails().getFbId(),
                Singleton.getInstance().getUserDetails().getAndroidAppToken());
        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                try {
                    if (response.body().isSuccess().equals("1") && canDelete) {
                        file.delete();
                    }
                } catch (Exception ex) {

                }
            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                Log.e("UPLOAD", "Failed");
            }
        });
    }

    File scaleImage(File file) {
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());

        double scale = calcScaleRatio(bmp.getWidth(), bmp.getHeight());
        File newFile = new File(getExternalFilesDir(null)
                , System.currentTimeMillis() + "_2.jpg");

        try {

            FileOutputStream fos = new FileOutputStream(newFile);
            Bitmap scaled = ImageOperations.scaleDown(bmp, (float) scale, true);
            scaled.compress(Bitmap.CompressFormat.PNG, 90, fos);

            fos.close();
            scaled.recycle();
            bmp.recycle();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newFile;

    }

    private double calcScaleRatio(int width, int height) {
        int maxDimen = (width > height) ? width : height;
        double ratio = Constant.IMAGE_MAX_DIMEN / maxDimen;
        return ratio;
    }
}
