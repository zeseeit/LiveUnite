package com.liveunite.opencamera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.liveunite.R;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ImageOperations;
import com.liveunite.opencamera.CameraController.CameraController;
import com.liveunite.opencamera.Preview.ApplicationInterface;
import com.liveunite.opencamera.Preview.Preview;
import com.liveunite.opencamera.UI.DrawPreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Our implementation of ApplicationInterface, see there for details.
 */
public class MyApplicationInterface implements ApplicationInterface
{
    private static final String TAG = "MyApplicationInterface";
    public boolean test_set_available_memory = false;
    public long test_available_memory = 0;
    private CameraActivity main_activity = null;
    private StorageUtils storageUtils = null;
    private DrawPreview drawPreview = null;
    private ImageSaver imageSaver = null;
    private Rect text_bounds = new Rect();
    private boolean used_front_screen_flash = false;
    private boolean last_images_saf = false; // whether the last images array are using SAF or not
    private List<LastImage> last_images = new ArrayList<LastImage>();

    // camera properties which are saved in bundle, but not stored in preferences (so will be remembered if the app goes into background, but not after restart)
    private int cameraId = 0;
    private int zoom_factor = 0;
    private float focus_distance = 0.0f;

    MyApplicationInterface(CameraActivity main_activity, Bundle savedInstanceState)
    {
        long debug_time = 0;
        if (MyDebug.LOG)
        {
            Log.d(TAG, "MyApplicationInterface");
            debug_time = System.currentTimeMillis();
        }
        this.main_activity = main_activity;
        if (MyDebug.LOG)
            Log.d(TAG, "MyApplicationInterface: time after creating location supplier: " + (System.currentTimeMillis() - debug_time));
        this.storageUtils = new StorageUtils(main_activity);
        if (MyDebug.LOG)
            Log.d(TAG, "MyApplicationInterface: time after creating storage utils: " + (System.currentTimeMillis() - debug_time));
        this.drawPreview = new DrawPreview(main_activity, this);

        this.imageSaver = new ImageSaver(main_activity);
        this.imageSaver.start();

        if (savedInstanceState != null)
        {
            cameraId = savedInstanceState.getInt("cameraId", 0);
            if (MyDebug.LOG)
                Log.d(TAG, "found cameraId: " + cameraId);
            zoom_factor = savedInstanceState.getInt("zoom_factor", 0);
            if (MyDebug.LOG)
                Log.d(TAG, "found zoom_factor: " + zoom_factor);
            focus_distance = savedInstanceState.getFloat("focus_distance", 0.0f);
            if (MyDebug.LOG)
                Log.d(TAG, "found focus_distance: " + focus_distance);
        }

        if (MyDebug.LOG)
            Log.d(TAG, "MyApplicationInterface: total time to create MyApplicationInterface: " + (System.currentTimeMillis() - debug_time));
    }

    void onSaveInstanceState(Bundle state)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onSaveInstanceState");
        if (MyDebug.LOG)
            Log.d(TAG, "save cameraId: " + cameraId);
        state.putInt("cameraId", cameraId);
        if (MyDebug.LOG)
            Log.d(TAG, "save zoom_factor: " + zoom_factor);
        state.putInt("zoom_factor", zoom_factor);
        if (MyDebug.LOG)
            Log.d(TAG, "save focus_distance: " + focus_distance);
        state.putFloat("focus_distance", focus_distance);
    }

    protected void onDestroy()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onDestroy");
        if (drawPreview != null)
        {
            drawPreview.onDestroy();
        }
        if (imageSaver != null)
        {
            imageSaver.onDestroy();
        }
    }

    StorageUtils getStorageUtils()
    {
        return storageUtils;
    }

    ImageSaver getImageSaver()
    {
        return imageSaver;
    }

    @Override
    public Context getContext()
    {
        return main_activity;
    }

    @Override
    public boolean useCamera2()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (main_activity.supportsCamera2())
        {
            return sharedPreferences.getBoolean(PreferenceKeys.getUseCamera2PreferenceKey(), false);
        }
        return false;
    }

    @Override
    public int createOutputVideoMethod()
    {
        String action = main_activity.getIntent().getAction();
        if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action))
        {
            if (MyDebug.LOG)
                Log.d(TAG, "from video capture intent");
            Bundle myExtras = main_activity.getIntent().getExtras();
            if (myExtras != null)
            {
                Uri intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
                if (intent_uri != null)
                {
                    if (MyDebug.LOG)
                        Log.d(TAG, "save to: " + intent_uri);
                    return VIDEOMETHOD_URI;
                }
            }
            // if no EXTRA_OUTPUT, we should save to standard location, and will pass back the Uri of that location
            if (MyDebug.LOG)
                Log.d(TAG, "intent uri not specified");
            // note that SAF URIs don't seem to work for calling applications (tested with Grabilla and "Photo Grabber Image From Video" (FreezeFrame)), so we use standard folder with non-SAF method
            return VIDEOMETHOD_FILE;
        }
        boolean using_saf = storageUtils.isUsingSAF();
        return using_saf ? VIDEOMETHOD_SAF : VIDEOMETHOD_FILE;
    }

    @Override
    public File createOutputVideoFile() throws IOException
    {
        return storageUtils.createOutputMediaFile(StorageUtils.MEDIA_TYPE_VIDEO, "", "mp4", new Date());
    }

    @Override
    public Uri createOutputVideoSAF() throws IOException
    {
        return storageUtils.createOutputMediaFileSAF(StorageUtils.MEDIA_TYPE_VIDEO, "", "mp4", new Date());
    }

    @Override
    public Uri createOutputVideoUri() throws IOException
    {
        String action = main_activity.getIntent().getAction();
        if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action))
        {
            if (MyDebug.LOG)
                Log.d(TAG, "from video capture intent");
            Bundle myExtras = main_activity.getIntent().getExtras();
            if (myExtras != null)
            {
                Uri intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
                if (intent_uri != null)
                {
                    if (MyDebug.LOG)
                        Log.d(TAG, "save to: " + intent_uri);
                    return intent_uri;
                }
            }
        }
        throw new RuntimeException(); // programming error if we arrived here
    }

    @Override
    public int getCameraIdPref()
    {
        return cameraId;
    }

    @Override
    public void setCameraIdPref(int cameraId)
    {
        this.cameraId = cameraId;
    }

    @Override
    public String getFlashPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getFlashPreferenceKey(cameraId), "");
    }

    @Override
    public void setFlashPref(String flash_value)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getFlashPreferenceKey(cameraId), flash_value);
        editor.apply();
    }

    @Override
    public String getFocusPref(boolean is_video)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getFocusPreferenceKey(cameraId, is_video), "");
    }

    @Override
    public String getSceneModePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String value = sharedPreferences.getString(PreferenceKeys.getSceneModePreferenceKey(), "auto");
        return value;
    }

    @Override
    public void setSceneModePref(String scene_mode)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getSceneModePreferenceKey(), scene_mode);
        editor.apply();
    }

    @Override
    public String getColorEffectPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getColorEffectPreferenceKey(), "none");
    }

    @Override
    public void setColorEffectPref(String color_effect)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getColorEffectPreferenceKey(), color_effect);
        editor.apply();
    }

    @Override
    public String getWhiteBalancePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getWhiteBalancePreferenceKey(), "auto");
    }

    @Override
    public void setWhiteBalancePref(String white_balance)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getWhiteBalancePreferenceKey(), white_balance);
        editor.apply();
    }

    @Override
    public String getISOPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getISOPreferenceKey(), "auto");
    }

    @Override
    public void setISOPref(String iso)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getISOPreferenceKey(), iso);
        editor.apply();
    }

    @Override
    public int getExposureCompensationPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String value = sharedPreferences.getString(PreferenceKeys.getExposurePreferenceKey(), "0");
        if (MyDebug.LOG)
            Log.d(TAG, "saved exposure value: " + value);
        int exposure = 0;
        try
        {
            exposure = Integer.parseInt(value);
            if (MyDebug.LOG)
                Log.d(TAG, "exposure: " + exposure);
        } catch (NumberFormatException exception)
        {
            if (MyDebug.LOG)
                Log.d(TAG, "exposure invalid format, can't parse to int");
        }
        return exposure;
    }

    @Override
    public void setExposureCompensationPref(int exposure)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getExposurePreferenceKey(), "" + exposure);
        editor.apply();
    }

    @Override
    public Pair<Integer, Integer> getCameraResolutionPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String resolution_value = sharedPreferences.getString(PreferenceKeys.getResolutionPreferenceKey(cameraId), "");
        if (MyDebug.LOG)
            Log.d(TAG, "resolution_value: " + resolution_value);
        if (resolution_value.length() > 0)
        {
            // parse the saved size, and make sure it is still valid
            int index = resolution_value.indexOf(' ');
            if (index == -1)
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "resolution_value invalid format, can't find space");
            } else
            {
                String resolution_w_s = resolution_value.substring(0, index);
                String resolution_h_s = resolution_value.substring(index + 1);
                if (MyDebug.LOG)
                {
                    Log.d(TAG, "resolution_w_s: " + resolution_w_s);
                    Log.d(TAG, "resolution_h_s: " + resolution_h_s);
                }
                try
                {
                    int resolution_w = Integer.parseInt(resolution_w_s);
                    if (MyDebug.LOG)
                        Log.d(TAG, "resolution_w: " + resolution_w);
                    int resolution_h = Integer.parseInt(resolution_h_s);
                    if (MyDebug.LOG)
                        Log.d(TAG, "resolution_h: " + resolution_h);
                    return new Pair<Integer, Integer>(resolution_w, resolution_h);
                } catch (NumberFormatException exception)
                {
                    if (MyDebug.LOG)
                        Log.d(TAG, "resolution_value invalid format, can't parse w or h to int");
                }
            }
        }
        return null;
    }

    @Override
    public int getImageQualityPref()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getImageQualityPref");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String image_quality_s = sharedPreferences.getString(PreferenceKeys.getQualityPreferenceKey(), "90");
        int image_quality = 0;
        try
        {
            image_quality = Integer.parseInt(image_quality_s);
        } catch (NumberFormatException exception)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "image_quality_s invalid format: " + image_quality_s);
            image_quality = 90;
        }
        return image_quality;
    }

    @Override
    public boolean getFaceDetectionPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getFaceDetectionPreferenceKey(), false);
    }

    @Override
    public String getVideoQualityPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getVideoQualityPreferenceKey(cameraId), "");
    }

    @Override
    public void setVideoQualityPref(String video_quality)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getVideoQualityPreferenceKey(cameraId), video_quality);
        editor.apply();
    }

    @Override
    public boolean getVideoStabilizationPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getVideoStabilizationPreferenceKey(), false);
    }

    @Override
    public String getVideoBitratePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getVideoBitratePreferenceKey(), "default");
    }

    @Override
    public String getVideoFPSPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getVideoFPSPreferenceKey(), "default");
    }

    @Override
    public long getVideoMaxDurationPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String video_max_duration_value = sharedPreferences.getString(PreferenceKeys.getVideoMaxDurationPreferenceKey(), "0");
        long video_max_duration = 0;
        try
        {
            video_max_duration = (long) Integer.parseInt(video_max_duration_value) * 1000;
        } catch (NumberFormatException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "failed to parse preference_video_max_duration value: " + video_max_duration_value);
            e.printStackTrace();
            video_max_duration = 0;
        }
        return video_max_duration;
    }

    long getVideoMaxFileSizeUserPref()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getVideoMaxFileSizeUserPref");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String video_max_filesize_value = sharedPreferences.getString(PreferenceKeys.getVideoMaxFileSizePreferenceKey(), "0");
        long video_max_filesize = 0;
        try
        {
            video_max_filesize = Integer.parseInt(video_max_filesize_value);
        } catch (NumberFormatException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "failed to parse preference_video_max_filesize value: " + video_max_filesize_value);
            e.printStackTrace();
            video_max_filesize = 0;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "video_max_filesize: " + video_max_filesize);
        return video_max_filesize;
    }

    @Override
    public VideoMaxFileSize getVideoMaxFileSizePref() throws NoFreeStorageException
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getVideoMaxFileSizePref");
        VideoMaxFileSize video_max_filesize = new VideoMaxFileSize();
        video_max_filesize.max_filesize = getVideoMaxFileSizeUserPref();
        video_max_filesize.auto_restart = false;

		/* Also if using internal memory without storage access framework, try to set the max filesize so we don't run out of space.
           This is the only way to avoid the problem where videos become corrupt when run out of space - MediaRecorder doesn't stop on
		   its own, and no error is given!
		   If using SD card, it's not reliable to get the free storage (see https://sourceforge.net/p/opencamera/tickets/153/ ).
		   If using storage access framework, in theory we could check if this was on internal storage, but risk of getting it wrong...
		   so seems safest to leave (the main reason for using SAF is for SD cards, anyway).
		   */
        if (!storageUtils.isUsingSAF())
        {
            String folder_name = storageUtils.getSaveLocation();
            if (MyDebug.LOG)
                Log.d(TAG, "saving to: " + folder_name);
            boolean is_internal = false;
            if (!folder_name.startsWith("/"))
            {
                is_internal = true;
            } else
            {
                // if save folder path is a full path, see if it matches the "external" storage (which actually means "primary", which typically isn't an SD card these days)
                File storage = Environment.getExternalStorageDirectory();
                if (MyDebug.LOG)
                    Log.d(TAG, "compare to: " + storage.getAbsolutePath());
                if (folder_name.startsWith(storage.getAbsolutePath()))
                    is_internal = true;
            }
            if (is_internal)
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "using internal storage");
                long free_memory = main_activity.freeMemory() * 1024 * 1024;
                final long min_free_memory = 50000000; // how much free space to leave after video
                // min_free_filesize is the minimum value to set for max file size:
                //   - no point trying to create a really short video
                //   - too short videos can end up being corrupted
                //   - also with auto-restart, if this is too small we'll end up repeatedly restarting and creating shorter and shorter videos
                final long min_free_filesize = 20000000;
                long available_memory = free_memory - min_free_memory;
                if (test_set_available_memory)
                {
                    available_memory = test_available_memory;
                }
                if (MyDebug.LOG)
                {
                    Log.d(TAG, "free_memory: " + free_memory);
                    Log.d(TAG, "available_memory: " + available_memory);
                }
                if (available_memory > min_free_filesize)
                {
                    if (video_max_filesize.max_filesize == 0 || video_max_filesize.max_filesize > available_memory)
                    {
                        video_max_filesize.max_filesize = available_memory;
                        // still leave auto_restart set to true - because even if we set a max filesize for running out of storage, the video may still hit a maximum limit before hand, if there's a device max limit set (typically ~2GB)
                        if (MyDebug.LOG)
                            Log.d(TAG, "set video_max_filesize to avoid running out of space: " + video_max_filesize);
                    }
                } else
                {
                    if (MyDebug.LOG)
                        Log.e(TAG, "not enough free storage to record video");
                    throw new NoFreeStorageException();
                }
            }
        }

        return video_max_filesize;
    }

    @Override
    public String getPreviewSizePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(PreferenceKeys.getPreviewSizePreferenceKey(), "preference_preview_size_wysiwyg");
    }

    @Override
    public boolean getTouchCapturePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String value = sharedPreferences.getString(PreferenceKeys.getTouchCapturePreferenceKey(), "none");
        return value.equals("single");
    }

    @Override
    public boolean getDoubleTapCapturePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String value = sharedPreferences.getString(PreferenceKeys.getTouchCapturePreferenceKey(), "none");
        return value.equals("double");
    }

    @Override
    public boolean getPausePreviewPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getPausePreviewPreferenceKey(), false);
    }

    @Override
    public boolean getShowToastsPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getShowToastsPreferenceKey(), true);
    }

    @Override
    public boolean getShutterSoundPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getShutterSoundPreferenceKey(), true);
    }

    @Override
    public boolean getStartupFocusPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getStartupFocusPreferenceKey(), true);
    }

    private boolean getAutoStabilisePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean auto_stabilise = sharedPreferences.getBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), false);
        if (auto_stabilise && main_activity.supportsAutoStabilise())
            return true;
        return false;
    }

    @Override
    public int getZoomPref()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getZoomPref: " + zoom_factor);
        return zoom_factor;
    }

    @Override
    public void setZoomPref(int zoom)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "setZoomPref: " + zoom);
        this.zoom_factor = zoom;
    }

    @Override
    public long getExposureTimePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getLong(PreferenceKeys.getExposureTimePreferenceKey(), CameraController.EXPOSURE_TIME_DEFAULT);
    }

    @Override
    public void setExposureTimePref(long exposure_time)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PreferenceKeys.getExposureTimePreferenceKey(), exposure_time);
        editor.apply();
    }

    @Override
    public float getFocusDistancePref()
    {
        return focus_distance;
    }

    @Override
    public void setFocusDistancePref(float focus_distance)
    {
        this.focus_distance = focus_distance;
    }

    @Override
    public boolean isExpoBracketingPref()
    {
        PhotoMode photo_mode = getPhotoMode();
        if (photo_mode == PhotoMode.HDR || photo_mode == PhotoMode.ExpoBracketing)
            return true;
        return false;
    }

    @Override
    public int getExpoBracketingNImagesPref()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getExpoBracketingNImagesPref");
        int n_images = 0;
        PhotoMode photo_mode = getPhotoMode();
        if (photo_mode == PhotoMode.HDR)
        {
            // always set 3 images for HDR
            n_images = 3;
        } else
        {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String n_images_s = sharedPreferences.getString(PreferenceKeys.getExpoBracketingNImagesPreferenceKey(), "3");
            try
            {
                n_images = Integer.parseInt(n_images_s);
            } catch (NumberFormatException exception)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "n_images_s invalid format: " + n_images_s);
                n_images = 3;
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "n_images = " + n_images);
        return n_images;
    }

    @Override
    public double getExpoBracketingStopsPref()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "getExpoBracketingStopsPref");
        double n_stops = 0.0;
        PhotoMode photo_mode = getPhotoMode();
        if (photo_mode == PhotoMode.HDR)
        {
            // always set 2 stops for HDR
            n_stops = 2.0;
        } else
        {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String n_stops_s = sharedPreferences.getString(PreferenceKeys.getExpoBracketingStopsPreferenceKey(), "2");
            try
            {
                n_stops = Double.parseDouble(n_stops_s);
            } catch (NumberFormatException exception)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "n_stops_s invalid format: " + n_stops_s);
                n_stops = 2.0;
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "n_stops = " + n_stops);
        return n_stops;
    }

    public PhotoMode getPhotoMode()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String photo_mode_pref = sharedPreferences.getString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_std");
        boolean hdr = photo_mode_pref.equals("preference_photo_mode_hdr");
        if (hdr && main_activity.supportsHDR())
            return PhotoMode.HDR;
        boolean expo_bracketing = photo_mode_pref.equals("preference_photo_mode_expo_bracketing");
        if (expo_bracketing && main_activity.supportsExpoBracketing())
            return PhotoMode.ExpoBracketing;
        return PhotoMode.Standard;
    }

    @Override
    public boolean useCamera2FakeFlash()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getCamera2FakeFlashPreferenceKey(), false);
    }

    @Override
    public boolean isTestAlwaysFocus()
    {
        if (MyDebug.LOG)
        {
            Log.d(TAG, "isTestAlwaysFocus: " + main_activity.is_test);
        }
        return main_activity.is_test;
    }

    @Override
    public void stoppedVideo(final int video_method, final Uri uri, final String filename)
    {
        if (MyDebug.LOG)
        {
            Log.d(TAG, "stoppedVideo");
            Log.d(TAG, "video_method " + video_method);
            Log.d(TAG, "uri " + uri);
            Log.d(TAG, "filename " + filename);
        }
        boolean done = false;
        if (video_method == VIDEOMETHOD_FILE)
        {
            if (filename != null)
            {
                //TODO call upload file
                ChangeActivity.sendToPreview(main_activity.getApplicationContext(),
                        filename,Constant.TYPE_VIDEO,true);
                ((CameraActivity)getContext()).finish();
                File file = new File(filename);

                storageUtils.broadcastFile(file, false, true, true);
                done = true;
            }
        } else
        {
            if (uri != null)
            {
                // see note in onPictureTaken() for where we call broadcastFile for SAF photos
                File real_file = storageUtils.getFileFromDocumentUriSAF(uri);
                if (MyDebug.LOG)
                    Log.d(TAG, "real_file: " + real_file);
                if (real_file != null)
                {
                    storageUtils.broadcastFile(real_file, false, true, true);
                    main_activity.test_last_saved_image = real_file.getAbsolutePath();
                } else
                {
                    // announce the SAF Uri
                    storageUtils.announceUri(uri, false, true);
                }
                done = true;
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "done? " + done);

        String action = main_activity.getIntent().getAction();
        if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action))
        {
            if (done && video_method == VIDEOMETHOD_FILE)
            {
                // do nothing here - we end the activity from storageUtils.broadcastFile after the file has been scanned, as it seems caller apps seem to prefer the content:// Uri rather than one based on a File
            } else
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "from video capture intent");
                Intent output = null;
                if (done)
                {
                    // may need to pass back the Uri we saved to, if the calling application didn't specify a Uri
                    // set note above for VIDEOMETHOD_FILE
                    // n.b., currently this code is not used, as we always switch to VIDEOMETHOD_FILE if the calling application didn't specify a Uri, but I've left this here for possible future behaviour
                    if (video_method == VIDEOMETHOD_SAF)
                    {
                        output = new Intent();
                        output.setData(uri);
                        if (MyDebug.LOG)
                            Log.d(TAG, "pass back output uri [saf]: " + output.getData());
                    }
                }
                main_activity.setResult(done ? Activity.RESULT_OK : Activity.RESULT_CANCELED, output);
                main_activity.finish();
            }
        } else if (done)
        {
            // create thumbnail
            long time_s = System.currentTimeMillis();
            Bitmap thumbnail = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try
            {
                if (video_method == VIDEOMETHOD_FILE)
                {
                    File file = new File(filename);
                    retriever.setDataSource(file.getPath());
                } else
                {
                    ParcelFileDescriptor pfd_saf = getContext().getContentResolver().openFileDescriptor(uri, "r");
                    retriever.setDataSource(pfd_saf.getFileDescriptor());
                }
            } catch (FileNotFoundException e)
            {
                // video file wasn't saved?
                Log.d(TAG, "failed to find thumbnail");
                e.printStackTrace();
            } catch (IllegalArgumentException e)
            {
                // corrupt video file?
                Log.d(TAG, "failed to find thumbnail");
                e.printStackTrace();
            } catch (RuntimeException e)
            {
                // corrupt video file?
                Log.d(TAG, "failed to find thumbnail");
                e.printStackTrace();
            } finally
            {
                try
                {
                    retriever.release();
                } catch (RuntimeException ex)
                {
                    // ignore
                }
            }
        }
    }

    @Override
    public void cameraSetup()
    {
        main_activity.cameraSetup();
        drawPreview.clearContinuousFocusMove();
    }

    @Override
    public void onContinuousFocusMove(boolean start)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onContinuousFocusMove: " + start);
        drawPreview.onContinuousFocusMove(start);
    }

    @Override
    public void touchEvent(MotionEvent event)
    {
        if (main_activity.usingKitKatImmersiveMode())
        {
            main_activity.setImmersiveMode(false);
        }
    }

    @Override
    public void startingVideo()
    {
        ImageView view = (ImageView) main_activity.findViewById(R.id.take_photo);
        view.setImageResource(R.drawable.click_video);
        view.setContentDescription(getContext().getResources().getString(R.string.stop_video));
    }

    @Override
    public void stoppingVideo()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "stoppingVideo()");
        ImageView view = (ImageView) main_activity.findViewById(R.id.take_photo);
        view.setImageResource(R.drawable.click);
    }

    @Override
    public void onVideoInfo(int what, int extra)
    {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
        {
            int message_id = 0;
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "max duration reached");
                message_id = R.string.video_max_duration;
            } else
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "max filesize reached");
                message_id = R.string.video_max_filesize;
            }
            if (message_id != 0)
                main_activity.getPreview().showToast(null, message_id);
            // in versions 1.24 and 1.24, there was a bug where we had "info_" for onVideoError and "error_" for onVideoInfo!
            // fixed in 1.25; also was correct for 1.23 and earlier
            String debug_value = "info_" + what + "_" + extra;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("last_video_error", debug_value);
            editor.apply();
        }
    }

    @Override
    public void onFailedStartPreview()
    {
        main_activity.getPreview().showToast(null, R.string.failed_to_start_camera_preview);
    }

    @Override
    public void onPhotoError()
    {
        main_activity.getPreview().showToast(null, R.string.failed_to_take_picture);
    }

    @Override
    public void onVideoError(int what, int extra)
    {
        if (MyDebug.LOG)
        {
            Log.d(TAG, "onVideoError: " + what + " extra: " + extra);
        }
        int message_id = R.string.video_error_unknown;
        if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED)
        {
            if (MyDebug.LOG)
                Log.d(TAG, "error: server died");
            message_id = R.string.video_error_server_died;
        }
        main_activity.getPreview().showToast(null, message_id);
        // in versions 1.24 and 1.24, there was a bug where we had "info_" for onVideoError and "error_" for onVideoInfo!
        // fixed in 1.25; also was correct for 1.23 and earlier
        String debug_value = "error_" + what + "_" + extra;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("last_video_error", debug_value);
        editor.apply();
    }

    @Override
    public void onVideoRecordStartError(CamcorderProfile profile)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onVideoRecordStartError");
        String error_message = "";
        String features = main_activity.getPreview().getErrorFeatures(profile);
        if (features.length() > 0)
        {
            error_message = getContext().getResources().getString(R.string.sorry) + ", " + features + " " + getContext().getResources().getString(R.string.not_supported);
        } else
        {
            error_message = getContext().getResources().getString(R.string.failed_to_record_video);
        }
        main_activity.getPreview().showToast(null, error_message);
        ImageView view = (ImageView) main_activity.findViewById(R.id.take_photo);
        view.setImageResource(R.drawable.click);
    }

    @Override
    public void onVideoRecordStopError(CamcorderProfile profile)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onVideoRecordStopError");
        //main_activity.getPreview().showToast(null, R.string.failed_to_record_video);
        String features = main_activity.getPreview().getErrorFeatures(profile);
        String error_message = getContext().getResources().getString(R.string.video_may_be_corrupted);
        if (features.length() > 0)
        {
            error_message += ", " + features + " " + getContext().getResources().getString(R.string.not_supported);
        }
        main_activity.getPreview().showToast(null, error_message);
    }

    @Override
    public void onFailedReconnectError()
    {
        main_activity.getPreview().showToast(null, R.string.failed_to_reconnect_camera);
    }

    @Override
    public void onFailedCreateVideoFileError()
    {
        main_activity.getPreview().showToast(null, R.string.failed_to_save_video);
        ImageView view = (ImageView) main_activity.findViewById(R.id.take_photo);
        view.setImageResource(R.drawable.click);
    }

    @Override
    public void hasPausedPreview(boolean paused)
    {
        View shareControls = main_activity.findViewById(R.id.share_controls);
        View cameraControls = main_activity.findViewById(R.id.camera_control);
        if (paused)
        {
            shareControls.setVisibility(View.VISIBLE);
            cameraControls.setVisibility(View.GONE);
        } else
        {
            cameraControls.setVisibility(View.VISIBLE);
            shareControls.setVisibility(View.GONE);
            this.clearLastImages();
        }
    }

    @Override
    public void cameraInOperation(boolean in_operation)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "cameraInOperation: " + in_operation);
        if (!in_operation && used_front_screen_flash)
        {
            main_activity.setBrightnessForCamera(false); // ensure screen brightness matches user preference, after using front screen flash
            used_front_screen_flash = false;
        }
        drawPreview.cameraInOperation(in_operation);
        main_activity.getMainUI().showGUI(!in_operation);
    }

    @Override
    public void turnFrontScreenFlashOn()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "turnFrontScreenFlashOn");
        used_front_screen_flash = true;
        main_activity.setBrightnessForCamera(true); // ensure we have max screen brightness, even if user preference not set for max brightness
        drawPreview.turnFrontScreenFlashOn();
    }

    @Override
    public void onPictureCompleted()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onPictureCompleted");
        // call this, so that if pause-preview-after-taking-photo option is set, we remove the "taking photo" border indicator straight away
        drawPreview.cameraInOperation(false);
    }

    @Override
    public void cameraClosed()
    {
        drawPreview.clearContinuousFocusMove();
    }

    @Override
    public void layoutUI()
    {
        main_activity.getMainUI().layoutUI();
    }

    @Override
    public void multitouchZoom(int new_zoom)
    {
        main_activity.getMainUI().setSeekbarZoom();
    }

    @Override
    public void setFocusPref(String focus_value, boolean is_video)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getFocusPreferenceKey(cameraId, is_video), focus_value);
        editor.apply();
        // focus may be updated by preview (e.g., when switching to/from video mode)
        final int visibility = main_activity.getPreview().getCurrentFocusValue() != null && main_activity.getPreview().getCurrentFocusValue().equals("focus_mode_manual2") ? View.VISIBLE : View.INVISIBLE;
        View focusSeekBar = (SeekBar) main_activity.findViewById(R.id.focus_seekbar);
        focusSeekBar.setVisibility(visibility);
    }

    @Override
    public void clearSceneModePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.getSceneModePreferenceKey());
        editor.apply();
    }

    @Override
    public void clearColorEffectPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.getColorEffectPreferenceKey());
        editor.apply();
    }

    @Override
    public void clearWhiteBalancePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.getWhiteBalancePreferenceKey());
        editor.apply();
    }

    @Override
    public void clearISOPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.getISOPreferenceKey());
        editor.apply();
    }

    @Override
    public void clearExposureCompensationPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.getExposurePreferenceKey());
        editor.apply();
    }

    @Override
    public void setCameraResolutionPref(int width, int height)
    {
        String resolution_value = width + " " + height;
        if (MyDebug.LOG)
        {
            Log.d(TAG, "save new resolution_value: " + resolution_value);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getResolutionPreferenceKey(cameraId), resolution_value);
        editor.apply();
    }

    @Override
    public void requestCameraPermission()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "requestCameraPermission");
        main_activity.requestCameraPermission();
    }

    @Override
    public void requestStoragePermission()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "requestStoragePermission");
        main_activity.requestStoragePermission();
    }

    @Override
    public void requestRecordAudioPermission()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "requestRecordAudioPermission");
        main_activity.requestRecordAudioPermission();
    }

    @Override
    public void clearExposureTimePref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.getExposureTimePreferenceKey());
        editor.apply();
    }

    @Override
    public void onDrawPreview(Canvas canvas)
    {
        drawPreview.onDrawPreview(canvas);
    }

    public void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y)
    {
        drawTextWithBackground(canvas, paint, text, foreground, background, location_x, location_y, false);
    }

    public void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y, boolean align_top)
    {
        drawTextWithBackground(canvas, paint, text, foreground, background, location_x, location_y, align_top, null, true);
    }

    public void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y, boolean align_top, String ybounds_text, boolean shadow)
    {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(background);
        paint.setAlpha(64);
        int alt_height = 0;
        if (ybounds_text != null)
        {
            paint.getTextBounds(ybounds_text, 0, ybounds_text.length(), text_bounds);
            alt_height = text_bounds.bottom - text_bounds.top;
        }
        paint.getTextBounds(text, 0, text.length(), text_bounds);
        if (ybounds_text != null)
        {
            text_bounds.bottom = text_bounds.top + alt_height;
        }
        final int padding = (int) (2 * scale + 0.5f); // convert dps to pixels
        if (paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER)
        {
            float width = paint.measureText(text); // n.b., need to use measureText rather than getTextBounds here
			/*if( MyDebug.LOG )
				Log.d(TAG, "width: " + width);*/
            if (paint.getTextAlign() == Paint.Align.CENTER)
                width /= 2.0f;
            text_bounds.left -= width;
            text_bounds.right -= width;
        }
		/*if( MyDebug.LOG )
			Log.d(TAG, "text_bounds left-right: " + text_bounds.left + " , " + text_bounds.right);*/
        text_bounds.left += location_x - padding;
        text_bounds.right += location_x + padding;
        if (align_top)
        {
            int height = text_bounds.bottom - text_bounds.top + 2 * padding;
            // unclear why we need the offset of -1, but need this to align properly on Galaxy Nexus at least
            int y_diff = -text_bounds.top + padding - 1;
            text_bounds.top = location_y - 1;
            text_bounds.bottom = text_bounds.top + height;
            location_y += y_diff;
        } else
        {
            text_bounds.top += location_y - padding;
            text_bounds.bottom += location_y + padding;
        }
        if (shadow)
        {
            canvas.drawRect(text_bounds, paint);
        }
        paint.setColor(foreground);
        canvas.drawText(text, location_x, location_y, paint);
    }

    private boolean saveInBackground(boolean image_capture_intent)
    {
        boolean do_in_background = true;
        if (image_capture_intent)
            do_in_background = false;
        else if (getPausePreviewPref())
            do_in_background = false;
        return do_in_background;
    }

    private boolean isImageCaptureIntent()
    {
        boolean image_capture_intent = false;
        String action = main_activity.getIntent().getAction();
        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action) || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action))
        {
            if (MyDebug.LOG)
                Log.d(TAG, "from image capture intent");
            image_capture_intent = true;
        }
        return image_capture_intent;
    }

    private boolean saveImage(boolean is_hdr, boolean save_expo, List<byte[]> images, Date current_date)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "saveImage");

        System.gc();

        boolean image_capture_intent = isImageCaptureIntent();
        Uri image_capture_intent_uri = null;
        if (image_capture_intent)
        {
            if (MyDebug.LOG)
                Log.d(TAG, "from image capture intent");
            Bundle myExtras = main_activity.getIntent().getExtras();
            if (myExtras != null)
            {
                image_capture_intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
                if (MyDebug.LOG)
                    Log.d(TAG, "save to: " + image_capture_intent_uri);
            }
        }

        boolean using_camera2 = main_activity.getPreview().usingCamera2API();
        int image_quality = getImageQualityPref();
        boolean do_auto_stabilise = getAutoStabilisePref() && main_activity.getPreview().hasLevelAngle();
        double level_angle = do_auto_stabilise ? main_activity.getPreview().getLevelAngle() : 0.0;
        if (do_auto_stabilise && main_activity.test_have_angle)
            level_angle = main_activity.test_angle;
        if (do_auto_stabilise && main_activity.test_low_memory)
            level_angle = 45.0;
        // I have received crashes where camera_controller was null - could perhaps happen if this thread was running just as the camera is closing?
        boolean is_front_facing = main_activity.getPreview().getCameraController() != null && main_activity.getPreview().getCameraController().isFrontFacing();

        boolean do_in_background = saveInBackground(image_capture_intent);

        int sample_factor = 1;

        if (MyDebug.LOG)
            Log.d(TAG, "sample_factor: " + sample_factor);

        boolean success = imageSaver.saveImageJpeg(do_in_background, is_hdr, save_expo, images,
                image_capture_intent, image_capture_intent_uri,
                using_camera2, image_quality,
                do_auto_stabilise, level_angle,
                is_front_facing,
                current_date,
                null, null, 0, 0, null, null, null, null,
                false, null, false, 0.0,
                sample_factor);

        if (MyDebug.LOG)
            Log.d(TAG, "saveImage complete, success: " + success);

        return success;
    }

    @Override
    public boolean onPictureTaken(byte[] data, Date current_date)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onPictureTaken");

        List<byte[]> images = new ArrayList<>();
        images.add(data);

        boolean success = saveImage(false, false, images, current_date);

        if (MyDebug.LOG)
            Log.d(TAG, "onPictureTaken complete, success: " + success);

        return success;
    }

    void addLastImage(File file, boolean share)
    {
        if (MyDebug.LOG)
        {
            Log.d(TAG, "addLastImage: " + file);
            Log.d(TAG, "share?: " + share);
        }
        last_images_saf = false;
        LastImage last_image = new LastImage(file.getAbsolutePath(), share);
        last_images.add(last_image);
    }

    void addLastImageSAF(Uri uri, boolean share)
    {
        if (MyDebug.LOG)
        {
            Log.d(TAG, "addLastImageSAF: " + uri);
            Log.d(TAG, "share?: " + share);
        }
        last_images_saf = true;
        LastImage last_image = new LastImage(uri, share);
        last_images.add(last_image);
    }

    void clearLastImages()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "clearLastImages");
        last_images_saf = false;
        last_images.clear();
    }

    void shareLastImage()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "shareLastImage");
        Preview preview = main_activity.getPreview();
        if (preview.isPreviewPaused())
        {
            LastImage share_image = null;
            for (int i = 0; i < last_images.size() && share_image == null; i++)
            {
                LastImage last_image = last_images.get(i);
                if (last_image.share)
                {
                    share_image = last_image;
                }
            }
            if (share_image != null)
            {
                String path = ImageOperations.getPath(main_activity,share_image.uri);
                clearLastImages();
                ChangeActivity.sendToPreview(main_activity.getApplicationContext(),path,Constant.TYPE_PICTURE,true);
                ((CameraActivity)getContext()).finish();
            }
            preview.startCameraPreview();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void trashImage(boolean image_saf, Uri image_uri, String image_name)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "trashImage");
        Preview preview = main_activity.getPreview();
        if (image_saf && image_uri != null)
        {
            if (MyDebug.LOG)
                Log.d(TAG, "Delete: " + image_uri);
            File file = storageUtils.getFileFromDocumentUriSAF(image_uri); // need to get file before deleting it, as fileFromDocumentUriSAF may depend on the file still existing
            if (!DocumentsContract.deleteDocument(main_activity.getContentResolver(), image_uri))
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to delete " + image_uri);
            } else
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "successfully deleted " + image_uri);
                preview.showToast(null, R.string.photo_deleted);
                if (file != null)
                {
                    // SAF doesn't broadcast when deleting them
                    storageUtils.broadcastFile(file, false, false, true);
                }
            }
        } else if (image_name != null)
        {
            if (MyDebug.LOG)
                Log.d(TAG, "Delete: " + image_name);
            File file = new File(image_name);
            if (!file.delete())
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to delete " + image_name);
            } else
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "successfully deleted " + image_name);
                preview.showToast(null, R.string.photo_deleted);
                storageUtils.broadcastFile(file, false, false, true);
            }
        }
    }

    void trashLastImage()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "trashImage");
        Preview preview = main_activity.getPreview();
        if (preview.isPreviewPaused())
        {
            for (int i = 0; i < last_images.size(); i++)
            {
                LastImage last_image = last_images.get(i);
                trashImage(last_images_saf, last_image.uri, last_image.name);
            }
            clearLastImages();
            preview.startCameraPreview();
        }
        // Calling updateGalleryIcon() immediately has problem that it still returns the latest image that we've just deleted!
        // But works okay if we call after a delay. 100ms works fine on Nexus 7 and Galaxy Nexus, but set to 500 just to be safe.

    }

    public static enum PhotoMode
    {
        Standard,
        HDR,
        ExpoBracketing
    }

    /**
     * This class keeps track of the images saved in this batch, for use with Pause Preview option, so we can share or trash images.
     */
    private static class LastImage
    {
        public boolean share = false; // one of the images in the list should have share set to true, to indicate which image to share
        public String name = null;
        public Uri uri = null;

        LastImage(Uri uri, boolean share)
        {
            this.name = null;
            this.uri = uri;
            this.share = share;
        }

        LastImage(String filename, boolean share)
        {
            this.name = filename;
            this.uri = Uri.parse("file://" + this.name);
            this.share = share;
        }
    }
}
