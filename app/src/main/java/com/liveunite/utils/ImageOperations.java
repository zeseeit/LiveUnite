package com.liveunite.utils;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by arunkr on 11/10/16.
 */

public class ImageOperations
{
    public static Bitmap scaleDown(Bitmap realImage, float ratio, boolean filter)
    {
        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    public static String getPath(Activity activity, Uri uri)
    {
        // just some safety built in
        if( uri == null )
        {
            // TODO perform some logging or show user feedback
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        //Cursor cursor = managedQuery(uri, projection, null, null, null);
        Cursor cursor =  activity.getContentResolver().query(uri, projection, null, null, null);
        if( cursor != null )
        {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String data = cursor.getString(column_index);
            cursor.close();
            return data;
        }
        // this is our fallback here
        return uri.getPath();
    }
}
