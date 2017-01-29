package com.liveunite.models;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Vishwesh on 27-10-2016.
 */

public class UserDetails {

    private Bitmap bitmap = null;

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }

    public void setMinAge(float minAge) {
        this.minAge = minAge;
    }

    public void setMaxAge(float maxAge) {
        this.maxAge = maxAge;
    }

    @SerializedName("maxDistance")
    @Expose
    private float maxDistance;

    @SerializedName("minAge")
    @Expose
    private float minAge;

    @SerializedName("maxAge")
    @Expose
    private float maxAge;


    @SerializedName("success")
    @Expose
    private String success;

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("fbId")
    @Expose
    private String fbId;

    @SerializedName("first_name")
    @Expose
    private String first_name;

    @SerializedName("last_name")
    @Expose
    private String last_name;

    @SerializedName("email")
    @Expose
    private String email;

    @SerializedName("phone")
    @Expose
    private String phone;

    @SerializedName("gender")
    @Expose
    private String gender;

    @SerializedName("dateOfBirth")
    @Expose
    private String dateOfBirth;

    @SerializedName("androidAppToken")
    @Expose
    private String androidAppToken;

    @SerializedName("age")
    @Expose
    private String age;

    public void setBio(String bio) {
        this.bio = bio;
    }

    @SerializedName("bio")
    @Expose
    private String bio;

    @SerializedName("dpUrl")
    @Expose
    private String dpUrl;


    public String getFbId() {
        return fbId;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getGender() {
        return gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAndroidAppToken() {
        return androidAppToken;
    }

    public String getAge() {
        return age;
    }

    public String getBio() {
        return bio;
    }

    public String getDpUrl() {
        return dpUrl;
    }

    public String getSuccess() {
        return success;
    }

    public String getId() {
        return id;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setDpUrl(String dpUrl) {
        this.dpUrl = dpUrl;
    }

    public float getMaxAge() {
        return maxAge;
    }

    public float getMinAge() {
        return minAge;
    }

    public float getMaxDistance() {
        return maxDistance;
    }
}

