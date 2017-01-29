package com.liveunite.opencamera.UI;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;

import com.liveunite.R;
import com.liveunite.opencamera.CameraActivity;
import com.liveunite.opencamera.CameraController.CameraController;
import com.liveunite.opencamera.MyApplicationInterface;
import com.liveunite.opencamera.MyDebug;
import com.liveunite.opencamera.PreferenceKeys;
import com.liveunite.opencamera.Preview.Preview;

public class DrawPreview
{
    private static final String TAG = "DrawPreview";

    private CameraActivity main_activity = null;
    private MyApplicationInterface applicationInterface = null;

    private Paint p = new Paint();
    private RectF face_rect = new RectF();
    private RectF draw_rect = new RectF();
    private int[] gui_location = new int[2];
    private float stroke_width = 0.0f;

    private long ae_started_scanning_ms = -1; // time when ae started scanning

    private boolean taking_picture = false;
    private boolean front_screen_flash = false;

    private boolean continuous_focus_moving = false;
    private long continuous_focus_moving_ms = 0;

    public DrawPreview(CameraActivity main_activity, MyApplicationInterface applicationInterface)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "DrawPreview");
        this.main_activity = main_activity;
        this.applicationInterface = applicationInterface;

        p.setAntiAlias(true);
        p.setStrokeCap(Paint.Cap.ROUND);
        final float scale = getContext().getResources().getDisplayMetrics().density;
        this.stroke_width = (float) (1.0f * scale + 0.5f); // convert dps to pixels
        p.setStrokeWidth(stroke_width);

    }

    public void onDestroy()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onDestroy");
        // clean up just in case
    }

    private Context getContext()
    {
        return main_activity;
    }

    public void cameraInOperation(boolean in_operation)
    {
        if (in_operation && !main_activity.getPreview().isVideo())
        {
            taking_picture = true;
        } else
        {
            taking_picture = false;
            front_screen_flash = false;
        }
    }

    public void turnFrontScreenFlashOn()
    {
        front_screen_flash = true;
    }

    public void onContinuousFocusMove(boolean start)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "onContinuousFocusMove: " + start);
        if (start)
        {
            if (!continuous_focus_moving)
            { // don't restart the animation if already in motion
                continuous_focus_moving = true;
                continuous_focus_moving_ms = System.currentTimeMillis();
            }
        }
        // if we receive start==false, we don't stop the animation - let it continue
    }

    public void clearContinuousFocusMove()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "clearContinuousFocusMove");
        continuous_focus_moving = false;
        continuous_focus_moving_ms = 0;
    }

    private boolean getTakePhotoBorderPref()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getBoolean(PreferenceKeys.getTakePhotoBorderPreferenceKey(), true);
    }

    private String getTimeStringFromSeconds(long time)
    {
        int secs = (int) (time % 60);
        time /= 60;
        int mins = (int) (time % 60);
        time /= 60;
        long hours = time;
        //String time_s = hours + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs) + ":" + String.format("%03d", ms);
        String time_s = hours + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs);
        return time_s;
    }

    public void onDrawPreview(Canvas canvas)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        Preview preview = main_activity.getPreview();
        CameraController camera_controller = preview.getCameraController();
        int ui_rotation = preview.getUIRotation();
        boolean has_level_angle = preview.hasLevelAngle();
        double level_angle = preview.getLevelAngle();
        boolean ui_placement_right = main_activity.getMainUI().getUIPlacementRight();
        if (main_activity.getMainUI().inImmersiveMode())
        {
            String immersive_mode = sharedPreferences.getString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");
            if (immersive_mode.equals("immersive_mode_everything"))
            {
                // exit, to ensure we don't display anything!
                return;
            }
        }
        final float scale = getContext().getResources().getDisplayMetrics().density;
        if (camera_controller != null && front_screen_flash)
        {
            p.setColor(Color.WHITE);
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
        } else if (camera_controller != null && taking_picture && getTakePhotoBorderPref())
        {
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            float this_stroke_width = (float) (5.0f * scale + 0.5f); // convert dps to pixels
            p.setStrokeWidth(this_stroke_width);
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
            p.setStyle(Paint.Style.FILL); // reset
            p.setStrokeWidth(stroke_width); // reset
        }

        if (preview.isVideo() || sharedPreferences.getString(PreferenceKeys.getPreviewSizePreferenceKey(), "preference_preview_size_wysiwyg").equals("preference_preview_size_wysiwyg"))
        {
            String preference_crop_guide = sharedPreferences.getString(PreferenceKeys.getShowCropGuidePreferenceKey(), "crop_guide_none");
            if (camera_controller != null && preview.getTargetRatio() > 0.0 && !preference_crop_guide.equals("crop_guide_none"))
            {
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.rgb(255, 235, 59)); // Yellow 500
                double crop_ratio = -1.0;
                if (preference_crop_guide.equals("crop_guide_1"))
                {
                    crop_ratio = 1.0;
                } else if (preference_crop_guide.equals("crop_guide_1.25"))
                {
                    crop_ratio = 1.25;
                } else if (preference_crop_guide.equals("crop_guide_1.33"))
                {
                    crop_ratio = 1.33333333;
                } else if (preference_crop_guide.equals("crop_guide_1.4"))
                {
                    crop_ratio = 1.4;
                } else if (preference_crop_guide.equals("crop_guide_1.5"))
                {
                    crop_ratio = 1.5;
                } else if (preference_crop_guide.equals("crop_guide_1.78"))
                {
                    crop_ratio = 1.77777778;
                } else if (preference_crop_guide.equals("crop_guide_1.85"))
                {
                    crop_ratio = 1.85;
                } else if (preference_crop_guide.equals("crop_guide_2.33"))
                {
                    crop_ratio = 2.33333333;
                } else if (preference_crop_guide.equals("crop_guide_2.35"))
                {
                    crop_ratio = 2.35006120; // actually 1920:817
                } else if (preference_crop_guide.equals("crop_guide_2.4"))
                {
                    crop_ratio = 2.4;
                }
                if (crop_ratio > 0.0 && Math.abs(preview.getTargetRatio() - crop_ratio) > 1.0e-5)
                {
                    /*if( MyDebug.LOG ) {
                        Log.d(TAG, "crop_ratio: " + crop_ratio);
		    			Log.d(TAG, "preview_targetRatio: " + preview_targetRatio);
		    			Log.d(TAG, "canvas width: " + canvas.getWidth());
		    			Log.d(TAG, "canvas height: " + canvas.getHeight());
		    		}*/
                    int left = 1, top = 1, right = canvas.getWidth() - 1, bottom = canvas.getHeight() - 1;
                    if (crop_ratio > preview.getTargetRatio())
                    {
                        // crop ratio is wider, so we have to crop top/bottom
                        double new_hheight = ((double) canvas.getWidth()) / (2.0f * crop_ratio);
                        top = (int) (canvas.getHeight() / 2 - (int) new_hheight);
                        bottom = (int) (canvas.getHeight() / 2 + (int) new_hheight);
                    } else
                    {
                        // crop ratio is taller, so we have to crop left/right
                        double new_hwidth = (((double) canvas.getHeight()) * crop_ratio) / 2.0f;
                        left = (int) (canvas.getWidth() / 2 - (int) new_hwidth);
                        right = (int) (canvas.getWidth() / 2 + (int) new_hwidth);
                    }
                    canvas.drawRect(left, top, right, bottom, p);
                }
                p.setStyle(Paint.Style.FILL); // reset
            }
        }

        canvas.save();
        canvas.rotate(ui_rotation, canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f);

        int text_y = (int) (20 * scale + 0.5f); // convert dps to pixels
        // fine tuning to adjust placement of text with respect to the GUI, depending on orientation
        int text_base_y = 0;
        if (ui_rotation == (ui_placement_right ? 0 : 180))
        {
            text_base_y = canvas.getHeight() - (int) (0.5 * text_y);
        } else if (ui_rotation == (ui_placement_right ? 180 : 0))
        {
            text_base_y = canvas.getHeight() - (int) (2.5 * text_y); // leave room for GUI icons
        } else if (ui_rotation == 90 || ui_rotation == 270)
        {
            //text_base_y = canvas.getHeight() + (int)(0.5*text_y);
            ImageView view = (ImageView) main_activity.findViewById(R.id.take_photo);
            // align with "top" of the take_photo button, but remember to take the rotation into account!
            view.getLocationOnScreen(gui_location);
            int view_left = gui_location[0];
            preview.getView().getLocationOnScreen(gui_location);
            int this_left = gui_location[0];
            int diff_x = view_left - (this_left + canvas.getWidth() / 2);
    		/*if( MyDebug.LOG ) {
    			Log.d(TAG, "view left: " + view_left);
    			Log.d(TAG, "this left: " + this_left);
    			Log.d(TAG, "canvas is " + canvas.getWidth() + " x " + canvas.getHeight());
    		}*/
            int max_x = canvas.getWidth();
            if (ui_rotation == 90)
            {
                // so we don't interfere with the top bar info (datetime, free memory, ISO)
                max_x -= (int) (2.5 * text_y);
            }
            if (canvas.getWidth() / 2 + diff_x > max_x)
            {
                // in case goes off the size of the canvas, for "black bar" cases (when preview aspect ratio != screen aspect ratio)
                diff_x = max_x - canvas.getWidth() / 2;
            }
            text_base_y = canvas.getHeight() / 2 + diff_x - (int) (0.5 * text_y);
        }
        final int top_y = (int) (5 * scale + 0.5f); // convert dps to pixels
        final int location_size = (int) (20 * scale + 0.5f); // convert dps to pixels

        final String ybounds_text = getContext().getResources().getString(R.string.zoom) + getContext().getResources().getString(R.string.angle) + getContext().getResources().getString(R.string.direction);
        final double close_angle = 1.0f;
        if (camera_controller != null && !preview.isPreviewPaused())
        {
			/*canvas.drawText("PREVIEW", canvas.getWidth() / 2,
					canvas.getHeight() / 2, p);*/
            if (preview.isVideoRecording())
            {
                long video_time = preview.getVideoTime();
                String time_s = getTimeStringFromSeconds(video_time / 1000);
            	/*if( MyDebug.LOG )
					Log.d(TAG, "video_time: " + video_time + " " + time_s);*/
                p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
                p.setTextAlign(Paint.Align.CENTER);
                int pixels_offset_y = 3 * text_y; // avoid overwriting the zoom, and also allow a bit extra space
                int color = Color.rgb(244, 67, 54); // Red 500
                applicationInterface.drawTextWithBackground(canvas, p, time_s, color, Color.BLACK, canvas.getWidth() / 2, text_base_y - pixels_offset_y);
            }
        } else if (camera_controller == null)
        {
			/*if( MyDebug.LOG ) {
				Log.d(TAG, "no camera!");
				Log.d(TAG, "width " + canvas.getWidth() + " height " + canvas.getHeight());
			}*/
            p.setColor(Color.WHITE);
            p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
            p.setTextAlign(Paint.Align.CENTER);
            int pixels_offset = (int) (20 * scale + 0.5f); // convert dps to pixels
            if (preview.hasPermissions())
            {
                canvas.drawText(getContext().getResources().getString(R.string.failed_to_open_camera_1), canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f, p);
                canvas.drawText(getContext().getResources().getString(R.string.failed_to_open_camera_2), canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f + pixels_offset, p);
                canvas.drawText(getContext().getResources().getString(R.string.failed_to_open_camera_3), canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f + 2 * pixels_offset, p);
            } else
            {
                canvas.drawText(getContext().getResources().getString(R.string.no_permission), canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f, p);
            }
            //canvas.drawRect(0.0f, 0.0f, 100.0f, 100.0f, p);
            //canvas.drawRGB(255, 0, 0);
            //canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
        }
        if (camera_controller != null && sharedPreferences.getBoolean(PreferenceKeys.getShowISOPreferenceKey(), true))
        {
            p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
            p.setTextAlign(Paint.Align.LEFT);
            int location_x = (int) (50 * scale + 0.5f); // convert dps to pixels
            int location_y = top_y + (int) (32 * scale + 0.5f); // convert dps to pixels
            //int location_y2 = top_y + (int) (48 * scale + 0.5f); // convert dps to pixels
            if (ui_rotation == 90 || ui_rotation == 270)
            {
                int diff = canvas.getWidth() - canvas.getHeight();
                location_x += diff / 2;
                location_y -= diff / 2;
                //location_y2 -= diff/2;
            }
            if (ui_rotation == 90)
            {
                location_y = canvas.getHeight() - location_y - location_size;
                //location_y2 = canvas.getHeight() - location_y2 - location_size;
            }
            if (ui_rotation == 180)
            {
                location_x = canvas.getWidth() - location_x;
                p.setTextAlign(Paint.Align.RIGHT);
            }
            String string = "";
            if (camera_controller.captureResultHasIso())
            {
                int iso = camera_controller.captureResultIso();
                if (string.length() > 0)
                    string += " ";
                string += preview.getISOString(iso);
            }
            if (camera_controller.captureResultHasExposureTime())
            {
                long exposure_time = camera_controller.captureResultExposureTime();
                if (string.length() > 0)
                    string += " ";
                string += preview.getExposureTimeString(exposure_time);
            }
			/*if( camera_controller.captureResultHasFrameDuration() ) {
				long frame_duration = camera_controller.captureResultFrameDuration();
				if( string.length() > 0 )
					string += " ";
				string += preview.getFrameDurationString(frame_duration);
			}*/
            if (string.length() > 0)
            {
                int text_color = Color.rgb(255, 235, 59); // Yellow 500
                if (camera_controller.captureResultIsAEScanning())
                {
                    // we only change the color if ae scanning is at least a certain time, otherwise we get a lot of flickering of the color
                    if (ae_started_scanning_ms == -1)
                    {
                        ae_started_scanning_ms = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - ae_started_scanning_ms > 500)
                    {
                        text_color = Color.rgb(244, 67, 54); // Red 500
                    }
                } else
                {
                    ae_started_scanning_ms = -1;
                }
                applicationInterface.drawTextWithBackground(canvas, p, string, text_color, Color.BLACK, location_x, location_y, true, ybounds_text, true);
            }
			/*if( camera_controller.captureResultHasFocusDistance() ) {
				float dist_min = camera_controller.captureResultFocusDistanceMin();
				float dist_max = camera_controller.captureResultFocusDistanceMin();
				string = preview.getFocusDistanceString(dist_min, dist_max);
				applicationInterface.drawTextWithBackground(canvas, p, string, Color.rgb(255, 235, 59), Color.BLACK, location_x, location_y2, true, ybounds_text, true); // Yellow 500
			}*/
        }
        if (preview.supportsZoom() && camera_controller != null )
        {
            float zoom_ratio = preview.getZoomRatio();
            // only show when actually zoomed in
            if (zoom_ratio > 1.0f + 1.0e-5f)
            {
                // Convert the dps to pixels, based on density scale
                int pixels_offset_y = text_y;
                p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
                p.setTextAlign(Paint.Align.CENTER);
                applicationInterface.drawTextWithBackground(canvas, p, getContext().getResources().getString(R.string.zoom) + ": " + zoom_ratio + "x", Color.WHITE, Color.BLACK, canvas.getWidth() / 2, text_base_y - pixels_offset_y, false, ybounds_text, true);
            }
        }

        canvas.restore();

        if (camera_controller != null && continuous_focus_moving)
        {
            long dt = System.currentTimeMillis() - continuous_focus_moving_ms;
            final long length = 1000;
            if (dt <= length)
            {
                float frac = ((float) dt) / (float) length;
                float pos_x = canvas.getWidth() / 2.0f;
                float pos_y = canvas.getHeight() / 2.0f;
                float min_radius = (float) (40 * scale + 0.5f); // convert dps to pixels
                float max_radius = (float) (60 * scale + 0.5f); // convert dps to pixels
                float radius = 0.0f;
                if (frac < 0.5f)
                {
                    float alpha = frac * 2.0f;
                    radius = (1.0f - alpha) * min_radius + alpha * max_radius;
                } else
                {
                    float alpha = (frac - 0.5f) * 2.0f;
                    radius = (1.0f - alpha) * max_radius + alpha * min_radius;
                }
				/*if( MyDebug.LOG ) {
					Log.d(TAG, "dt: " + dt);
					Log.d(TAG, "radius: " + radius);
				}*/
                p.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(pos_x, pos_y, radius, p);
                p.setStyle(Paint.Style.FILL); // reset
            } else
            {
                continuous_focus_moving = false;
            }
        }

        if (preview.isFocusWaiting() || preview.isFocusRecentSuccess() || preview.isFocusRecentFailure())
        {
            long time_since_focus_started = preview.timeSinceStartedAutoFocus();
            float min_radius = (float) (40 * scale + 0.5f); // convert dps to pixels
            float max_radius = (float) (45 * scale + 0.5f); // convert dps to pixels
            float radius = min_radius;
            if (time_since_focus_started > 0)
            {
                final long length = 500;
                float frac = ((float) time_since_focus_started) / (float) length;
                if (frac > 1.0f)
                    frac = 1.0f;
                if (frac < 0.5f)
                {
                    float alpha = frac * 2.0f;
                    radius = (1.0f - alpha) * min_radius + alpha * max_radius;
                } else
                {
                    float alpha = (frac - 0.5f) * 2.0f;
                    radius = (1.0f - alpha) * max_radius + alpha * min_radius;
                }
            }
            int size = (int) radius;

            if (preview.isFocusRecentSuccess())
                p.setColor(Color.rgb(20, 231, 21)); // Green A400
            else if (preview.isFocusRecentFailure())
                p.setColor(Color.rgb(244, 67, 54)); // Red 500
            else
                p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            int pos_x = 0;
            int pos_y = 0;
            if (preview.hasFocusArea())
            {
                Pair<Integer, Integer> focus_pos = preview.getFocusPos();
                pos_x = focus_pos.first;
                pos_y = focus_pos.second;
            } else
            {
                pos_x = canvas.getWidth() / 2;
                pos_y = canvas.getHeight() / 2;
            }
            float frac = 0.5f;
            // horizontal strokes
            canvas.drawLine(pos_x - size, pos_y - size, pos_x - frac * size, pos_y - size, p);
            canvas.drawLine(pos_x + frac * size, pos_y - size, pos_x + size, pos_y - size, p);
            canvas.drawLine(pos_x - size, pos_y + size, pos_x - frac * size, pos_y + size, p);
            canvas.drawLine(pos_x + frac * size, pos_y + size, pos_x + size, pos_y + size, p);
            // vertical strokes
            canvas.drawLine(pos_x - size, pos_y - size, pos_x - size, pos_y - frac * size, p);
            canvas.drawLine(pos_x - size, pos_y + frac * size, pos_x - size, pos_y + size, p);
            canvas.drawLine(pos_x + size, pos_y - size, pos_x + size, pos_y - frac * size, p);
            canvas.drawLine(pos_x + size, pos_y + frac * size, pos_x + size, pos_y + size, p);
            p.setStyle(Paint.Style.FILL); // reset
        }

        CameraController.Face[] faces_detected = preview.getFacesDetected();
        if (faces_detected != null)
        {
            p.setColor(Color.rgb(255, 235, 59)); // Yellow 500
            p.setStyle(Paint.Style.STROKE);
            for (CameraController.Face face : faces_detected)
            {
                // Android doc recommends filtering out faces with score less than 50 (same for both Camera and Camera2 APIs)
                if (face.score >= 50)
                {
                    face_rect.set(face.rect);
                    preview.getCameraToPreviewMatrix().mapRect(face_rect);
					/*int eye_radius = (int) (5 * scale + 0.5f); // convert dps to pixels
					int mouth_radius = (int) (10 * scale + 0.5f); // convert dps to pixels
					float [] top_left = {face.rect.left, face.rect.top};
					float [] bottom_right = {face.rect.right, face.rect.bottom};
					canvas.drawRect(top_left[0], top_left[1], bottom_right[0], bottom_right[1], p);*/
                    canvas.drawRect(face_rect, p);
					/*if( face.leftEye != null ) {
						float [] left_point = {face.leftEye.x, face.leftEye.y};
						cameraToPreview(left_point);
						canvas.drawCircle(left_point[0], left_point[1], eye_radius, p);
					}
					if( face.rightEye != null ) {
						float [] right_point = {face.rightEye.x, face.rightEye.y};
						cameraToPreview(right_point);
						canvas.drawCircle(right_point[0], right_point[1], eye_radius, p);
					}
					if( face.mouth != null ) {
						float [] mouth_point = {face.mouth.x, face.mouth.y};
						cameraToPreview(mouth_point);
						canvas.drawCircle(mouth_point[0], mouth_point[1], mouth_radius, p);
					}*/
                }
            }
            p.setStyle(Paint.Style.FILL); // reset
        }
    }
}
