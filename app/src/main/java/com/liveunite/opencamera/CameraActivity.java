package com.liveunite.opencamera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.liveunite.R;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ImageOperations;
import com.liveunite.opencamera.CameraController.CameraController;
import com.liveunite.opencamera.CameraController.CameraControllerManager2;
import com.liveunite.opencamera.Preview.Preview;
import com.liveunite.opencamera.UI.MainUI;

import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The main Activity for Open Camera.
 */
public class CameraActivity extends Activity implements Handler.Callback {
    private static final String TAG = "CameraActivity";
    final private int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    final private int MY_PERMISSIONS_REQUEST_STORAGE = 1;
    final private int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2;
    // for testing:
    public boolean is_test = false; // whether called from OpenCamera.test testing
    public boolean test_low_memory = false;
    public boolean test_have_angle = false;
    public float test_angle = 0.0f;
    public String test_last_saved_image = null;
    private SensorManager mSensorManager = null;
    private Sensor mSensorAccelerometer = null;
    private Sensor mSensorMagnetic = null;
    private MainUI mainUI = null;
    private MyApplicationInterface applicationInterface = null;
    private Preview preview = null;
    private OrientationEventListener orientationEventListener = null;
    private boolean supports_auto_stabilise = false;
    private boolean supports_force_video_4k = false;
    private boolean supports_camera2 = false;
    private boolean saf_dialog_from_preferences = false; // if a SAF dialog is opened, this records whether we opened it from the Preferences
    private boolean camera_in_background = false; // whether the camera is covered by a fragment/dialog (such as settings or folder picker)

    //private boolean ui_placement_right = true;
    private Map<Integer, Bitmap> preloaded_bitmap_resources = new Hashtable<Integer, Bitmap>();
    private SoundPool sound_pool = null;
    private SparseIntArray sound_ids = null;
    private ToastBoxer switch_video_toast = new ToastBoxer();
    private ToastBoxer changed_auto_stabilise_toast = new ToastBoxer();
    private boolean block_startup_toast = false; // used when returning from Settings/Popup - if we're displaying a toast anyway, don't want to display the info toast too
    private boolean keydown_volume_up = false;
    private boolean keydown_volume_down = false;

    Handler mVideoLimitHandler = new Handler(this);

    String[] icons, values;
    int currentFlashModeIndex = 1;
    List<String> supported_flash_values;

    private SensorEventListener accelerometerListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            preview.onAccelerometerSensorChanged(event);
        }
    };
    private SensorEventListener magneticListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            preview.onMagneticSensorChanged(event);
        }
    };


    private static double seekbarScaling(double frac) {
        // For various seekbars, we want to use a non-linear scaling, so user has more control over smaller values
        double scaling = (Math.pow(100.0, frac) - 1.0) / 99.0;
        return scaling;
    }

    private static double seekbarScalingInverse(double scaling) {
        double frac = Math.log(99.0 * scaling + 1.0) / Math.log(100.0);
        return frac;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "onCreate");
            debug_time = System.currentTimeMillis();
        }
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false); // initialise any unset preferences to their default values
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after setting default preference values: " + (System.currentTimeMillis() - debug_time));

        if (getIntent() != null && getIntent().getExtras() != null) {
            // whether called from testing
            is_test = getIntent().getExtras().getBoolean("test_project");
            if (MyDebug.LOG)
                Log.d(TAG, "is_test: " + is_test);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // determine whether we should support "auto stabilise" feature
        // risk of running out of memory on lower end devices, due to manipulation of large bitmaps
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (MyDebug.LOG) {
            Log.d(TAG, "standard max memory = " + activityManager.getMemoryClass() + "MB");
            Log.d(TAG, "large max memory = " + activityManager.getLargeMemoryClass() + "MB");
        }
        //if( activityManager.getMemoryClass() >= 128 ) { // test
        if (activityManager.getLargeMemoryClass() >= 128) {
            supports_auto_stabilise = true;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "supports_auto_stabilise? " + supports_auto_stabilise);

        // hack to rule out phones unlikely to have 4K video, so no point even offering the option!
        // both S5 and Note 3 have 128MB standard and 512MB large heap (tested via Samsung RTL), as does Galaxy K Zoom
        // also added the check for having 128MB standard heap, to support modded LG G2, which has 128MB standard, 256MB large - see https://sourceforge.net/p/opencamera/tickets/9/
        if (activityManager.getMemoryClass() >= 128 || activityManager.getLargeMemoryClass() >= 512) {
            supports_force_video_4k = true;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "supports_force_video_4k? " + supports_force_video_4k);

        // set up components
        mainUI = new MainUI(this);
        applicationInterface = new MyApplicationInterface(this, savedInstanceState);
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after creating application interface: " + (System.currentTimeMillis() - debug_time));

        // determine whether we support Camera2 API
        initCamera2Support();

        // set up window flags for normal operation
        setWindowFlagsForCamera();
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after setting window flags: " + (System.currentTimeMillis() - debug_time));

        // set up sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // accelerometer sensor (for device orientation)
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "found accelerometer");
            mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "no support for accelerometer");
        }
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after creating accelerometer sensor: " + (System.currentTimeMillis() - debug_time));

        // magnetic sensor (for compass direction)
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "found magnetic sensor");
            mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "no support for magnetic sensor");
        }
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after creating magnetic sensor: " + (System.currentTimeMillis() - debug_time));

        // set up the camera and its preview
        preview = new Preview(applicationInterface, savedInstanceState, ((ViewGroup) this.findViewById(R.id.preview)));
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after creating preview: " + (System.currentTimeMillis() - debug_time));

        // initialise on-screen button visibility
        View switchCameraButton = (View) findViewById(R.id.switch_camera);
        switchCameraButton.setEnabled(preview.getCameraControllerManager().getNumberOfCameras() > 1);
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after setting button visibility: " + (System.currentTimeMillis() - debug_time));

        // listen for orientation event change
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                CameraActivity.this.mainUI.onOrientationChanged(orientation);
            }
        };
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after setting orientation event listener: " + (System.currentTimeMillis() - debug_time));

        // set up listener to handle immersive mode options
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if (!false)
                            return;
                        if (MyDebug.LOG)
                            Log.d(TAG, "onSystemUiVisibilityChange: " + visibility);
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "system bars now visible");
                            // The system bars are visible. Make any desired
                            // adjustments to your UI, such as showing the action bar or
                            // other navigational controls.
                            mainUI.setImmersiveMode(false);
                        } else {
                            if (MyDebug.LOG)
                                Log.d(TAG, "system bars now NOT visible");
                            // The system bars are NOT visible. Make any desired
                            // adjustments to your UI, such as hiding the action bar or
                            // other navigational controls.
                            mainUI.setImmersiveMode(true);
                        }
                    }
                });
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: time after setting immersive mode listener: " + (System.currentTimeMillis() - debug_time));

        // show "about" dialog for first time use; also set some per-device defaults
        boolean has_done_first_time = sharedPreferences.contains(PreferenceKeys.getFirstTimePreferenceKey());
        if (!has_done_first_time) {
            boolean is_samsung = Build.MANUFACTURER.toLowerCase(Locale.US).contains("samsung");
            if (MyDebug.LOG) {
                Log.d(TAG, "running for first time");
                Log.d(TAG, "is_samsung? " + is_samsung);
            }
            if (is_samsung) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(PreferenceKeys.getCamera2FakeFlashPreferenceKey(), true);
                editor.apply();
            }
        }
        if (!has_done_first_time && !is_test) {

            /*AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(R.string.app_name);
            alertDialog.setMessage(R.string.intro_text);
            alertDialog.setPositiveButton(R.string.intro_ok, null);
            alertDialog.show();
            */
            setFirstTimeFlag();
        }

        icons = getResources().getStringArray(R.array.flash_icons);
        values = getResources().getStringArray(R.array.flash_values);

        // load icons
         preloadIcons(R.array.flash_icons);
       preloadIcons(R.array.focus_mode_icons);

        initControl();

        if (preview != null) {
            supported_flash_values = preview.getSupportedFlashValues();
            //preview.updateFlash("flash_auto");
        }

        if (MyDebug.LOG)
            Log.d(TAG, "onCreate: total time for Activity startup: " + (System.currentTimeMillis() - debug_time));
    }

    void initControl() {
        ImageView click = (ImageView) findViewById(R.id.take_photo);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Log.e("ERR","OnClick");
                if (preview.isVideo() && !preview.isVideoRecording()) {
                    //something went wrong while recording
                    //switch to normal
                    preview.switchVideo(false);
                }
                if (preview.isVideoRecording()) {
                    stopVideoRecording();
                } else {
                    takePicture();
                }
            }
        });

       /* click.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(OpenCameraType.getInstance().isOpenPostCamera())
                {

                    if ( !preview.isVideoRecording())
                    {
                        startVideoRecording();
                        return true;
                    }
                    else if ( preview.isVideoRecording())
                    {
                        stopVideoRecording();
                        return true;
                    }
                }
                else
                {
                    Toast.makeText(CameraActivity.this, "Recording is not available for profile picture", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });*/

      /*  click.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                if(OpenCameraType.getInstance().isOpenPostCamera())
                {
                    long elapsed = motionEvent.getEventTime() - motionEvent.getDownTime();
                    if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && elapsed > 700 && !preview.isVideoRecording())
                    {
                        startVideoRecording();
                        return true;
                    }
                    else if (motionEvent.getAction() == MotionEvent.ACTION_UP && preview.isVideoRecording())
                    {
                        stopVideoRecording();
                        return true;
                    }
                }
                else
                {
                    Toast.makeText(CameraActivity.this, "Recording is not available for profile picture", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });*/
    }

    void stopVideoRecording()
    {
        //stop recording
        takePicture();
        //switch to picture mode
        preview.switchVideo(false);
        mVideoLimitHandler.removeCallbacksAndMessages(null);
    }

    void startVideoRecording()
    {
        //switch to video mode
        preview.switchVideo(false);
        //start recording
        takePicture();
        mVideoLimitHandler.sendEmptyMessageDelayed(1, 2 * 60 * 1000);
    }


    @Override
    public boolean handleMessage(Message message) {
        if (message.what == 1) {
            if (preview.isVideoRecording()) {
                //stop recording
                takePicture();
                //switch to picture mode
                preview.switchVideo(false);
            }
        }
        return true;
    }

    /**
     * Determine whether we support Camera2 API.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initCamera2Support() {
        if (MyDebug.LOG)
            Log.d(TAG, "initCamera2Support");
        supports_camera2 = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraControllerManager2 manager2 = new CameraControllerManager2(this);
            supports_camera2 = true;
            if (manager2.getNumberOfCameras() == 0) {
                if (MyDebug.LOG)
                    Log.d(TAG, "Camera2 reports 0 cameras");
                supports_camera2 = false;
            }
            for (int i = 0; i < manager2.getNumberOfCameras() && supports_camera2; i++) {
                if (!manager2.allowCamera2Support(i)) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "camera " + i + " doesn't have limited or full support for Camera2 API");
                    supports_camera2 = false;
                }
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "supports_camera2? " + supports_camera2);
    }

    private void preloadIcons(int icons_id) {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "preloadIcons: " + icons_id);
            debug_time = System.currentTimeMillis();
        }
        String[] icons = getResources().getStringArray(icons_id);
        for (int i = 0; i < icons.length; i++) {
            int resource = getResources().getIdentifier(icons[i], null, this.getApplicationContext().getPackageName());
            if (MyDebug.LOG)
                Log.d(TAG, "load resource: " + resource);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), resource);
            this.preloaded_bitmap_resources.put(resource, bm);
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "preloadIcons: total time for preloadIcons: " + (System.currentTimeMillis() - debug_time));
            Log.d(TAG, "size of preloaded_bitmap_resources: " + preloaded_bitmap_resources.size());
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onDestroy() {
        if (MyDebug.LOG) {
            Log.d(TAG, "onDestroy");
            Log.d(TAG, "size of preloaded_bitmap_resources: " + preloaded_bitmap_resources.size());
        }
        if (applicationInterface != null) {
            applicationInterface.onDestroy();
        }
        // Need to recycle to avoid out of memory when running tests - probably good practice to do anyway
        for (Map.Entry<Integer, Bitmap> entry : preloaded_bitmap_resources.entrySet()) {
            if (MyDebug.LOG)
                Log.d(TAG, "recycle: " + entry.getKey());
            entry.getValue().recycle();
        }
        preloaded_bitmap_resources.clear();

        super.onDestroy();
    }

    private void setFirstTimeFlag() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PreferenceKeys.getFirstTimePreferenceKey(), true);
        editor.apply();
    }

    @SuppressWarnings("deprecation")
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (MyDebug.LOG)
            Log.d(TAG, "onKeyDown: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: // media codes are for "selfie sticks" buttons
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP: {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    keydown_volume_up = true;
                else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                    keydown_volume_down = true;

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String volume_keys = sharedPreferences.getString(PreferenceKeys.getVolumeKeysPreferenceKey(), "volume_take_photo");

                if ((keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                        || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        || keyCode == KeyEvent.KEYCODE_MEDIA_STOP)
                        && !(volume_keys.equals("volume_take_photo"))) {
                    AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager == null) break;
                    if (!audioManager.isWiredHeadsetOn())
                        break; // isWiredHeadsetOn() is deprecated, but comment says "Use only to check is a headset is connected or not."
                }

                if (volume_keys.equals("volume_take_photo")) {
                    takePicture();
                    return true;
                } else if (volume_keys.equals("volume_focus")) {
                    if (keydown_volume_up && keydown_volume_down) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "take photo rather than focus, as both volume keys are down");
                        takePicture();
                    } else if (preview.getCurrentFocusValue() != null && preview.getCurrentFocusValue().equals("focus_mode_manual2")) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                            this.changeFocusDistance(-1);
                        else
                            this.changeFocusDistance(1);
                    } else {
                        // important not to repeatedly request focus, even though preview.requestAutoFocus() will cancel, as causes problem if key is held down (e.g., flash gets stuck on)
                        // also check DownTime vs EventTime to prevent repeated focusing whilst the key is held down
                        if (event.getDownTime() == event.getEventTime() && !preview.isFocusWaiting()) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "request focus due to volume key");
                            preview.requestAutoFocus();
                        }
                    }
                    return true;
                } else if (volume_keys.equals("volume_zoom")) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        this.zoomIn();
                    else
                        this.zoomOut();
                    return true;
                } else if (volume_keys.equals("volume_auto_stabilise")) {
                    if (this.supports_auto_stabilise) {
                        boolean auto_stabilise = sharedPreferences.getBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), false);
                        auto_stabilise = !auto_stabilise;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), auto_stabilise);
                        editor.apply();
                        String message = getResources().getString(R.string.preference_auto_stabilise) + ": " + getResources().getString(auto_stabilise ? R.string.on : R.string.off);
                        preview.showToast(changed_auto_stabilise_toast, message);
                    } else {
                        preview.showToast(changed_auto_stabilise_toast, R.string.auto_stabilise_not_supported);
                    }
                    return true;
                } else if (volume_keys.equals("volume_really_nothing")) {
                    // do nothing, but still return true so we don't change volume either
                    return true;
                }
                // else do nothing here, but still allow changing of volume (i.e., the default behaviour)
                break;
            }
            case KeyEvent.KEYCODE_MENU: {
                // needed to support hardware menu button
                // tested successfully on Samsung S3 (via RTL)
                // see http://stackoverflow.com/questions/8264611/how-to-detect-when-user-presses-menu-key-on-their-android-device
                //openSettings();
                return true;
            }
            case KeyEvent.KEYCODE_CAMERA: {
                if (event.getRepeatCount() == 0) {
                    takePicture();
                    return true;
                }
            }
            case KeyEvent.KEYCODE_FOCUS: {
                // important not to repeatedly request focus, even though preview.requestAutoFocus() will cancel - causes problem with hardware camera key where a half-press means to focus
                // also check DownTime vs EventTime to prevent repeated focusing whilst the key is held down - see https://sourceforge.net/p/opencamera/tickets/174/ ,
                // or same issue above for volume key focus
                if (event.getDownTime() == event.getEventTime() && !preview.isFocusWaiting()) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "request focus due to focus key");
                    preview.requestAutoFocus();
                }
                return true;
            }
            case KeyEvent.KEYCODE_ZOOM_IN: {
                this.zoomIn();
                return true;
            }
            case KeyEvent.KEYCODE_ZOOM_OUT: {
                this.zoomOut();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (MyDebug.LOG)
            Log.d(TAG, "onKeyUp: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            keydown_volume_up = false;
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
            keydown_volume_down = false;

        return super.onKeyUp(keyCode, event);
    }

    public void zoomIn() {
        mainUI.changeSeekbar(R.id.zoom_seekbar, -1);
    }

    public void zoomOut() {
        mainUI.changeSeekbar(R.id.zoom_seekbar, 1);
    }

    void changeFocusDistance(int change) {
        mainUI.changeSeekbar(R.id.focus_seekbar, change);
    }

    @Override
    protected void onResume() {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "onResume");
            debug_time = System.currentTimeMillis();
        }
        super.onResume();

        // Set black window background; also needed if we hide the virtual buttons in immersive mode
        // Note that we do it here rather than customising the theme's android:windowBackground, so this doesn't affect other views - in particular, the MyPreferenceFragment settings
        getWindow().getDecorView().getRootView().setBackgroundColor(Color.BLACK);

        mSensorManager.registerListener(accelerometerListener, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(magneticListener, mSensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        orientationEventListener.enable();

        initSound();
        loadSound(R.raw.beep);
        loadSound(R.raw.beep_hi);

        mainUI.layoutUI();

        preview.onResume();

        if (MyDebug.LOG) {
            Log.d(TAG, "onResume: total time to resume: " + (System.currentTimeMillis() - debug_time));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (MyDebug.LOG)
            Log.d(TAG, "onWindowFocusChanged: " + hasFocus);
        super.onWindowFocusChanged(hasFocus);
        if (!this.camera_in_background && hasFocus) {
            // low profile mode is cleared when app goes into background
            // and for Kit Kat immersive mode, we want to set up the timer
            // we do in onWindowFocusChanged rather than onResume(), to also catch when window lost focus due to notification bar being dragged down (which prevents resetting of immersive mode)
        }
    }

    @Override
    protected void onPause() {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "onPause");
            debug_time = System.currentTimeMillis();
        }
        waitUntilImageQueueEmpty(); // so we don't risk losing any images
        super.onPause(); // docs say to call this before freeing other things
        mSensorManager.unregisterListener(accelerometerListener);
        mSensorManager.unregisterListener(magneticListener);
        orientationEventListener.disable();
        releaseSound();
        applicationInterface.clearLastImages(); // this should happen when pausing the preview, but call explicitly just to be safe
        preview.onPause();
        if (MyDebug.LOG) {
            Log.d(TAG, "onPause: total time to pause: " + (System.currentTimeMillis() - debug_time));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (MyDebug.LOG)
            Log.d(TAG, "onConfigurationChanged()");
        // configuration change can include screen orientation (landscape/portrait) when not locked (when settings is open)
        // needed if app is paused/resumed when settings is open and device is in portrait mode
        preview.setCameraDisplayOrientation();
        super.onConfigurationChanged(newConfig);
    }

    public void waitUntilImageQueueEmpty() {
        if (MyDebug.LOG)
            Log.d(TAG, "waitUntilImageQueueEmpty");
        applicationInterface.getImageSaver().waitUntilDone();
    }

    /*
    public void clickedTakePhoto(View view)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedTakePhoto");
        this.takePicture();
    }
    */

    /* Returns the cameraId that the "Switch camera" button will switch to.
     */
    public int getNextCameraId() {
        if (MyDebug.LOG)
            Log.d(TAG, "getNextCameraId");
        int cameraId = preview.getCameraId();
        if (MyDebug.LOG)
            Log.d(TAG, "current cameraId: " + cameraId);
        if (this.preview.canSwitchCamera()) {
            int n_cameras = preview.getCameraControllerManager().getNumberOfCameras();
            cameraId = (cameraId + 1) % n_cameras;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "next cameraId: " + cameraId);
        return cameraId;
    }

    public void clickedSwitchCamera(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedSwitchCamera");
        if (this.preview.canSwitchCamera()) {
            int cameraId = getNextCameraId();
            View switchCameraButton = (View) findViewById(R.id.switch_camera);
            switchCameraButton.setEnabled(false); // prevent slowdown if user repeatedly clicks
            this.preview.setCamera(cameraId);
            switchCameraButton.setEnabled(true);
            mainUI.setSwitchCameraContentDescription();
        }
    }

    public void clickedSwitchGallery(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedSwitchGallery");
        //go to gallery
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/* video/*");

        PackageManager pm = getPackageManager();

        List<ResolveInfo> launchables=pm.queryIntentActivities(getIntent, 0);
        ActivityInfo targetActivity = null;

        for (ResolveInfo info :launchables)
        {
            boolean b =checkForGallery(info);
            if(b)
            {
                targetActivity = info.activityInfo;
                break;
            }
        }

        if(targetActivity!=null)
        {
            Intent targetedShareIntent = new Intent(Intent.ACTION_GET_CONTENT);
            targetedShareIntent.setPackage(targetActivity.packageName);
            targetedShareIntent.setClassName(targetActivity.packageName, targetActivity.name);
            startActivityForResult(targetedShareIntent,Constant.GALLERY_RESULT);
        }
        else
        {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/* video/*");

            Intent chooserIntent = Intent.createChooser(getIntent, "Select Image/Video");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

            startActivityForResult(chooserIntent,Constant.GALLERY_RESULT);
        }
    }

    boolean checkForGallery(ResolveInfo resolveInfo)
    {
        String search = "gallery";
        ActivityInfo mActivityInfo = resolveInfo.activityInfo;
        Log.e("ERR",String.valueOf(mActivityInfo.loadLabel(getPackageManager())).toLowerCase());
        if(String.valueOf(mActivityInfo.loadLabel(getPackageManager())).toLowerCase().contains(search) ||
                mActivityInfo.packageName.toLowerCase().contains(search) ||
                mActivityInfo.name.toLowerCase().contains(search))
        {
            return true;
        }
        return false;
    }

    public void clickedShare(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedShare");
        //Upload the last image
        //TODO upload img code


        //start the preview to enter the caption
        applicationInterface.shareLastImage();

    }

    public void clickedDelete(View view) {
        //delete the last image
        if (MyDebug.LOG)
            Log.d(TAG, "clickedDelete");
        applicationInterface.trashLastImage();
    }

    public void clickedMoments(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedMoments");
       // startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void setProgressSeekbarScaled(SeekBar seekBar, double min_value, double max_value, double value) {
        seekBar.setMax(100);
        double scaling = (value - min_value) / (max_value - min_value);
        double frac = CameraActivity.seekbarScalingInverse(scaling);
        int percent = (int) (frac * 100.0 + 0.5); // add 0.5 for rounding
        if (percent < 0)
            percent = 0;
        else if (percent > 100)
            percent = 100;
        seekBar.setProgress(percent);
    }

    public Bitmap getPreloadedBitmap(int resource) {
        Bitmap bm = this.preloaded_bitmap_resources.get(resource);
        return bm;
    }

    public void clickedUpdateFlash(View view) {
        if (supported_flash_values == null) {
            supported_flash_values = preview.getSupportedFlashValues();
        }

        if (supported_flash_values == null)
            return;

        int resource = -1;

        int size = supported_flash_values.size()>3? 3 :supported_flash_values.size();

        currentFlashModeIndex = (currentFlashModeIndex + 1) % size;

        String option = supported_flash_values.get(currentFlashModeIndex);
        if (icons != null && values != null) {
            int index = -1;
            for (int i = 0; i < values.length && index == -1; i++) {
                if (values[i].equals(option))
                    index = i;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "index: " + index);
            if (index != -1) {
                resource = getResources().getIdentifier(icons[index], null, getApplicationContext().getPackageName());
            }
        }

        Bitmap bm = getPreloadedBitmap(resource);

        ((ImageView) view).setImageBitmap(bm);

        //if (bm != null)
        //    image_button.setImageBitmap(bm);
        if (MyDebug.LOG)
            Log.d(TAG, "clicked flash: " + option);
        preview.updateFlash(option);
    }

    public boolean usingKitKatImmersiveMode() {
        // whether we are using a Kit Kat style immersive mode (either hiding GUI, or everything)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String immersive_mode = sharedPreferences.getString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");
            if (immersive_mode.equals("immersive_mode_gui") || immersive_mode.equals("immersive_mode_everything"))
                return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    void setImmersiveMode(boolean on) {
        if (MyDebug.LOG)
            Log.d(TAG, "setImmersiveMode: " + on);
        // n.b., preview.setImmersiveMode() is called from onSystemUiVisibilityChange()
        if (on) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && usingKitKatImmersiveMode()) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String immersive_mode = sharedPreferences.getString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");
                if (immersive_mode.equals("immersive_mode_low_profile"))
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                else
                    getWindow().getDecorView().setSystemUiVisibility(0);
            }
        } else
            getWindow().getDecorView().setSystemUiVisibility(0);
    }

    /**
     * Sets the brightness level for normal operation (when camera preview is visible).
     * If force_max is true, this always forces maximum brightness; otherwise this depends on user preference.
     */
    void setBrightnessForCamera(boolean force_max) {
        if (MyDebug.LOG)
            Log.d(TAG, "setBrightnessForCamera");
        // set screen to max brightness - see http://stackoverflow.com/questions/11978042/android-screen-brightness-max-value
        // done here rather than onCreate, so that changing it in preferences takes effect without restarting app
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (force_max || sharedPreferences.getBoolean(PreferenceKeys.getMaxBrightnessPreferenceKey(), true)) {
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        } else {
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
        getWindow().setAttributes(layout);
    }

    /**
     * Sets the window flags for normal operation (when camera preview is visible).
     */
    public void setWindowFlagsForCamera() {
        if (MyDebug.LOG)
            Log.d(TAG, "setWindowFlagsForCamera");
        /*{
            Intent intent = new Intent(this, MyWidgetProvider.class);
    		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    		AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
    		ComponentName widgetComponent = new ComponentName(this, MyWidgetProvider.class);
    		int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
    		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
    		sendBroadcast(intent);
    	}*/
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // force to landscape mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE); // testing for devices with unusual sensor orientation (e.g., Nexus 5X)
        // keep screen active - see http://stackoverflow.com/questions/2131948/force-screen-on
        if (sharedPreferences.getBoolean(PreferenceKeys.getKeepDisplayOnPreferenceKey(), true)) {
            if (MyDebug.LOG)
                Log.d(TAG, "do keep screen on");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "don't keep screen on");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (sharedPreferences.getBoolean(PreferenceKeys.getShowWhenLockedPreferenceKey(), true)) {
            if (MyDebug.LOG)
                Log.d(TAG, "do show when locked");
            // keep Open Camera on top of screen-lock (will still need to unlock when going to gallery or settings)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "don't show when locked");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        setBrightnessForCamera(false);

        camera_in_background = false;
    }

    public void showPreview(boolean show) {
        if (MyDebug.LOG)
            Log.d(TAG, "showPreview: " + show);
    }

    void savingImage(final boolean started) {
        if (MyDebug.LOG)
            Log.d(TAG, "savingImage: " + started);
    }

    /**
     * Opens the Storage Access Framework dialog to select a folder.
     *
     * @param from_preferences Whether called from the Preferences
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void openFolderChooserDialogSAF(boolean from_preferences) {
        if (MyDebug.LOG)
            Log.d(TAG, "openFolderChooserDialogSAF: " + from_preferences);
        this.saf_dialog_from_preferences = from_preferences;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 42);
    }

    /**
     * Listens for the response from the Storage Access Framework dialog to select a folder
     * (as opened with openFolderChooserDialogSAF()).
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (MyDebug.LOG)
            Log.d(TAG, "onActivityResult: " + requestCode);
        if (requestCode == Constant.GALLERY_RESULT) {
            if (resultCode == RESULT_OK && resultData != null) {
                Uri selectedMediaUri = resultData.getData();

                String selectedPath = ImageOperations.getPath(this, selectedMediaUri);

                if (selectedMediaUri.toString().contains("images")) {
                    //uploadFile(selectedPath,Constant.TYPE_PICTURE,false);
                    ChangeActivity.sendToPreview(CameraActivity.this, selectedPath, Constant.TYPE_PICTURE, false);
                } else if (selectedMediaUri.toString().contains("video")) {
                    //uploadFile(selectedPath,Constant.TYPE_VIDEO,false);
                    ChangeActivity.sendToPreview(CameraActivity.this, selectedPath, Constant.TYPE_VIDEO, false);
                }
                finish();
            }
        } else if (requestCode == 42) {
            if (resultCode == RESULT_OK && resultData != null) {
                Uri treeUri = resultData.getData();
                if (MyDebug.LOG)
                    Log.d(TAG, "returned treeUri: " + treeUri);
                // from https://developer.android.com/guide/topics/providers/document-provider.html#permissions :
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), treeUri.toString());
                editor.apply();

                String filename = applicationInterface.getStorageUtils().getImageFolderNameSAF();
                if (filename != null) {
                    preview.showToast(null, getResources().getString(R.string.changed_save_location) + "\n" + filename);
                }
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "SAF dialog cancelled");
                // cancelled - if the user had yet to set a save location, make sure we switch SAF back off
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String uri = sharedPreferences.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "");
                if (uri.length() == 0) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "no SAF save location was set");
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false);
                    editor.apply();
                    preview.showToast(null, R.string.saf_cancelled);
                }
            }

            if (!saf_dialog_from_preferences) {
                setWindowFlagsForCamera();
                showPreview(true);
            }
        }
    }

    private void takePicture() {
        if (MyDebug.LOG)
            Log.d(TAG, "takePicture");
        this.preview.takePicturePressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(state);
        if (this.preview != null) {
            preview.onSaveInstanceState(state);
        }
        if (this.applicationInterface != null) {
            applicationInterface.onSaveInstanceState(state);
        }
    }

    void cameraSetup() {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "cameraSetup");
            debug_time = System.currentTimeMillis();
        }
        if (this.supportsForceVideo4K() && preview.usingCamera2API()) {
            if (MyDebug.LOG)
                Log.d(TAG, "using Camera2 API, so can disable the force 4K option");
            this.disableForceVideo4K();
        }
        if (this.supportsForceVideo4K() && preview.getSupportedVideoSizes() != null) {
            for (CameraController.Size size : preview.getSupportedVideoSizes()) {
                if (size.width >= 3840 && size.height >= 2160) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "camera natively supports 4K, so can disable the force option");
                    this.disableForceVideo4K();
                }
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "cameraSetup: time after handling Force 4K option: " + (System.currentTimeMillis() - debug_time));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        {
            if (MyDebug.LOG)
                Log.d(TAG, "set up zoom");
            if (MyDebug.LOG)
                Log.d(TAG, "has_zoom? " + preview.supportsZoom());
            SeekBar zoomSeekBar = (SeekBar) findViewById(R.id.zoom_seekbar);

            if (preview.supportsZoom()) {
                zoomSeekBar.setOnSeekBarChangeListener(null); // clear an existing listener - don't want to call the listener when setting up the progress bar to match the existing state
                zoomSeekBar.setMax(preview.getMaxZoom());
                zoomSeekBar.setProgress(preview.getMaxZoom() - preview.getCameraController().getZoom());
                zoomSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "zoom onProgressChanged: " + progress);
                        preview.zoomTo(preview.getMaxZoom() - progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
            } else {
                zoomSeekBar.setVisibility(View.GONE);
            }
            if (MyDebug.LOG)
                Log.d(TAG, "cameraSetup: time after setting up zoom: " + (System.currentTimeMillis() - debug_time));
        }
        {
            if (MyDebug.LOG)
                Log.d(TAG, "set up manual focus");
            SeekBar focusSeekBar = (SeekBar) findViewById(R.id.focus_seekbar);
            focusSeekBar.setOnSeekBarChangeListener(null); // clear an existing listener - don't want to call the listener when setting up the progress bar to match the existing state
            setProgressSeekbarScaled(focusSeekBar, 0.0, preview.getMinimumFocusDistance(), preview.getCameraController().getFocusDistance());

            focusSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                {
                    double frac = progress / 100.0;
                    double scaling = CameraActivity.seekbarScaling(frac);
                    float focus_distance = (float) (scaling * preview.getMinimumFocusDistance());
                    preview.setFocusDistance(focus_distance);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar)
                {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar)
                {
                }
            });
            final int visibility = preview.getCurrentFocusValue() != null && this.getPreview().getCurrentFocusValue().equals("focus_mode_manual2") ? View.VISIBLE : View.INVISIBLE;
            focusSeekBar.setVisibility(visibility);
            }
            if (MyDebug.LOG)
                Log.d(TAG, "cameraSetup: time after setting up manual focus: " + (System.currentTimeMillis() - debug_time));

            mainUI.setSwitchCameraContentDescription();
            if (MyDebug.LOG)
                Log.d(TAG, "cameraSetup: time after setting take photo icon: " + (System.currentTimeMillis() - debug_time));

            if (MyDebug.LOG)
                Log.d(TAG, "cameraSetup: total time for cameraSetup: " + (System.currentTimeMillis() - debug_time));
        }

    public boolean supportsAutoStabilise() {
        return this.supports_auto_stabilise;
    }

    public boolean supportsHDR() {
        // we also require the device have sufficient memory to do the processing, simplest to use the same test as we do for auto-stabilise...
        if (this.supportsAutoStabilise() && preview.supportsExpoBracketing())
            return true;
        return false;
    }

    public boolean supportsExpoBracketing() {
        if (preview.supportsExpoBracketing())
            return true;
        return false;
    }

    /*public static String getDonateMarketLink() {
        return "market://details?id=harman.mark.donation";
    }*/

    public boolean supportsForceVideo4K() {
        return this.supports_force_video_4k;
    }

    public boolean supportsCamera2() {
        return this.supports_camera2;
    }

    void disableForceVideo4K() {
        this.supports_force_video_4k = false;
    }

    /**
     * Return free memory in MB.
     */
    @SuppressWarnings("deprecation")
    public long freeMemory() { // return free memory in MB
        try {
            File folder = applicationInterface.getStorageUtils().getImageFolder();
            if (folder == null) {
                throw new IllegalArgumentException(); // so that we fall onto the backup
            }
            StatFs statFs = new StatFs(folder.getAbsolutePath());
            // cast to long to avoid overflow!
            long blocks = statFs.getAvailableBlocks();
            long size = statFs.getBlockSize();
            long free = (blocks * size) / 1048576;
            /*if( MyDebug.LOG ) {
				Log.d(TAG, "freeMemory blocks: " + blocks + " size: " + size + " free: " + free);
			}*/
            return free;
        } catch (IllegalArgumentException e) {
            // this can happen if folder doesn't exist, or don't have read access
            // if the save folder is a subfolder of DCIM, we can just use that instead
            try {
                if (!applicationInterface.getStorageUtils().isUsingSAF()) {
                    // StorageUtils.getSaveLocation() only valid if !isUsingSAF()
                    String folder_name = applicationInterface.getStorageUtils().getSaveLocation();
                    if (!folder_name.startsWith("/")) {
                        File folder = StorageUtils.getBaseFolder();
                        StatFs statFs = new StatFs(folder.getAbsolutePath());
                        // cast to long to avoid overflow!
                        long blocks = statFs.getAvailableBlocks();
                        long size = statFs.getBlockSize();
                        long free = (blocks * size) / 1048576;
            			/*if( MyDebug.LOG ) {
            				Log.d(TAG, "freeMemory blocks: " + blocks + " size: " + size + " free: " + free);
            			}*/
                        return free;
                    }
                }
            } catch (IllegalArgumentException e2) {
                // just in case
            }
        }
        return -1;
    }

    public Preview getPreview() {
        return this.preview;
    }

    public MainUI getMainUI() {
        return this.mainUI;
    }

    public MyApplicationInterface getApplicationInterface() {
        return this.applicationInterface;
    }

    public StorageUtils getStorageUtils() {
        return this.applicationInterface.getStorageUtils();
    }

    public File getImageFolder() {
        return this.applicationInterface.getStorageUtils().getImageFolder();
    }

    public ToastBoxer getChangedAutoStabiliseToastBoxer() {
        return changed_auto_stabilise_toast;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initSound() {
        if (sound_pool == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "create new sound_pool");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audio_attributes = new AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_SYSTEM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                sound_pool = new SoundPool.Builder()
                        .setMaxStreams(1)
                        .setAudioAttributes(audio_attributes)
                        .build();
            } else {
                sound_pool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
            }
            sound_ids = new SparseIntArray();
        }
    }

    // Android 6+ permission handling:

    private void releaseSound() {
        if (sound_pool != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "release sound_pool");
            sound_pool.release();
            sound_pool = null;
            sound_ids = null;
        }
    }

    // must be called before playSound (allowing enough time to load the sound)
    void loadSound(int resource_id) {
        if (sound_pool != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "loading sound resource: " + resource_id);
            int sound_id = sound_pool.load(this, resource_id, 1);
            if (MyDebug.LOG)
                Log.d(TAG, "    loaded sound: " + sound_id);
            sound_ids.put(resource_id, sound_id);
        }
    }

    // must call loadSound first (allowing enough time to load the sound)
    void playSound(int resource_id) {
        if (sound_pool != null) {
            if (sound_ids.indexOfKey(resource_id) < 0) {
                if (MyDebug.LOG)
                    Log.d(TAG, "resource not loaded: " + resource_id);
            } else {
                int sound_id = sound_ids.get(resource_id);
                if (MyDebug.LOG)
                    Log.d(TAG, "play sound: " + sound_id);
                sound_pool.play(sound_id, 1.0f, 1.0f, 0, 0, 1);
            }
        }
    }

    /**
     * Show a "rationale" to the user for needing a particular permission, then request that permission again
     * once they close the dialog.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void showRequestPermissionRationale(final int permission_code) {
        if (MyDebug.LOG)
            Log.d(TAG, "showRequestPermissionRational: " + permission_code);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (MyDebug.LOG)
                Log.e(TAG, "shouldn't be requesting permissions for pre-Android M!");
            return;
        }

        boolean ok = true;
        String[] permissions = null;
        int message_id = 0;
        if (permission_code == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (MyDebug.LOG)
                Log.d(TAG, "display rationale for camera permission");
            permissions = new String[]{Manifest.permission.CAMERA};
            message_id = R.string.permission_rationale_camera;
        } else if (permission_code == MY_PERMISSIONS_REQUEST_STORAGE) {
            if (MyDebug.LOG)
                Log.d(TAG, "display rationale for storage permission");
            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            message_id = R.string.permission_rationale_storage;
        } else if (permission_code == MY_PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (MyDebug.LOG)
                Log.d(TAG, "display rationale for record audio permission");
            permissions = new String[]{Manifest.permission.RECORD_AUDIO};
            message_id = R.string.permission_rationale_record_audio;
        } else {
            if (MyDebug.LOG)
                Log.e(TAG, "showRequestPermissionRational unknown permission_code: " + permission_code);
            ok = false;
        }

        if (ok) {
            final String[] permissions_f = permissions;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_rationale_title)
                    .setMessage(message_id)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(new OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "requesting permission...");
                            ActivityCompat.requestPermissions(CameraActivity.this, permissions_f, permission_code);
                        }
                    }).show();
        }
    }

    void requestCameraPermission() {
        if (MyDebug.LOG)
            Log.d(TAG, "requestCameraPermission");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (MyDebug.LOG)
                Log.e(TAG, "shouldn't be requesting permissions for pre-Android M!");
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            showRequestPermissionRationale(MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            // Can go ahead and request the permission
            if (MyDebug.LOG)
                Log.d(TAG, "requesting camera permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    void requestStoragePermission() {
        if (MyDebug.LOG)
            Log.d(TAG, "requestStoragePermission");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (MyDebug.LOG)
                Log.e(TAG, "shouldn't be requesting permissions for pre-Android M!");
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            showRequestPermissionRationale(MY_PERMISSIONS_REQUEST_STORAGE);
        } else {
            // Can go ahead and request the permission
            if (MyDebug.LOG)
                Log.d(TAG, "requesting storage permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
        }
    }

    void requestRecordAudioPermission() {
        if (MyDebug.LOG)
            Log.d(TAG, "requestRecordAudioPermission");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (MyDebug.LOG)
                Log.e(TAG, "shouldn't be requesting permissions for pre-Android M!");
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            showRequestPermissionRationale(MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            // Can go ahead and request the permission
            if (MyDebug.LOG)
                Log.d(TAG, "requesting record audio permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (MyDebug.LOG)
            Log.d(TAG, "onRequestPermissionsResult: requestCode " + requestCode);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (MyDebug.LOG)
                Log.e(TAG, "shouldn't be requesting permissions for pre-Android M!");
            return;
        }

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (MyDebug.LOG)
                        Log.d(TAG, "camera permission granted");
                    preview.retryOpenCamera();
                } else {
                    if (MyDebug.LOG)
                        Log.d(TAG, "camera permission denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // Open Camera doesn't need to do anything: the camera will remain closed
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (MyDebug.LOG)
                        Log.d(TAG, "storage permission granted");
                    preview.retryOpenCamera();
                } else {
                    if (MyDebug.LOG)
                        Log.d(TAG, "storage permission denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // Open Camera doesn't need to do anything: the camera will remain closed
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (MyDebug.LOG)
                        Log.d(TAG, "record audio permission granted");
                    // no need to do anything
                } else {
                    if (MyDebug.LOG)
                        Log.d(TAG, "record audio permission denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // no need to do anything
                    // note that we don't turn off record audio option, as user may then record video not realising audio won't be recorded - best to be explicit each time
                }
                return;
            }
            default: {
                if (MyDebug.LOG)
                    Log.e(TAG, "unknown requestCode " + requestCode);
            }
        }
    }
}
