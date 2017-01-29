package com.liveunite.chat.core;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.chat.activities.ChatRoom;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.LiveUniteTimeFormatter;
import com.liveunite.chat.converters.ModelToJsonConverter;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.gcm.Config;
import com.liveunite.chat.helper.LiveUniteTime;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.chat.model.ChatRoomModel;
import com.liveunite.chat.model.LiveUniteGcmRegistration;
import com.liveunite.chat.model.LiveUniteMessage;
import com.liveunite.chat.model.PushMessageSeenModel;
import com.liveunite.chat.service.MessageDispatchService;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.utils.Constant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ankit on 12/6/2016.
 */

public class ChatCentre {

    public final static int FLAG_KEEP_ALIVE = 101;
    public final static int FLAG_DELETE = 103;
    public final static int FLAG_SUBMIT = 104;
    public final static int FLAG_RELOCATION_INVALIDATION = 105;
    public final static int FLAG_SYNC = 102;
    public final static int FLAG_SYNC_TOKEN = 109;
    public final static int FLAG_ARRIVAL_NEW = 107;
    public final static int FLAG_ADD_CHAT_ROOM = 111;
    public final static int FLAG_DELETE_CHAT_ROOM = 112;
    public final static int FLAG_KEEP_LIVE_CHAT_WALL = 113;
    public final static int FLAG_CHAT_ROOM_INVALIDATION = 114;

    private final String TAG = ChatCentre.class.getSimpleName();

    private static Context context;
    private static ChatCentre mInstance;
    private Handler mHandler;
    private ChatRoomModel chatRoomModel;
    private LiveUniteMessage message;
    private int relocationType;
    private DatabaseHelper mDbHelper;
    private String chatRoomId;
    private DataBaseEventListener mDataBaseEventListener;
    private int SYNC_GCM_IDS_TIMEOUT_MS = 60 * 1000;    // 1 min
    private int REPORT_SEND_TIMEOUT = 60 * 1000;
    private boolean isHelpRoomQuery = false;


    public ChatCentre(Context context) {
        this.context = context;
        mDbHelper = DatabaseHelper.getInstance(context);
    }

    public static synchronized ChatCentre getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ChatCentre(context);
        }
        return mInstance;
    }

    //todo: stream line messages submitted to chat center
    public void submitAction(int flagSyncToken) {
        initTask(flagSyncToken);
    }

    public void submitAction(int actionType, Handler handler) {
        Log.d(TAG, "Action Invoke Type :" + actionType);
        this.mHandler = handler;
        initTask(actionType);

    }

    public void submitAction(int actionType, Handler handler, String chatRoomId) {
        Log.d(TAG, "Action Invoke Type :" + actionType);
        this.mHandler = handler;
        this.chatRoomId = chatRoomId;
        initTask(actionType);
    }

    // override for no-callback callers
    public void submitAction(int actionType, LiveUniteMessage msg) {
        Log.d(TAG, "Action Invoke Type :" + actionType);
        this.message = msg;
        initTask(actionType);
    }


    // override for no-callback callers
    public void submitAction(int actionType, ChatRoomModel chatRoomModel) {
        Log.d(TAG, "Action Invoke Type :" + actionType);
        this.chatRoomModel = chatRoomModel;
        initTask(actionType);
    }

    //override for no-callback callers
    public void submitAction(int actionType, LiveUniteMessage msg, int relocationType) {
        Log.d(TAG, "Action Invoke Type :" + actionType);
        this.message = msg;
        this.relocationType = relocationType;
        initTask(actionType);

    }

    public void submitAction(int actionType, Handler handler, LiveUniteMessage msg , boolean isHelpRoom) {
        Log.d(TAG, "Action Invoke Type :" + actionType);
        this.mHandler = handler;
        this.message = msg;
        this.isHelpRoomQuery = isHelpRoom;
        initTask(actionType);
    }

    public void submitAction(int actionType, Handler handler, LiveUniteMessage msg, int relocationType) {

        Log.d(TAG, "Action Invoke Type :" + actionType);
        this.mHandler = handler;
        this.message = msg;
        this.relocationType = relocationType;
        initTask(actionType);

    }

    private void initTask(final int actionType) {

        new Thread() {
            @Override
            public void run() {

                switch (actionType) {

                    case FLAG_KEEP_ALIVE:
                        keepAlive();
                        break;
                    case FLAG_SUBMIT:
                        submit();
                        break;
                    case FLAG_SYNC_TOKEN:
                        syncGcmIds();
                        break;
                    case FLAG_ARRIVAL_NEW:
                        handleNewArrival();
                    case FLAG_DELETE:
                        delete();
                        break;
                    case FLAG_RELOCATION_INVALIDATION:
                        invalidateMessageRelocations();
                        break;
                    case FLAG_ADD_CHAT_ROOM:
                        addChatRoom();
                        break;
                    case FLAG_KEEP_LIVE_CHAT_WALL:
                        keepChatWallLive();
                        break;
                    case FLAG_DELETE_CHAT_ROOM:
                        deleteChatRoom();
                        break;
                    case FLAG_CHAT_ROOM_INVALIDATION:
                        invalidateChatRooms();
                        break;
                    default:
                        break;
                }

            }
        }.start();

        Log.d(TAG, "started Thread For Action");

    }

    private void invalidateChatRooms() {
        //WID: update the chat room
        mDbHelper.updateChatRoom(chatRoomModel);
        Log.d(TAG, "update the chat room");

    }

    private void keepChatWallLive() {
        //WID: loads all the chat rooms and register for further events

        ArrayList<ChatRoomModel> chatRooms = mDbHelper.getChatRooms();

        //clear any old data in adapter if exists
        Message clearFlagMessage = Message.obtain();
        clearFlagMessage.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_CLEAR_OLD;
        mHandler.sendMessage(clearFlagMessage);


        for (ChatRoomModel chatRoom : chatRooms) {

            Message otmObj = Message.obtain();
            otmObj.arg1 = Constants.CHAT_ROOM.FLAG_ADDED_NEW;
            otmObj.obj = chatRoom;
            mHandler.sendMessage(otmObj);
        }

        Log.d(TAG, "Sent Last Cached Data");
        Log.d(TAG, "Registered for onward event");


        registerEventListenerForDatabaseAction(new DataBaseEventListener() {
            @Override
            public void onMessageAdded(LiveUniteMessage message) {
//                // send message added to chatRoom via handler
//                Message msgObj = Message.obtain();
//                msgObj.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_ADDED_NEW;
//                msgObj.obj = message;
//                mHandler.sendMessage(msgObj);
//                Log.d(TAG, "onMessageAdded()");
            }

            @Override
            public void onMessageDelete(String sender_id) {
//                // send handler with flush chat thread flag
//                Message otmObj = Message.obtain();
//                otmObj.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_DELETE;
//                otmObj.obj = sender_id;
//                mHandler.sendMessage(otmObj);
//                Log.d(TAG, "onMessageDelete()");

            }

            @Override
            public void onMessageRelocationOccurred(LiveUniteMessage message) {
//                // send message changed to chatRoom via handler
//                Message otmObj = Message.obtain();
//                otmObj.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_RELOCATIONS;
//                otmObj.obj = message;
//                mHandler.sendMessage(otmObj);
//                Log.d(TAG, "onMessageRelocation()");
            }

            @Override
            public void onChatRoomAdded(ChatRoomModel model) {
                // send message added to chatRoom via handler
                Message msgObj = Message.obtain();
                msgObj.arg1 = Constants.CHAT_ROOM.FLAG_ADDED_NEW;
                msgObj.obj = model;
                mHandler.sendMessage(msgObj);
                Log.d(TAG, "onChatRoomAdded()");

            }

            @Override
            public void onChatRoomDeleted(String roomId) {

                Message otmObj = Message.obtain();
                otmObj.arg1 = Constants.CHAT_ROOM.FLAG_DELETE;
                otmObj.obj = roomId;
                mHandler.sendMessage(otmObj);
                Log.d(TAG, "onMessageDelete()");
            }

            @Override
            public void onChatRoomInvalidationOccured(ChatRoomModel model) {

                Message otmObj = Message.obtain();
                otmObj.arg1 = Constants.CHAT_ROOM.FLAG_INVALIDATION;
                otmObj.obj = model;
                mHandler.sendMessage(otmObj);
                Log.d(TAG, "onChatRoomInvalidation()");
            }
        });

    }

    private void deleteChatRoom() {

        mDbHelper.deleteChatRoom(chatRoomModel.chatRoomId);
        Log.d(TAG, "deleted chat room id");
    }

    private void addChatRoom() {
        //WID: add chat room to database
        mDbHelper.writeChatRoom(chatRoomModel);
        Log.d(TAG, "Added chat room");

    }

    private void handleNewArrival() {
        //WID: takes the message and write to database
        Log.d(TAG, "writing arrival to database");
        DatabaseHelper.getInstance(context).writeMessage(message);
        createOrUpdateChatRoom();

    }

    private void syncGcmIds() {
        //WID: download all the gcm id registered and save to database

        Log.d(TAG, "sync Gcm Ids");

        StringRequest syncGCMIdsReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_SYNC_GCM_IDS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d(TAG, " syncGCM response " + response);

                        parseGcmIds(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "syncChat " + error);
                    }
                });

        syncGCMIdsReq.setRetryPolicy(new DefaultRetryPolicy(
                SYNC_GCM_IDS_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(syncGCMIdsReq, "syncGcmIds", context);
        //so download

    }

    private void parseGcmIds(String response) {
        /*
        * {
  "results": [
    {
      "user_id": "test_user_id1",
      "gcm_id": "test_gcm_id1"
    }
  ],
  "error": false
}
        * */

        ArrayList<LiveUniteGcmRegistration> gcmRegistrations = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(response);

            if (!jsonObject.getBoolean("error")) {
                JSONArray results = jsonObject.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = (JSONObject) results.get(i);
                    gcmRegistrations.add(new LiveUniteGcmRegistration(result.getString("user_id"), result.getString("gcm_id"), Constants.USER.VALUE_COMMUNICATION_CHANNEL_OPEN));
                }
            }

            DatabaseHelper.getInstance(context).writeGcmRegistrations(gcmRegistrations);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    // message sent from chat box
    private void submit() {
        // WID: get message from chat box and write to database and start Dispatcher Service
        mDbHelper.writeMessage(message);
        startDispatchService();

        if(!isHelpRoomQuery)
            createOrUpdateChatRoom();

        Log.d(TAG, "Action Chat Submit()");

    }

    //  Chat Rooms creations/updations
    private void createOrUpdateChatRoom() {

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        //check for existing chat room
        ChatRoomModel oldChatRoom = dbHelper.getChatRoom(message.chatRoomID);

        if (oldChatRoom != null) {
            // means there exists a chat room for given message
            //Update: chatRoomOrder, last Seen Message , unread count(for incomming msg)
            Log.d("ChatCentre", "updating old chat room");
            int previousOrder = oldChatRoom.chatRoomOrder;
            oldChatRoom.chatRoomOrder = 0;
            oldChatRoom.lastMessage = message.message;
            Log.d("ChatCentre", "updating old chat room[last msg]:" + oldChatRoom.lastMessage);
            //increment unread count for incomming msg
            if (message.chatType == Constants.CHAT.MESSAGE_TYPE_INCOMMING) {


                oldChatRoom.lastMessageTime = new LiveUniteTimeFormatter(message.receivedTime).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_CHAT_WALL);
                Log.d("ChatCentre", "updating old chat room[time]:" + oldChatRoom.lastMessageTime);
                // increment unread count
                oldChatRoom.unreadCount = String.valueOf(Integer.parseInt(oldChatRoom.unreadCount) + 1);
                Log.d("ChatCentre", "updating old chat room[unread count]:" + oldChatRoom.unreadCount);
            }

            oldChatRoom.lastMessageTime = new LiveUniteTimeFormatter(message.sentTime).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_CHAT_WALL);

            //for chat room having    :: (chatRoomOrder<previousOrder)
            ArrayList<ChatRoomModel> chatRooms = dbHelper.getChatRooms();
            for (ChatRoomModel chatRoom : chatRooms) {

                if (chatRoom.chatRoomOrder < previousOrder) {
                    chatRoom.chatRoomOrder += 1;
                    Log.d("ChatCentre", " updating chat room " + chatRoom.chatRoomTitle);
                    dbHelper.updateChatRoom(chatRoom);
                }

            }
            //update target chatRoom
            dbHelper.updateChatRoom(oldChatRoom);


        } else {
            // no chat rooms
            //Create


            String msgInitCount = "";
            String chatRoomTitle = "";
            String chatRoomThumbnail = getDpUrl(message);
            String selfId = "";
            String companianId = "";
            String time = "";

            if (message.chatType == Constants.CHAT.MESSAGE_TYPE_INCOMMING) {
                msgInitCount = "1";
                chatRoomTitle = message.senderTitle;
                selfId = message.receiverId;
                companianId = message.senderId;
                time = new LiveUniteTimeFormatter(message.receivedTime).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_CHAT_WALL);

            } else {
                msgInitCount = "0";
                chatRoomTitle = message.receiverTitle;
                selfId = message.senderId;
                companianId = message.receiverId;
                time = new LiveUniteTimeFormatter(message.sentTime).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_CHAT_WALL);
            }

            ChatRoomModel newChatRoom = new ChatRoomModel(
                    message.chatRoomID,
                    chatRoomTitle,
                    chatRoomThumbnail,
                    msgInitCount,
                    message.message,
                    time,
                    "normal",
                    "normal",
                    0,
                    selfId,
                    companianId);

            //for all chat room , increase orders by 1
            ArrayList<ChatRoomModel> chatRooms = dbHelper.getChatRooms();
            for (ChatRoomModel chatRoom : chatRooms) {

                chatRoom.chatRoomOrder += 1;
                dbHelper.updateChatRoom(chatRoom);

            }
            Log.d("ChatCentre", "Creating new chat room " + newChatRoom.chatRoomTitle);
            // write target chatRoom
            dbHelper.writeChatRoom(newChatRoom);

        }
    }

    private String getDpUrl(LiveUniteMessage msg) {
        String fbId = (msg.chatType == Constants.CHAT.MESSAGE_TYPE_INCOMMING) ? msg.senderId : msg.receiverId;
        Log.d(TAG, "Chat wall fbId:" + fbId);
        return "https://graph.facebook.com/" + fbId + "/picture?width=144&height=144";
    }

    private void startDispatchService() {

        Intent dispatchIntent = new Intent(context, MessageDispatchService.class);
        context.startService(dispatchIntent);
        Log.d(TAG, " started Dispatch Service");

    }

    // call when new push arrives for relocation status e.g.-sent,delivery,seen
    private void invalidateMessageRelocations() {
        //WID: update the message status to database
        switch (relocationType) {
            case Constants.CHAT.FLAG_RELOCATION_TYPE_SENT:
                mDbHelper.updateMessageSentStatus(message.chatId);
                Log.d(TAG, "Chat Relocation Type [SENT]");
                break;
            case Constants.CHAT.FLAG_RELOCATION_TYPE_DELIVERED:
                mDbHelper.updateMessageDeliveryStatus(message);
                Log.d(TAG, "Chat Relocation Type [DELIVERED]");
                break;
            case Constants.CHAT.FLAG_RELOCATION_TYPE_SEEN:
                mDbHelper.updateMessageSeenStatus(message);
                Log.d(TAG, "Chat Relocation Type [SEEN]");
                break;
            default:
                break;
        }

    }

    private void delete() {
        //WID: delete all messages from db and send request to delete at app server
        Log.d(TAG, "Delete Action()");
        //todo:implement delete method
    }

    // WID: send the cached data and register for onward event
    private void keepAlive() {

        Log.e(TAG, "Action Keep Alive [Chat Room Id]:" + chatRoomId);
        //send data first
        ArrayList<LiveUniteMessage> messageArrayList = mDbHelper.readMessages(chatRoomId);

        //clear any old data in adapter if exists
        Message clearFlagMessage = Message.obtain();
        clearFlagMessage.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_CLEAR_OLD;
        mHandler.sendMessage(clearFlagMessage);

        //now send cached data
        for (LiveUniteMessage msg : messageArrayList) {

            if (msg.isSeen.equals("FALSE") && msg.chatType == Constants.CHAT.MESSAGE_TYPE_INCOMMING) {
                reportSeen(msg);
            }

            Message otmObj = Message.obtain();
            otmObj.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_ADDED_NEW;
            otmObj.obj = msg;
            mHandler.sendMessage(otmObj);
        }

        Log.d(TAG, "Sent Last Cached Data");
        Log.d(TAG, "Registered for onward event");

        registerEventListenerForDatabaseAction(new DataBaseEventListener() {
            @Override
            public void onMessageAdded(LiveUniteMessage message) {

                if (message.isSeen.equals("FALSE") && message.chatType == Constants.CHAT.MESSAGE_TYPE_INCOMMING) {
                    reportSeen(message);
                }

                //[If message belong to current chatRoom] send message added to chatRoom via handler
                if (message.chatRoomID.equals(chatRoomId)) {
                    Message msgObj = Message.obtain();
                    msgObj.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_ADDED_NEW;
                    msgObj.obj = message;
                    mHandler.sendMessage(msgObj);
                    Log.d(TAG, "onMessageAdded()");
                }
            }

            @Override
            public void onMessageDelete(String sender_id) {
                // send handler with flush chat thread flag
                Message otmObj = Message.obtain();
                otmObj.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_DELETE;
                otmObj.obj = sender_id;
                mHandler.sendMessage(otmObj);
                Log.d(TAG, "onMessageDelete()");

            }

            @Override
            public void onMessageRelocationOccurred(LiveUniteMessage message) {

                //[If message belong to current chatRoom] send message changed to chatRoom via handler
                if (message.chatRoomID.equals(chatRoomId)) {

                    Message otmObj = Message.obtain();
                    otmObj.arg1 = Constants.CHAT.HANDLER_ARGS_FLAG_RELOCATIONS;
                    otmObj.obj = message;
                    mHandler.sendMessage(otmObj);
                    Log.d(TAG, "onMessageRelocation()");
                }
            }

            @Override
            public void onChatRoomAdded(ChatRoomModel model) {
//                // send message added to chatRoom via handler
//                Message msgObj = Message.obtain();
//                msgObj.arg1 = Constants.CHAT_ROOM.FLAG_ADDED_NEW;
//                msgObj.obj = model;
//                mHandler.sendMessage(msgObj);
//                Log.d(TAG, "onChatRoomAdded()");
            }

            @Override
            public void onChatRoomDeleted(String roomId) {
//
//                Message otmObj = Message.obtain();
//                otmObj.arg1 = Constants.CHAT_ROOM.FLAG_DELETE;
//                otmObj.obj = roomId;
//                mHandler.sendMessage(otmObj);
//                Log.d(TAG, "onMessageDelete()");
            }

            @Override
            public void onChatRoomInvalidationOccured(ChatRoomModel model) {
//
//                Message otmObj = Message.obtain();
//                otmObj.arg1 = Constants.CHAT_ROOM.FLAG_INVALIDATION;
//                otmObj.obj = model;
//                mHandler.sendMessage(otmObj);
//                Log.d(TAG, "onChatRoomInvalidation()");
            }
        });

    }

    private void reportSeen(final LiveUniteMessage data) {

        //checked for incomming and seen=false
        String senderGcmId = DatabaseHelper.getInstance(context).getGcmID(data.senderId);
        String seenTime = LiveUniteTime.getInstance().getDateTime();   // current time

        final String jsonBody = new ModelToJsonConverter()
                .getMessageSeenModelJson(
                        new PushMessageSeenModel(
                                senderGcmId,
                                data.chatId,
                                seenTime));

        StringRequest sendChatReq = new StringRequest(Request.Method.POST,
                Constants.SERVER.URL_GCM_SERVER_SEND_MESSAGE,

                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("ChatThreadAdapter", " seen reported:" + response);
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
                                Log.d("ChatThreadAdapter", " Successfully sent seen report back to sender");
                                ChatCentre.getInstance(context).submitAction(ChatCentre.FLAG_RELOCATION_INVALIDATION, data, Constants.CHAT.FLAG_RELOCATION_TYPE_SEEN);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ChatThreadAdapter", "volley Error " + error);
                    }
                }) {

            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.d("ChatThreadAdapter", " seen Report getBody() => " + jsonBody);
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

        VolleyUtils.getInstance().addToRequestQueue(sendChatReq, "sendSeenReportReq", context);

    }

    private String createChatId(String selfId, String companionId) {
        return "cr_" + selfId + companionId;
    }

    public void registerEventListenerForDatabaseAction(DataBaseEventListener dataBaseEventListener) {
        mDbHelper.setOnDataBaseEventListener(dataBaseEventListener);
    }

    public void submitAction(int flagDelete, String chatRoomId) {

        if (flagDelete == FLAG_DELETE_CHAT_ROOM) {
            DatabaseHelper.getInstance(context).deleteChatRoom(chatRoomId);
        }

        if (flagDelete == ChatCentre.FLAG_DELETE) {
            DatabaseHelper.getInstance(context).deleteMessagesFromChatRoom(chatRoomId);
        }

        Message otmObj = Message.obtain();
        otmObj.arg1 = Constants.CHAT_ROOM.FLAG_DELETE;
        otmObj.obj = chatRoomId;
        mHandler.sendMessage(otmObj);
        Log.d(TAG, "onMessageDelete()");


    }

    public interface DataBaseEventListener {

        void onMessageAdded(LiveUniteMessage message);

        void onMessageDelete(String sender_id);

        void onMessageRelocationOccurred(LiveUniteMessage message);

        void onChatRoomAdded(ChatRoomModel model);

        void onChatRoomDeleted(String roomId);

        void onChatRoomInvalidationOccured(ChatRoomModel model);

    }

}