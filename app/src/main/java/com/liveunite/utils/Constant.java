package com.liveunite.utils;

import android.util.SparseIntArray;
import android.view.Surface;

/**
 * Created by Vishwesh on 28-09-2016.
 */

public class Constant {

    public static long SLEEP_TIME = 2000;
    public static String FB_GRAPH = "https://graph.facebook.com";
    public static String FB_DP_TYPE = "?type=large";
    public static String FB_DP = "/picture";
    public static float DISTANCE_START = 0;
    public static float DISTANCE_END = 250;
    public static float DISTANCE_INTERVAL = 1;
    public static float AGE_START = 13;
    public static float AGE_END = 60;
    public static float AGE_INTERVAL = 1;
    public static String SHARING_TEXT_START = "Hi! You are awesome and so does people around you. " +
            "Discover people locally and express yourself without worrying because life is lived in moments. " +
            "Check out https://play.google.com/store/apps/details?id=";
    public static String SHARING_TEXT_END = " made with \uD83D\uDC96 in India.";
    public static int LOAD_FEEDS_LIMITS = 5;
    public static int LOAD_FEEDS_PROFILE_MEDIA = 24;
    public static final String UPLOAD_FILENAME = "UPLOAD_FILENAME";
    public static final String UPLOAD_CAPTION = "UPLOAD_CAPTION";
    public static final String UPLOAD_TYPE = "UPLOAD_TYPE";
    public static final String PREVIEW_FILENAME = "PREVIEW_FILENAME";
    public static final int TYPE_PICTURE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_PROFILE_CATION_PHOTO = 3;
    public static final String UPLOAD_AUTODELETE = "UPLOAD_AUTODELETE";
    public static final int GALLERY_RESULT = 1;
    public static final double IMAGE_MAX_DIMEN = 1248.0; // max width or height

    public static final SparseIntArray DISPLAY_ORIENTATIONS = new SparseIntArray();

    public static String ITEM_POSITION_FULL_SCREEN = "itemNumber";
    public static String MEDIA_PHOTO_TYPE = "Photo";
    public static String MEDIA_VIDEO_TYPE = "Video";
    public static int MEDIA_IN_ONE_ROW_PROFILE = 3;

    public static String PING_SITE = "www.google.com";
    public static int PLAY_SERVICES_RESOLUTION_REQUEST = 123;

    static {
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_0, 0);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_90, 90);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_180, 180);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_270, 270);
    }
}
