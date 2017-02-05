package com.liveunite.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.liveunite.LiveUniteMains.LiveUnite;

/**
 * CheckInternetConnection Class is used to check Internet connection
 */
public class CheckInternetConnection {

    private Context context;

    public CheckInternetConnection(Context mContext) {
        context = mContext;
    }

    /**
     * @return isConnectedToInternet?(), is used to check Internet Connectivity by using Connection Manager.
     * it will return true if it is connected and false if not connected
     */
    public boolean isConnectedToInternet() {
        if (checkConnectivity()) {
            return true;//isOnline(Constant.PING_SITE);
        }
        return false;
    }

    public Boolean isOnline(String site) {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 "+site);
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal==0);
            return reachable;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Toast.makeText(context,"Please make sure you have active Internet connection",Toast.LENGTH_LONG).show();
        return false;
    }

    private boolean checkConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void showDialog() {

       final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("No Internet Connectivity Found");
        alertDialogBuilder
                .setMessage("Move to internet connection settings?")
                .setCancelable(false)
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                        ((Activity) context).finish();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        context.startActivity(intent);
                    }
                });

        new TextView(LiveUnite.getInstance().getApplicationContext()).post(new Runnable() {
            @Override
            public void run() {

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

    }
}