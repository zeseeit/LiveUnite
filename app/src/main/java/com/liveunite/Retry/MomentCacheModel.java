package com.liveunite.Retry;

/**
 * Created by Ankit on 3/12/2017.
 */

public class MomentCacheModel {
    public String file_name;
    public String latitude;
    public String longitude;
    public String caption;

    public MomentCacheModel(String file_name , String latitude, String longitude, String caption) {
        this.file_name = file_name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.caption = caption;
    }
}
