package com.liveunite.utils;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by Vishwesh on 08-10-2016.
 */

public class ShowProgressDialog {
    ProgressDialog progressDialog;

    public void create(Context context)
    {
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Please wait while loading");
    }

    public void show(){
        progressDialog.show();
    }

    public void dismiss(){
        progressDialog.dismiss();
    }
}
