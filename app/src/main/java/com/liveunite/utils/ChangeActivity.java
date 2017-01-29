package com.liveunite.utils;

import android.content.Context;
import android.content.Intent;

import com.liveunite.activities.PicturePreview;

/**
 * Created by Vishwesh on 29-09-2016.
 */

public class ChangeActivity {

    public void change(Context context, Class activity) {
        Intent intent = new Intent(context,activity);
        context.startActivity(intent);
    }

    public static void sendToPreview(Context context, String filename, int type, boolean auto_delete)
    {
        Intent intent = new Intent(context,PicturePreview.class);
        intent.putExtra(Constant.PREVIEW_FILENAME,filename);
        intent.putExtra(Constant.UPLOAD_TYPE,type);
        intent.putExtra(Constant.UPLOAD_AUTODELETE,auto_delete);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }


}
