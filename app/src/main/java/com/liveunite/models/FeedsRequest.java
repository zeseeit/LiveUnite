package com.liveunite.models;

/**
 * Created by Vishwesh on 08-10-2016.
 */

public class FeedsRequest {

    private String fbId= "";
    private double latitude =0;
    private double longitude =0;
    private int fromLimit = 0;
    private int toLimit = 0;
    private String id ="";
    private String androidAppToken="";
    private String homeProfile ="";
    private float maxDistance = 0;
    private float minAge = 13;
    private float maxAge = 60;


    public String getFbId() {
        return fbId;
    }

    public void setFbId(String fbId) {
        this.fbId = fbId;
    }

    public int getFromLimit() {
        return fromLimit;
    }

    public void setFromLimit(int fromLimit) {
        this.fromLimit = fromLimit;
    }

    public int getToLimit() {
        return toLimit;
    }

    public void setToLimit(int toLimit) {
        this.toLimit = toLimit;
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

    public String getHomeProfile() {
        return homeProfile;
    }

    public void setHomeProfile(String homeProfile) {
        this.homeProfile = homeProfile;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(float maxAge) {
        this.maxAge = maxAge;
    }

    public float getMinAge() {
        return minAge;
    }

    public void setMinAge(float minAge) {
        this.minAge = minAge;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }

}
