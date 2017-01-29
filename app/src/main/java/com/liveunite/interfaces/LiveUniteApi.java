package com.liveunite.interfaces;

/**
 * Created by Vishwesh on 08-10-2016.
 */
import com.liveunite.models.DeletePostRequest;
import com.liveunite.models.DeletePostResponce;
import com.liveunite.models.FeedsRequest;
import com.liveunite.models.FeedsResponse;
import com.liveunite.models.RegisterRequest;
import com.liveunite.models.UpdateSettingRequest;
import com.liveunite.models.UserDetails;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LiveUniteApi {
    @POST("register.php")
    Call<ArrayList<UserDetails>> getRegStatus(@Body RegisterRequest registerRequest);

    @POST("getFeeds.php")
    Call<ArrayList<FeedsResponse>> getFeeds(@Body FeedsRequest feedsRequest);

    @POST("deletePost.php")
    Call<ArrayList<DeletePostResponce>> deletePost(@Body DeletePostRequest deletePostRequest);

    @POST("updateSetting.php")
    Call<ArrayList<DeletePostResponce>> updateSetting(@Body UpdateSettingRequest updateSettingRequest);
}
