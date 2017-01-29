package com.liveunite.models;

/**
 * Created by Vishwesh on 05-11-2016.
 */

public class DeletePostRequest {
    private String fbId;
    private String id;
    private String postId;
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

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getAndroidAppToken() {
        return androidAppToken;
    }

    public void setAndroidAppToken(String androidAppToken) {
        this.androidAppToken = androidAppToken;
    }
}
