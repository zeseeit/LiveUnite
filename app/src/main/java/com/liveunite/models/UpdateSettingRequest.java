package com.liveunite.models;

/**
 * Created by Vishwesh on 05-11-2016.
 */

public class UpdateSettingRequest {
    private String fbId;
    private String id;
    private float minAge;
    private float maxAge;
    private float maxDistance;
    private String androidAppToken;

    public String getFbId() {
        return fbId;
    }

    public void setFbId(String fbId) {
        this.fbId = fbId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getAndroidAppToken() {
        return androidAppToken;
    }

    public void setAndroidAppToken(String androidAppToken) {
        this.androidAppToken = androidAppToken;
    }

    public float getMinAge() {
        return minAge;
    }

    public void setMinAge(float minAge) {
        this.minAge = minAge;
    }

    public float getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(float maxAge) {
        this.maxAge = maxAge;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }
}
