package com.liveunite.infoContainer;

/**
 * Created by Vishwesh on 10-10-2016.
 */

public class OpenCameraType {
    public static OpenCameraType mInstance = null;
    public  boolean openPostCamera = true; // false for ChangeDPcamera

    public OpenCameraType() {

    }

    public void setOpenPostCamera(boolean openPostCamera) {
        this.openPostCamera = openPostCamera;
    }

    public boolean isOpenPostCamera() {
        return openPostCamera;
    }

    public static OpenCameraType getInstance() {
        if (mInstance == null) {
            mInstance = new OpenCameraType();
        }
        return mInstance;
    }

}
