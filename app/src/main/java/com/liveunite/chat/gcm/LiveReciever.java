package com.liveunite.chat.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Created by Ankit on 1/8/2017.
 */

public class LiveReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("gcm_debug", "PushReceiver onReceive called");

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

        String msgType = gcm.getMessageType(intent);

        if(!extras.isEmpty()){
            if(GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(msgType)){
                Log.i("gcm_debug", "Message send error");
            }else if(GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(msgType)){
                Log.i("gcm_debug", "Message deleted");
            }else if(GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(msgType)){
                Log.i("gcm_debug", "Message received : " + extras.toString());
            }
        }
    }
}
