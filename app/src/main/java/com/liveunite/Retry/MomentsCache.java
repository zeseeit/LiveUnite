package com.liveunite.Retry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.liveunite.chat.config.Constants;
import com.liveunite.chat.gcm.LiveUnitePreferenceManager;
import com.liveunite.chat.helper.Segmentor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Ankit on 3/12/2017.
 */

public class MomentsCache {

    private LiveUnitePreferenceManager preferences;
    private Context context;
    private MomentsCache mInstance;

    public MomentsCache(Context context) {
        this.context = context;
        preferences = new LiveUnitePreferenceManager(context);
        File file = new File(Constants.RETRY_MOMENTS.DIR_CACHE_MOMENTS);

        if(!file.exists())
            file.mkdirs();
    }

    public MomentsCache getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MomentsCache(context);
        }
        return mInstance;
    }

    public void cacheMoment(File _file){
       String myDir =  Constants.RETRY_MOMENTS.DIR_CACHE_MOMENTS;
        File file = new File(myDir, _file.getName());

        Log.d("MomentCache"," written cache to "+file.getAbsolutePath());

        Bitmap b = BitmapFactory.decodeFile(_file.getAbsolutePath());

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            b.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public Bitmap getCachedMoment(String file_name){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(Constants.RETRY_MOMENTS.DIR_CACHE_MOMENTS+"/"+file_name, options);
    }

    public void setCacheMetaData( MomentCacheModel cacheModel){


        ArrayList<String> items = new Segmentor().getParts(preferences.getCachedItemNames(),'#');
        for(String i:items){
            if(i.equals(cacheModel.file_name))
                return;
        }

        preferences.setCacheItemName(preferences.getCachedItemNames()+"#"+cacheModel.file_name);
        Log.d("MomentsCache"," items "+preferences.getCachedItemNames());
        preferences.setCacheLatitude(cacheModel.file_name,cacheModel.latitude);
        preferences.setCacheLongitude(cacheModel.file_name,cacheModel.longitude);
        preferences.setCacheCaption(cacheModel.file_name,cacheModel.caption);
    }

    public MomentCacheModel getCacheModel(String file_name){
        return new MomentCacheModel(file_name,preferences.getCacheLatitude(file_name),preferences.getCacheLongitude(file_name),preferences.getCacheCaption(file_name));
    }

    public void clearCache(String file_name) {

        File fdelete = new File(Constants.RETRY_MOMENTS.DIR_CACHE_MOMENTS + "/" + file_name);
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.d("MomentsRetry", "deleted cached");
            }

            removeCacheItem(file_name);

            preferences.setCacheLatitude(file_name, "");
            preferences.setCacheLongitude(file_name, "");
            preferences.setCacheCaption(file_name, "");
        }
    }

    public void removeCacheItem(String file_name){

        ArrayList<String> items = new Segmentor().getParts(preferences.getCachedItemNames(),'#');
        preferences.setCacheItemName("");
        for(String t : items){
            if(t.equals(file_name)){
                continue;
            }else{
                preferences.setCacheItemName(preferences.getCachedItemNames()+"#"+t);
            }
        }

    }

}
