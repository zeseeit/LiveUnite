package com.liveunite.opencamera;

/**
 * Stores all of the string keys used for SharedPreferences.
 */
public class PreferenceKeys
{
    // must be static, to safely call from other Activities

    // arguably the static methods here that don't receive an argument could just be static final strings? Though we may want to change some of them to be cameraId-specific in future

    /**
     * If this preference is set, no longer show the intro dialog.
     */
    public static String getFirstTimePreferenceKey()
    {
        return "done_first_time";
    }

    /**
     * If this preference is set, no longer show the auto-stabilise info dialog.
     */
    public static String getAutoStabiliseInfoPreferenceKey()
    {
        return "done_auto_stabilise_info";
    }

    /**
     * If this preference is set, no longer show the raw info dialog.
     */
    public static String getRawInfoPreferenceKey()
    {
        return "done_raw_info";
    }

    public static String getUseCamera2PreferenceKey()
    {
        return "preference_use_camera2";
    }

    public static String getFlashPreferenceKey(int cameraId)
    {
        return "flash_value_" + cameraId;
    }

    public static String getFocusPreferenceKey(int cameraId, boolean is_video)
    {
        return "focus_value_" + cameraId + "_" + is_video;
    }

    public static String getResolutionPreferenceKey(int cameraId)
    {
        return "camera_resolution_" + cameraId;
    }

    public static String getVideoQualityPreferenceKey(int cameraId)
    {
        return "video_quality_" + cameraId;
    }

    public static String getIsVideoPreferenceKey()
    {
        return "is_video";
    }

    public static String getExposurePreferenceKey()
    {
        return "preference_exposure";
    }

    public static String getColorEffectPreferenceKey()
    {
        return "preference_color_effect";
    }

    public static String getSceneModePreferenceKey()
    {
        return "preference_scene_mode";
    }

    public static String getWhiteBalancePreferenceKey()
    {
        return "preference_white_balance";
    }

    public static String getISOPreferenceKey()
    {
        return "preference_iso";
    }

    public static String getExposureTimePreferenceKey()
    {
        return "preference_exposure_time";
    }

    public static String getRawPreferenceKey()
    {
        return "preference_raw";
    }

    public static String getExpoBracketingNImagesPreferenceKey()
    {
        return "preference_expo_bracketing_n_images";
    }

    public static String getExpoBracketingStopsPreferenceKey()
    {
        return "preference_expo_bracketing_stops";
    }

    public static String getVolumeKeysPreferenceKey()
    {
        return "preference_volume_keys";
    }

    public static String getAudioNoiseControlSensitivityPreferenceKey()
    {
        return "preference_audio_noise_control_sensitivity";
    }

    public static String getQualityPreferenceKey()
    {
        return "preference_quality";
    }

    public static String getAutoStabilisePreferenceKey()
    {
        return "preference_auto_stabilise";
    }

    public static String getPhotoModePreferenceKey()
    {
        return "preference_photo_mode";
    }

    public static String getCamera2FakeFlashPreferenceKey()
    {
        return "preference_camera2_fake_flash";
    }

    public static String getUIPlacementPreferenceKey()
    {
        return "preference_ui_placement";
    }

    public static String getTouchCapturePreferenceKey()
    {
        return "preference_touch_capture";
    }

    public static String getPausePreviewPreferenceKey()
    {
        return "preference_pause_preview";
    }

    public static String getShowToastsPreferenceKey()
    {
        return "preference_show_toasts";
    }

    public static String getThumbnailAnimationPreferenceKey()
    {
        return "preference_thumbnail_animation";
    }

    public static String getTakePhotoBorderPreferenceKey()
    {
        return "preference_take_photo_border";
    }

    public static String getShowWhenLockedPreferenceKey()
    {
        return "preference_show_when_locked";
    }

    public static String getStartupFocusPreferenceKey()
    {
        return "preference_startup_focus";
    }

    public static String getKeepDisplayOnPreferenceKey()
    {
        return "preference_keep_display_on";
    }

    public static String getMaxBrightnessPreferenceKey()
    {
        return "preference_max_brightness";
    }

    public static String getUsingSAFPreferenceKey()
    {
        return "preference_using_saf";
    }

    public static String getSaveLocationPreferenceKey()
    {
        return "preference_save_location";
    }

    public static String getSaveLocationSAFPreferenceKey()
    {
        return "preference_save_location_saf";
    }

    public static String getSavePhotoPrefixPreferenceKey()
    {
        return "preference_save_photo_prefix";
    }

    public static String getSaveVideoPrefixPreferenceKey()
    {
        return "preference_save_video_prefix";
    }

    public static String getSaveZuluTimePreferenceKey()
    {
        return "preference_save_zulu_time";
    }

    public static String getShowISOPreferenceKey()
    {
        return "preference_show_iso";
    }

    public static String getShowCropGuidePreferenceKey()
    {
        return "preference_crop_guide";
    }

    public static String getFaceDetectionPreferenceKey()
    {
        return "preference_face_detection";
    }

    public static String getVideoStabilizationPreferenceKey()
    {
        return "preference_video_stabilization";
    }

    public static String getVideoBitratePreferenceKey()
    {
        return "preference_video_bitrate";
    }

    public static String getVideoFPSPreferenceKey()
    {
        return "preference_video_fps";
    }

    public static String getVideoMaxDurationPreferenceKey()
    {
        return "preference_video_max_duration";
    }

    public static String getVideoMaxFileSizePreferenceKey()
    {
        return "preference_video_max_filesize";
    }

    public static String getPreviewSizePreferenceKey()
    {
        return "preference_preview_size";
    }

    public static String getShutterSoundPreferenceKey()
    {
        return "preference_shutter_sound";
    }

    public static String getImmersiveModePreferenceKey()
    {
        return "preference_immersive_mode";
    }
}
