package com.liveunite.interfaces;

import com.liveunite.models.UploadResponse;

import java.util.ArrayList;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by arunkr on 13/10/16.
 */
public interface IUploadFile {
    @Multipart
    @POST("uploadImage.php")
    Call<ArrayList<UploadResponse>> postImage(@Part MultipartBody.Part data,
                                              @Part("caption") String caption,
                                              @Part("id") String id,
                                              @Part("fbId") String fbId,
                                              @Part("androidAppToken") String androidAppToken,
                                              @Part("isPostImage") boolean isPostImage,
                                              @Part("longitude") String longitude,
                                              @Part("latitude") String latitude,
                                              @Part("isPhotoChange") boolean isPhotoChange
    );

    @Multipart
    @POST("videoupload.php")
    Call<UploadResponse> postVideo(@Part MultipartBody.Part data,
                                   @Part("caption") String caption,
                                   @Part("id") String id,
                                   @Part("fbId") String fbId,
                                   @Part("androidAppToken") String androidAppToken);
}
