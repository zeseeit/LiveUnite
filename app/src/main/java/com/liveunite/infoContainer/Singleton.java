package com.liveunite.infoContainer;

import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.chat.gcm.LiveUniteGCMIntentService;
import com.liveunite.models.FeedsResponse;
import com.liveunite.models.UserDetails;
import com.liveunite.models.UserLocationModal;

import java.io.File;

import static com.facebook.FacebookSdk.getApplicationContext;
import static com.liveunite.utils.Constant.PLAY_SERVICES_RESOLUTION_REQUEST;

/**
 * Created by Vishwesh on 27-10-2016.
 */

public class Singleton {

    public static Singleton mInstance = null;

    public static Singleton getInstance() {
        if (mInstance == null) {
            mInstance = new Singleton();
        }
        return mInstance;
    }

    private UserDetails userDetails = new UserDetails();
    private FeedsResponse feedsResponse;
    private UserLocationModal userLocationModal = new UserLocationModal();
    private UserLocationModal postLocationDetails = new UserLocationModal();
    private int tempPosition = -99;
    private int xSize;
    private File file;


    public void setScrrenWidth(int size) {
        this.xSize = size;
    }

    public UserDetails getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    public FeedsResponse getFeedsResponse() {
        return feedsResponse;
    }

    public void setFeedsResponse(FeedsResponse feedsResponse) {
        this.feedsResponse = feedsResponse;
    }

    public UserLocationModal getUserLocationModal() {
        return userLocationModal;
    }

    public void setUserLocationModal(UserLocationModal userLocationModal) {
        this.userLocationModal = userLocationModal;
    }

    public void setPostLocationDetails(double latitude, double longitude) {
        postLocationDetails = new UserLocationModal();
        postLocationDetails.setLatitude(latitude);
        postLocationDetails.setLongitude(longitude);
    }

    public UserLocationModal getPostLocationDetails() {
        return postLocationDetails;
    }

    public int getTempPosition() {
        return tempPosition;
    }

    public void setTempPosition(int tempPosition) {
        this.tempPosition = tempPosition;
    }

    public int getScreenWidth() {
        return this.xSize;
    }

    public void setxSize(int xSize) {
        this.xSize = xSize;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

}
