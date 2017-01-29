package com.liveunite.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by arunkr on 11/10/16.
 */


public class UploadResponse
{
    @SerializedName("success")
    @Expose
    private String success;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("url")
    @Expose
    private String url;


    public String getUrl() {
        return url;
    }

    public String getMessage() {
        return message;
    }

    public String isSuccess() {
        return success;
    }
}

