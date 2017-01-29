package com.liveunite.chat.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.ModelToJsonConverter;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.helper.LiveUniteTime;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.chat.model.LiveUniteMessage;
import com.liveunite.chat.model.PushMessageModel;
import com.liveunite.utils.CheckInternetConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ankit on 12/6/2016.
 */

public class MessageDispatchService extends Service {

    private static final String TAG = MessageDispatchService.class.getSimpleName();

    private DatabaseHelper mDbHelper;
    private ChatCentre mChatCenter;
    private int CHAT_SEND_TIMEOUT = 20 * 1000;      // 20 sec
    private static boolean DISPATCHING = false;
    private int CHAT_DISPATCH_TASK_INTERVAL = 10000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Timer mTimer;
    private TimerTask dispatcherTask = new TimerTask() {
        @Override
        public void run() {
            Log.d("MessageDispatch", "dispatcher Task Running...");

            VolleyUtils.getInstance().cancelPendingRequests("dispatch460chatReq");
           dispatchPendings();


        }
    };

    @Override
    public void onCreate() {

        super.onCreate();
        Log.d(TAG, "onCreate()");
        mDbHelper = DatabaseHelper.getInstance(this);
        mChatCenter = ChatCentre.getInstance(this);

        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(dispatcherTask, 0, CHAT_DISPATCH_TASK_INTERVAL);
        }

    }

    private void dispatchPendings() {
        //WID: reads all unsent messages from database and send to server
        // and reflect back the success to database

        ArrayList<LiveUniteMessage> messages = mDbHelper.getPendings();

        VolleyUtils.getInstance().cancelPendingRequests("dispatch460chatReq");

        for (LiveUniteMessage msg : messages) {

            send(msg);
            logChat(msg);
            Log.d(TAG, "sending... msgID " + msg.chatId);

        }
    }

    private void send(final LiveUniteMessage msg) {

        String receiver_id = DatabaseHelper.getInstance(this).getGcmID(msg.receiverId);

        final String jsonBody = (new ModelToJsonConverter()).getNewMessagePushModelJson(new PushMessageModel(
                receiver_id,
                Constants.PUSH.TYPE_NEW_MESSAGE,
                msg.senderId,
                msg.senderTitle,
                msg.senderDpUrl,
                msg.message,
                msg.sentTime,
                msg.chatId
        ));

        StringRequest sendChatReq = new StringRequest(Request.Method.POST,
                Constants.SERVER.URL_GCM_SERVER_SEND_MESSAGE,

                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d(TAG, " response " + response);
                        try {

                            JSONObject object = new JSONObject(response);
                            if (object.getInt("success") == 1) {

                                // submit a no-callback Action Flag to Chat Center For MessageRelocation
                                LiveUniteMessage tempMessage = msg;
                                tempMessage.isSent = "TRUE";
                                tempMessage.sentTime = LiveUniteTime.getInstance().getDateTime();

                                mChatCenter.submitAction(
                                        ChatCentre.FLAG_RELOCATION_INVALIDATION,
                                        tempMessage,
                                        Constants.CHAT.FLAG_RELOCATION_TYPE_SENT);


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "volley Error " + error);
                    }
                }) {

            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.d(TAG, " body => " + jsonBody);
                return jsonBody.getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headerMap = new HashMap<>();
                headerMap.put(Constants.SERVER.KEY_AUTHORIZATION, Constants.SERVER.GOOGLE_SERVER_KEY);
                headerMap.put(Constants.SERVER.KEY_CONTENT_TYPE, "application/json");
                return headerMap;
            }
        };

        sendChatReq.setRetryPolicy(new DefaultRetryPolicy(
                CHAT_SEND_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(sendChatReq, "dispatch460chatReq", getApplicationContext());

    }

    private void logChat(final LiveUniteMessage message) {

        Log.d("MessageDispatch"," chatID "+message.chatId);

        StringRequest msgLogReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_UPLOAD_CHAT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("Chat Upload ", "On Rep:- " + response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ChatUpload", "" + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                HashMap<String, String> map = new HashMap<>();
                map.put("user_id", message.senderId);
                map.put("rec_id", message.receiverId);
                map.put("time", message.sentTime);
                map.put("msg", message.message);
                map.put("msg_id", message.chatId);
                return map;
            }
        };

        msgLogReq.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(msgLogReq, "msgLogReq", this);


    }


}
