package com.liveunite.chat.gcm;

import android.content.Context;
import android.content.SharedPreferences;

import com.liveunite.chat.config.Constants;

/**
 * Created by Ankit on 12/7/2016.
 */

public class LiveUnitePreferenceManager {
    private String TAG = LiveUnitePreferenceManager.class.getSimpleName();

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context mContext;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "live_unite_pref";

    // All Shared Preferences Keys
    private static final String KEY_GCM_TOKEN = "gcm_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_NOTIFICATIONS = "notifications";

    // Constructor
    public LiveUnitePreferenceManager(Context context) {
        this.mContext = context;
        pref = mContext.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public boolean homeIconCreated(){
        return pref.getBoolean("isHomeCreated",false);
    }

    public void setHomeIconCreated(boolean created){
        editor.putBoolean("isHomeCreated",created);
    }

    public void clearNotifications() {
        editor.putString(KEY_NOTIFICATIONS, "");
        editor.commit();
    }

    public void addNotification(String notification) {

        // get old notifications
        String oldNotifications = getNotifications();

        if (oldNotifications != null) {
            oldNotifications += "|" + notification;
        } else {
            oldNotifications = notification;
        }

        editor.putString(KEY_NOTIFICATIONS, oldNotifications);
        editor.commit();
    }

    public String getNotifications() {
        return pref.getString(KEY_NOTIFICATIONS, null);
    }

    public void setUserID(String user_id) {
        editor.putString(Constants.USER.KEY_USER_ID, user_id);
        editor.commit();
    }

    public String getUserID() {
        return pref.getString(Constants.USER.KEY_USER_ID, "");
    }

    public void setGcmToken(String token) {
        editor.putString(KEY_GCM_TOKEN, token);
        editor.commit();
    }

    public String getGcmToken() {
        return pref.getString(KEY_GCM_TOKEN, "");
    }

    public void clear() {
        editor.clear();
        editor.commit();
    }

    public void setDeviceRegistered(boolean b) {
        editor.putBoolean("deviceRegistered", b);
        editor.commit();
    }

    public boolean isDeviceRegistered() {
        return pref.getBoolean("deviceRegistered", false);
    }

    public void setFbId(String id) {
        editor.putString("fbId", id);
        editor.commit();
    }

    public String getFbId() {
        return pref.getString("fbId", "");
    }

    public String getNextReportType() {
        return pref.getString("reportType", Constants.USER.KEY_REPORT_TYPE_ONLINE);
    }

    public void setNextReportType(String type) {
        editor.putString("reportType", type);
        editor.commit();
    }

    public String getUserTitle() {
        return pref.getString("selfTitle", "");
    }

    public void setUserTitle(String title) {
        editor.putString("selfTitle", title);
        editor.commit();
    }

    public String getNotifiedType() {
        return pref.getString("notifiedType", Constants.USER.KEY_REPORT_TYPE_LAST_SEEN);
    }


    public void setNotifiedType(String keyReportTypeOnline) {
        editor.putString("notifiedType", keyReportTypeOnline);
        editor.commit();
    }

    public boolean isNotificationMuted() {
        return pref.getBoolean("notificationMute", false);
    }

    public void muteNotification(boolean b) {
        editor.putBoolean("notificationMute", b);
        editor.commit();
    }

    public void setFirstName(String first_name) {
        editor.putString("fname", first_name);
        editor.commit();
    }

    public void setLastName(String last_name) {
        editor.putString("lname", last_name);
        editor.commit();
    }

    public void setPhone(String phone) {
        editor.putString("phone", phone);
        editor.commit();
    }

    public void setEmail(String email) {
        editor.putString("email", email);
        editor.commit();
    }

    public void setGender(String gender) {
        editor.putString("gender", gender);
        editor.commit();
    }

    public void setDob(String dateOfBirth) {
        editor.putString("dob", dateOfBirth);
        editor.commit();
    }

    public void setLat(String latitude) {

        editor.putString("lat", latitude);
        editor.commit();
    }

    public void setLong(String longitude) {
        editor.putString("lon", longitude);
        editor.commit();
    }

    public String getFirstname() {
        return pref.getString("fname", "");
    }

    public String getLastname() {
        return pref.getString("lname", "");
    }

    public String getPhone() {
        return pref.getString("phone", "");
    }


    public String getEmail() {
        return pref.getString("email", "");
    }


    public String getGender() {
        return pref.getString("gender", "");
    }


    public String getDob() {
        return pref.getString("dob", "");
    }

    public String getLat() {
        return pref.getString("lat", "0.0");
    }


    public String getLong() {
        return pref.getString("lon", "0.0");
    }

    public void setLiveUniteId(String id) {
        editor.putString("liveunite_id",id);
        editor.commit();
    }

    public String getLiveUnitId(){
        return pref.getString("liveunite_id","");
    }

    public void flushDetails() {
        setFbId("");
        setGcmToken("");
        setDob("");
        setEmail("");
        setFirstName("");
        setUserID("");
        setLastName("");
        setPhone("");
        setLiveUniteId("");
    }

    public String getBio() {
        return pref.getString("bio","");
    }

    public void setBio(String bio){
        editor.putString("bio",bio);
        editor.commit();
    }

    public void setMaxAge(float maxAge) {
        editor.putFloat("maxAge",maxAge);
        editor.commit();
    }

    public float getMaxAge(){
        return pref.getFloat("maxAge",99);
    }

    public void setMinAge(float minAge) {
        editor.putFloat("minAge",minAge);
        editor.commit();
    }

    public float getMinAge(){
        return pref.getFloat("minAge",-99);
    }


    public void setMaxDist(float maxDist){
        editor.putFloat("maxDist",maxDist);
        editor.commit();
    }

    public float getMaxDistance() {
        return pref.getFloat("maxDist",10000);
    }

    public void turnNotificationSound(boolean notificationSound) {
        editor.putBoolean("nsound",notificationSound);
        editor.commit();
    }

    public boolean getNotificationSoundChoice(){
        return pref.getBoolean("nsound",true);
    }

    public void setIsProfileOwner(boolean isOwner) {
        editor.putBoolean("profileOwner",isOwner);
        editor.commit();
    }
    public boolean isProfileOwner(){
        return pref.getBoolean("profileOwner",false);
    }

    public void setCacheLatitude(String file_name, String latitude) {
        editor.putString(file_name+"Lat",latitude);
        editor.commit();
    }

    public void setCacheLongitude(String file_name, String longitude) {
        editor.putString(file_name+"Lon",longitude);
        editor.commit();
    }

    public void setCacheCaption(String file_name, String caption) {
        editor.putString(file_name+"Cap",caption);
        editor.commit();
    }

    public String getCacheLatitude(String file_name) {
        return pref.getString(file_name+"Lat","");
    }

    public String getCacheLongitude(String file_name) {
        return pref.getString(file_name+"Lon","");
    }

    public String getCacheCaption(String file_name) {
        return pref.getString(file_name+"Cap","");
    }

    public String getCachedItemNames() {
        return pref.getString("cachedItems","");
    }

    public void setCacheItemName(String s) {
        editor.putString("cachedItems",s);
        editor.commit();
    }

    public String getReportedPosts() {
        return pref.getString("reportedPids","#");
    }

    public void setReportedPost(String s) {
        editor.putString("reportedPids",s);
        editor.commit();
    }
}
