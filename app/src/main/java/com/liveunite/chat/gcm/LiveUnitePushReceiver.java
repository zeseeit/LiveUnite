package com.liveunite.chat.gcm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.gson.JsonArray;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.activities.HomeActivity;
import com.liveunite.chat.activities.ChatRoom;
import com.liveunite.chat.activities.ChatWall;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.ModelToJsonConverter;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.helper.ForegroundCheckTask;
import com.liveunite.chat.helper.LiveUniteTime;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.chat.model.LiveUniteMessage;
import com.liveunite.chat.model.PushMessageDeliveryModel;
import com.liveunite.infoContainer.Singleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.liveunite.activities.HomeActivity.homeActivity;

/**
 * Created by Ankit on 12/7/2016.
 */

public class LiveUnitePushReceiver extends GcmListenerService {

    private static final String TAG = LiveUnitePushReceiver.class.getSimpleName();
    private LiveUniteNotificationUtils notificationUtils;
    private int REPORT_SEND_TIMEOUT = 60 * 1000;
    boolean isForeground = false;

    @Override
    public void onMessageReceived(String from, Bundle data) {

        isForeground = new ForegroundCheckTask().isForeground(this);

        //todo: handle the message received
        Log.e(TAG, "Message From " + from);
        int PUSH_TYPE = Integer.parseInt(data.getString("push_type"));
        String payload = data.getString("payload");

        switch (PUSH_TYPE) {

            case Constants.PUSH.TYPE_NEW_MESSAGE:
                Log.d(TAG, "handling New Message Arrival");
                submitNewArrivalActionToCenter(payload);

                break;
            case Constants.PUSH.TYPE_MESSAGE_DELIVERED:

                Log.d(TAG, "handling Sent Message Delivery");
                handleMessageDelivered(payload);

                break;
            case Constants.PUSH.TYPE_MESSAGE_SEEN:

                Log.d(TAG, "handling Sent Message Seen");
                handleMessageSeen(payload);

                break;
            case Constants.PUSH.TYPE_TOKEN_REFRESH:

                handleTokenRefresh(payload);

                break;
            case Constants.PUSH.TYPE_TYPING:

                Log.d(TAG, "handling Typing....info");
                handleTyping(payload);

                break;
            default:
                break;
        }

        Log.d("ChatBox", " message received " + data.getString("payload"));
        super.onMessageReceived(from, data);

    }

    private void handleTyping(String payload) {
        //WID: locally broadcast the typing event
        String senderId = "";
        try {
            JSONObject object = new JSONObject(payload);
            senderId = object.getString("sender_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent typingInfo = new Intent(Config.ACTION_TYPING_INFO);
        typingInfo.putExtra("senderId", senderId);
        sendBroadcast(typingInfo);

    }

    private void handleTokenRefresh(String payload) {
        //WID: sends action to refresh the token to chat center
        ChatCentre.getInstance(this).submitAction(ChatCentre.FLAG_SYNC_TOKEN);

    }

    //as [SENDER]
    // called when our sent message is seen by target receiver
    private void handleMessageSeen(String payload) {

        //WID:  send corresponding action to chat center

        try {
            JSONObject object = new JSONObject(payload);

            LiveUniteMessage updatedMessageWithDelivery = DatabaseHelper
                    .getInstance(this)
                    .getMessage(object.getString("msg_id"), Constants.CHAT.MESSAGE_TYPE_OUTGOING);

            if (updatedMessageWithDelivery != null) {
                updatedMessageWithDelivery.setIsSeen("TRUE");
                updatedMessageWithDelivery.setIsDelivered("TRUE");  // incase it is left out
                updatedMessageWithDelivery.setSeenTime(object.getString("seen_time"));
                ChatCentre.getInstance(this).submitAction(ChatCentre.FLAG_RELOCATION_INVALIDATION, updatedMessageWithDelivery, Constants.CHAT.FLAG_RELOCATION_TYPE_SEEN);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //as [SENDER]
    // called when our sent message is delivered to target receiver
    private void handleMessageDelivered(String payload) {
        //WID:  send corresponding action to chat center

        try {
            JSONObject object = new JSONObject(payload);

            LiveUniteMessage updatedMessageWithDelivery = DatabaseHelper
                    .getInstance(this)
                    .getMessage(object.getString("msg_id"), Constants.CHAT.MESSAGE_TYPE_OUTGOING);

            if (updatedMessageWithDelivery != null) {

                updatedMessageWithDelivery.isDelivered = "TRUE";
                updatedMessageWithDelivery.receivedTime = object.getString("delivered_time");

                ChatCentre.getInstance(this).submitAction(
                        ChatCentre.FLAG_RELOCATION_INVALIDATION,
                        updatedMessageWithDelivery,
                        Constants.CHAT.FLAG_RELOCATION_TYPE_DELIVERED);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //as [RECEIVER]
    private void respondToSenderViaGcmForDelivery(LiveUniteMessage msg) {
        //WID: as response: direct gcm to send delivery report to sender

        // to : the sender of message
        String to = DatabaseHelper.getInstance(this).getGcmID(msg.senderId);
        Log.d(TAG, "to=>" + to);
        // msg_id: same as message
        String msg_id = msg.chatId;
        // time: current time
        String deliveryTime = LiveUniteTime.getInstance().getDateTime();
        PushMessageDeliveryModel deliveryModel = new PushMessageDeliveryModel(to, msg_id, deliveryTime);
        sendDeliveryReportToGCM(new ModelToJsonConverter().getMessageDeliveryModelJson(deliveryModel));

    }

    private void sendDeliveryReportToGCM(final String jsonBody) {

        StringRequest sendChatReq = new StringRequest(Request.Method.POST,
                Constants.SERVER.URL_GCM_SERVER_SEND_MESSAGE,

                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "response " + response);
                        try {
                            //response from gcm server
                            //{
                            // "multicast_id":5841215299712553614,
                            // "success":1,
                            // "failure":0,
                            // "canonical_ids":0,
                            // "results":[
                            //              {"message_id":"0:1481704417127836%1a9c01c8f9fd7ecd"}
                            //           ]
                            // }
                            JSONObject object = new JSONObject(response);
                            if (object.getInt("success") == 1) {
                                // submit a no-callback Action Flag to Chat Center For MessageRelocation

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
                REPORT_SEND_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(sendChatReq, "sendDeliveryReportReq", getApplicationContext());


    }

    private void submitNewArrivalActionToCenter(String response) {

        LiveUniteMessage newMsg = null;
        try {

            JSONObject object = new JSONObject(response);

            String selfId = LiveUnite.getInstance().getPreferenceManager().getFbId(); // fbId as user id
            String chatRoomId = createChatId(selfId, object.getString("sender_id"),object.getString("type"));
            String receivedTime = LiveUniteTime.getInstance().getDateTime();

            Log.d("PushReceiver", "ChatRoomId=>" + chatRoomId + " SenderId =>" + object.getString("sender_id"));

            //Check for user commChannel [Blockings]
            if (DatabaseHelper.getInstance(this).getCommChannel(object.getString("sender_id")).equals(Constants.USER.VALUE_COMMUNICATION_CHANNEL_BLOCKED)) {
                Log.d(TAG, "msg from blocked user");
                return;
            }

            String BLANK_FOR_NOW = "";
            String selfTitle = LiveUnite.getInstance().getPreferenceManager().getUserTitle();
            newMsg = new LiveUniteMessage(
                    Constants.CHAT.MESSAGE_TYPE_INCOMMING,
                    object.getString("msg_id"),         // chatId created at the sender end
                    chatRoomId,
                    object.getString("sender_id"),
                    selfTitle,                                                  // sender_id of sender
                    object.getString("sender_title"),
                    object.getString("sender_dp_url"),
                    selfId,                             // receiver id
                    object.getString("sent_time"),      // sent time of sender device
                    receivedTime,                       // now
                    BLANK_FOR_NOW,  // SEEN TIME
                    object.getString("msg"),
                    "TRUE",     // IS-SENT
                    "FALSE",    // IS-SEEN
                    "TRUE"      // IS-DELIVERED
            );

            respondToSenderViaGcmForDelivery(newMsg);

            if (!isForeground) {

                Log.d("LiveUnitePushReceiver", " App Backgrounded.....Showing Notifications");
                Intent chatWallIntent = new Intent(this, ChatWall.class);
                chatWallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//                Intent chatRoomIntent = new Intent(this, ChatWall.class);
//                Bundle bundle = new Bundle();
//                bundle.putString("fbId",newMsg.senderId);
//                bundle.putString("title",newMsg.senderTitle);
//                String dpUrl = "https://graph.facebook.com/"+newMsg.senderId+"/picture";
//                bundle.putString("dpUrl",dpUrl);
//                chatRoomIntent.putExtras(bundle);
//                chatRoomIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                new LiveUniteNotificationUtils(this).showNotificationMessage("LiveUnite", newMsg.senderTitle + " : " + newMsg.message, newMsg.sentTime, chatWallIntent);

            } else {
                try {
                    homeActivity.updateToolbar();
                } catch (Exception e) {
                    Log.d("PushReceiver", "something went wrong.Cannot Update Toolbar Data");
                }
            }

            ChatCentre.getInstance(this).submitAction(ChatCentre.FLAG_ARRIVAL_NEW, newMsg);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private String createChatId(String selfId, String companionId,String type) {

        String HelpQueryPrefix = type.equals(Constants.CHAT.TYPE_QUERY) ? "HelpRoomQuery":"ChatCommons";
        return "cr_"+ HelpQueryPrefix+"_"+ selfId + companionId;
    }

}
