package com.liveunite.opencamera.UI;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.liveunite.R;
import com.liveunite.opencamera.CameraActivity;
import com.liveunite.opencamera.MyDebug;
import com.liveunite.opencamera.PreferenceKeys;

/**
 * This contains functionality related to the main UI.
 */
public class MainUI
{
    private static final String TAG = "MainUI";

    private CameraActivity main_activity = null;

    private int current_orientation = 0;
    private boolean ui_placement_right = true;

    private boolean immersive_mode = false;
    private boolean show_gui = true; // result of call to showGUI() - false means a "reduced" GUI is displayed, whilst taking photo or video

    public MainUI(CameraActivity main_activity)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "MainUI");
        this.main_activity = main_activity;

        this.setIcon(R.id.share_tick);
        this.setIcon(R.id.delete_cross);

    }

    private void setIcon(int id)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "setIcon: " + id);
        ImageView button = (ImageView) main_activity.findViewById(id);
        button.setBackgroundColor(Color.argb(63, 63, 63, 63)); // n.b., rgb color seems to be ignored for Android 6 onwards, but still relevant for older versions
    }

    /**
     * Similar view.setRotation(ui_rotation), but achieves this via an animation.
     */
    private void setViewRotation(View view, float ui_rotation)
    {
        //view.setRotation(ui_rotation);
        float rotate_by = ui_rotation - view.getRotation();
        if (rotate_by > 181.0f)
            rotate_by -= 360.0f;
        else if (rotate_by < -181.0f)
            rotate_by += 360.0f;
        // view.animate() modifies the view's rotation attribute, so it ends up equivalent to view.setRotation()
        // we use rotationBy() instead of rotation(), so we get the minimal rotation for clockwise vs anti-clockwise
        view.animate().rotationBy(rotate_by).setDuration(100).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    public void layoutUI()
    {
        long debug_time = 0;
        if (MyDebug.LOG)
        {
            Log.d(TAG, "layoutUI");
            debug_time = System.currentTimeMillis();
        }
        //this.preview.updateUIPlacement();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
        String ui_placement = sharedPreferences.getString(PreferenceKeys.getUIPlacementPreferenceKey(), "ui_right");
        // we cache the preference_ui_placement to save having to check it in the draw() method
        this.ui_placement_right = ui_placement.equals("ui_right");
        if (MyDebug.LOG)
            Log.d(TAG, "ui_placement: " + ui_placement);
        // new code for orientation fixed to landscape
        // the display orientation should be locked to landscape, but how many degrees is that?
        int rotation = main_activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        // getRotation is anti-clockwise, but current_orientation is clockwise, so we add rather than subtract
        // relative_orientation is clockwise from landscape-left
        //int relative_orientation = (current_orientation + 360 - degrees) % 360;
        int relative_orientation = (current_orientation + degrees) % 360;
        if (MyDebug.LOG)
        {
            Log.d(TAG, "    current_orientation = " + current_orientation);
            Log.d(TAG, "    degrees = " + degrees);
            Log.d(TAG, "    relative_orientation = " + relative_orientation);
        }
        int ui_rotation = (360 - relative_orientation) % 360;
        main_activity.getPreview().setUIRotation(ui_rotation);

        {
            View view = main_activity.findViewById(R.id.switch_flash);
            setViewRotation(view, ui_rotation);

            view = main_activity.findViewById(R.id.switch_gallery);
            setViewRotation(view, ui_rotation);

            view = main_activity.findViewById(R.id.switch_camera);
            setViewRotation(view, ui_rotation);

            view = main_activity.findViewById(R.id.take_photo);
            setViewRotation(view, ui_rotation);

            view = main_activity.findViewById(R.id.moments);
            setViewRotation(view,ui_rotation);

            view = main_activity.findViewById(R.id.share_tick);
            setViewRotation(view,ui_rotation);

            view = main_activity.findViewById(R.id.delete_cross);
            setViewRotation(view,ui_rotation);
        }

        if (MyDebug.LOG)
        {
            Log.d(TAG, "layoutUI: total time: " + (System.currentTimeMillis() - debug_time));
        }
    }

    /**
     * Set content description for switch camera button.
     */
    public void setSwitchCameraContentDescription()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "setSwitchCameraContentDescription()");
        if (main_activity.getPreview() != null && main_activity.getPreview().canSwitchCamera())
        {
            ImageView view = (ImageView) main_activity.findViewById(R.id.switch_camera);
            int content_description = 0;
            int cameraId = main_activity.getNextCameraId();
            if (main_activity.getPreview().getCameraControllerManager().isFrontFacing(cameraId))
            {
                content_description = R.string.switch_to_front_camera;
            } else
            {
                content_description = R.string.switch_to_back_camera;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "content_description: " + main_activity.getResources().getString(content_description));
            view.setContentDescription(main_activity.getResources().getString(content_description));
        }
    }

    public boolean getUIPlacementRight()
    {
        return this.ui_placement_right;
    }

    public void onOrientationChanged(int orientation)
    {
        /*if( MyDebug.LOG ) {
            Log.d(TAG, "onOrientationChanged()");
			Log.d(TAG, "orientation: " + orientation);
			Log.d(TAG, "current_orientation: " + current_orientation);
		}*/
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return;
        int diff = Math.abs(orientation - current_orientation);
        if (diff > 180)
            diff = 360 - diff;
        // only change orientation when sufficiently changed
        if (diff > 60)
        {
            orientation = (orientation + 45) / 90 * 90;
            orientation = orientation % 360;
            if (orientation != current_orientation)
            {
                this.current_orientation = orientation;
                if (MyDebug.LOG)
                {
                    Log.d(TAG, "current_orientation is now: " + current_orientation);
                }
                layoutUI();
            }
        }
    }

    public void setImmersiveMode(final boolean immersive_mode)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "setImmersiveMode: " + immersive_mode);
        this.immersive_mode = immersive_mode;
        main_activity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
                // if going into immersive mode, the we should set GONE the ones that are set GONE in showGUI(false)
                //final int visibility_gone = immersive_mode ? View.GONE : View.VISIBLE;
                final int visibility = immersive_mode ? View.GONE : View.VISIBLE;
                if (MyDebug.LOG)
                    Log.d(TAG, "setImmersiveMode: set visibility: " + visibility);
                // n.b., don't hide share and trash buttons, as they require immediate user input for us to continue
                View switchCameraButton = (View) main_activity.findViewById(R.id.switch_camera);
                View switchGalleryButton = (View) main_activity.findViewById(R.id.switch_gallery);
                View switchCameraBtn = (View) main_activity.findViewById(R.id.switch_flash);
                if (main_activity.getPreview().getCameraControllerManager().getNumberOfCameras() > 1)
                    switchCameraButton.setVisibility(visibility);
                switchGalleryButton.setVisibility(visibility);

                switchCameraBtn.setVisibility(visibility);
                if (MyDebug.LOG)
                {
                    Log.d(TAG, "has_zoom: " + main_activity.getPreview().supportsZoom());
                }
                String pref_immersive_mode = sharedPreferences.getString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");
                if (pref_immersive_mode.equals("immersive_mode_everything"))
                {
                    View takePhotoButton = (View) main_activity.findViewById(R.id.take_photo);
                    takePhotoButton.setVisibility(visibility);
                }
                if (!immersive_mode)
                {
                    // make sure the GUI is set up as expected
                    showGUI(show_gui);
                }
            }
        });
    }

    public boolean inImmersiveMode()
    {
        return immersive_mode;
    }

    public void showGUI(final boolean show)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "showGUI: " + show);
        this.show_gui = show;
        if (inImmersiveMode())
            return;
        if (show && main_activity.usingKitKatImmersiveMode())
        {
            // call to reset the timer
        }
        main_activity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                //final int visibility = show ? View.VISIBLE : View.INVISIBLE;
                View switchCameraButton = (View) main_activity.findViewById(R.id.switch_camera);
                View switchGalleryButton = (View) main_activity.findViewById(R.id.switch_gallery);
                View momentsButton = (View) main_activity.findViewById(R.id.moments);

                if (main_activity.getPreview().getCameraControllerManager().getNumberOfCameras() > 1)
                    switchCameraButton.setEnabled(show);

                //if camera in operation disable gallery and moments and switch camera btn
                switchCameraButton.setEnabled(show);
                switchGalleryButton.setEnabled(show); // still allow switch video when recording video
                momentsButton.setEnabled(show);
            }
        });
    }

    public void setSeekbarZoom()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "setSeekbarZoom");
        SeekBar zoomSeekBar = (SeekBar) main_activity.findViewById(R.id.zoom_seekbar);
        zoomSeekBar.setProgress(main_activity.getPreview().getMaxZoom() - main_activity.getPreview().getCameraController().getZoom());
        if (MyDebug.LOG)
            Log.d(TAG, "progress is now: " + zoomSeekBar.getProgress());
    }

    public void changeSeekbar(int seekBarId, int change)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "changeSeekbar: " + change);
        SeekBar seekBar = (SeekBar) main_activity.findViewById(seekBarId);
        int value = seekBar.getProgress();
        int new_value = value + change;
        if (new_value < 0)
            new_value = 0;
        else if (new_value > seekBar.getMax())
            new_value = seekBar.getMax();
        if (MyDebug.LOG)
        {
            Log.d(TAG, "value: " + value);
            Log.d(TAG, "new_value: " + new_value);
            Log.d(TAG, "max: " + seekBar.getMax());
        }
        if (new_value != value)
        {
            seekBar.setProgress(new_value);
        }
    }
}
