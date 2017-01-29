package com.liveunite.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.interfaces.LiveUniteApi;
import com.liveunite.models.FeedsRequest;
import com.liveunite.models.UserDetails;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.network.Urls;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Vishwesh on 29-09-2016.
 */

public class ExtraMethods {
    public boolean isLogedIn(Context context) {
        FacebookSdk.sdkInitialize(context);
        // for testing purpose always true;
       // return true;
        return  AccessToken.getCurrentAccessToken() != null;
    }

    public void completeLogOut(Context context) {
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();
        LiveUnite.getInstance().getPreferenceManager().flushDetails();
        Singleton.getInstance().setUserDetails(new UserDetails());
    }

    public FeedsRequest getFeedRequest(String _fbId,String homeProfile) {

        UserDetails userDetails = Singleton.getInstance().getInstance().getUserDetails();

        String fbId = LiveUnite.getInstance().getPreferenceManager().getFbId();
        String liveUnite = LiveUnite.getInstance().getPreferenceManager().getLiveUnitId();

        FeedsRequest feedsRequest = new FeedsRequest();
        feedsRequest.setAndroidAppToken(userDetails.getAndroidAppToken());
        feedsRequest.setFbId(_fbId);
        feedsRequest.setId(liveUnite);
        feedsRequest.setHomeProfile(homeProfile);
        feedsRequest.setMaxAge((int) LiveUnite.getInstance().getPreferenceManager().getMaxAge());
        feedsRequest.setMinAge((int) LiveUnite.getInstance().getPreferenceManager().getMinAge());
        feedsRequest.setMaxDistance((int) LiveUnite.getInstance().getPreferenceManager().getMaxDistance());
        feedsRequest.setLongitude(Singleton.getInstance().getUserLocationModal().getLongitude());
        feedsRequest.setLatitude(Singleton.getInstance().getUserLocationModal().getLatitude());
        return feedsRequest;

    }

    public LiveUniteApi getLiveUniteAPI() {

        return getRetrofit().create(LiveUniteApi.class);
    }

    private Retrofit getRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(Urls.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public String getDateBefore(Context context, String days, String min, String hrs) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, (-1)*Integer.parseInt(days));
        cal.add(Calendar.MINUTE, (-1)*Integer.parseInt(min));
        cal.add(Calendar.HOUR_OF_DAY, (-1)*Integer.parseInt(hrs));
        DateFormat dateFormat;
        if (days.equals( "0"))
        {
             dateFormat = new SimpleDateFormat("hh:mm a");
        }else
        {
            Calendar mCalendar = Calendar.getInstance();
           if (mCalendar.get(Calendar.YEAR) == cal.get(Calendar.YEAR) )
           {
               dateFormat = new SimpleDateFormat("dd MMM, hh:mm a");
           }else
           {
               dateFormat = new SimpleDateFormat("dd MMM, yyyy, hh:mm a");
           }
        }


        return dateFormat.format(cal.getTime());
    }


    public static Bitmap retriveVideoFrameFromVideo(String videoPath)
            throws Throwable {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            if (Build.VERSION.SDK_INT >= 14)
                mediaMetadataRetriever.setDataSource(videoPath, new HashMap<String, String>());
            else
                mediaMetadataRetriever.setDataSource(videoPath);
            //   mediaMetadataRetriever.setDataSource(videoPath);
            bitmap = mediaMetadataRetriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Throwable(
                    "Exception in retriveVideoFrameFromVideo(String videoPath)"
                            + e.getMessage());

        } finally {
            if (mediaMetadataRetriever != null) {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }
}
