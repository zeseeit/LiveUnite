package com.liveunite.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.File;

/**
 * Created by Vishwesh on 08-10-2016.
 */

public class FeedsResponse {

    @SerializedName("success")
    @Expose
    private String success;

/*    @SerializedName("base_64")
    @Expose
    private String base_64;*/

    @SerializedName("message")
    @Expose
    private String message;


    @SerializedName("url")
    @Expose
    private String url;

    @SerializedName("dpUrl")
    @Expose
    private String dpUrl;


    @SerializedName("imageWidth")
    @Expose
    private String imageWidth;

    @SerializedName("imageHeight")
    @Expose
    private String imageHeight;

    @SerializedName("bio")
    @Expose
    private String bio;

    @SerializedName("longitude")
    @Expose
    private double longitude;

    @SerializedName("latitude")
    @Expose
    private double latitude;

    @SerializedName("first_name")
    @Expose
    private String first_name;

    @SerializedName("last_name")
    @Expose
    private String last_name;

    @SerializedName("gender")
    @Expose
    private String gender;

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("postId")
    @Expose
    private String postId;


    @SerializedName("age")
    @Expose
    private String age;

    @SerializedName("time")
    @Expose
    private String time;

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @SerializedName("isUploaded")
    @Expose
    private boolean isUploaded = true;

    @SerializedName("isUploading")
    @Expose
    private boolean isUploading = false;

    @SerializedName("file")
    @Expose
    private File file = null;

    @SerializedName("caption")
    @Expose
    private String caption;

    public void setType(String type) {
        this.type = type;
    }

    @SerializedName("type")
    @Expose
    private String type;


    @SerializedName("days")
    @Expose
    private String days="0";

    @SerializedName("min")
    @Expose

    private String min="0";

    @SerializedName("hrs")
    @Expose
    private String hrs="0";

    @SerializedName("distance")
    @Expose
    private String distance;

    public String getDistance() {
        return distance;
    }

    public String getUrl() {
        return url;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public String getGender() {
        return gender;
    }

    public String getId() {
        return id;
    }

    public String getPostId(){return postId;}

    public String getAge() {
        return age;
    }

    public String getTime() {
        return time;
    }

    public String getCaption() {
        return caption;
    }

    public String getType() {
        return type;
    }

    public String getDays() {
        return days;
    }

    public String getMin() {
        return min;
    }

    public String getHrs() {
        return hrs;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getDpUrl() {
        return dpUrl;
    }

    public String getBio() {
        return bio;
    }

    public String getImageWidth() {
        return imageWidth;
    }

    public String getImageHeight() {
        return imageHeight;
    }

    public String getMessage() {
        return message;
    }

    public String getSuccess() {
        return success;
    }

    public boolean isUploaded() {
        return isUploaded;
    }

    public void setUploaded(boolean uploaded) {
        isUploaded = uploaded;
    }

    public boolean isUploading() {
        return isUploading;
    }

    public void setUploading(boolean uploading) {
        isUploading = uploading;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setAge(String age) {
        this.age = age;
    }

   /* public String getBase_64() {
        return base_64;
    }*/
}
