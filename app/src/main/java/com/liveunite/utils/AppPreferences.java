package com.liveunite.utils;

/**
 * Created by vishwesh on 2/4/16.
 */
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Shared Prefences saved in local files
 */
public class AppPreferences {

    public static final String PREF_NAME = "liveUnite";

    @SuppressWarnings("deprecation")
    public static final int MODE = Context.MODE_PRIVATE;

    private String KEY_LOG_IN="isLogedIn";




    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, MODE);
    }
    private static SharedPreferences.Editor getEditor(Context context) {
        return getPreferences(context).edit();
    }




    /*public boolean isLognedIn(Context context) {
        return getPreferences(context).getBoolean(KEY_LOG_IN, false);
    }
    public void setLognedIn(Context context,Boolean addOrNot){
        getEditor(context).putBoolean(KEY_LOG_IN , addOrNot).commit();
    }
*/



}
