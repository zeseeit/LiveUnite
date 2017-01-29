package com.liveunite.chat.activities;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.R;
import com.liveunite.chat.adapters.ChatRoomThreadAdapter;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.FontManager;
import com.liveunite.chat.converters.LiveUniteTimeFormatter;
import com.liveunite.chat.converters.ModelToJsonConverter;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.gcm.Config;
import com.liveunite.chat.gcm.LiveUnitePreferenceManager;
import com.liveunite.chat.helper.CircularImageTransformer;
import com.liveunite.chat.helper.LiveUniteTime;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.chat.model.ChatRoomModel;
import com.liveunite.chat.model.LiveUniteMessage;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.models.UserDetails;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ChatRoom extends AppCompatActivity implements View.OnFocusChangeListener, TextWatcher , ChatRoomThreadAdapter.LongPressedListener {

    private Bundle receivingBundle;
    private String companionId;
    private String selfId;
    private String companionDpUrl;
    private String companionTitle;
    private TextView chatRoomTitle;
    private TextView lastSeenStatus;
    private Boolean isTyping = false;
    private ChatRoomThreadAdapter chatRoomThreadAdapter;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private long LAST_STATUS_SEEN_SYNC_INTERVAL = 4000; // 1 Sec
    private int SYNC_LAST_SEEN_TIMEOUT_MS = 1 * 60 * 1000;  // 1 MIN
    private String chatRoomId;
    private long TYPING_INFO_RETAINING_TIMEOUT = 4000;
    private boolean typingBroadcastReceiverRegistered = false;
    private long TYPING_INFO_SEND_INTERVAL = 2000;
    private String companionGcmId;
    private boolean isHelpRoom = false;
    private TypingInfoBroadcastReceiver typingInfoBroadcastReceiver;
    private ImageView chatRoomDp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        receivingBundle = getIntent().getExtras();
        companionId = receivingBundle.getString("fbId");
        companionDpUrl = receivingBundle.getString("dpUrl")+"?width=144&height=144";
        companionTitle = receivingBundle.getString("title");
        isHelpRoom = receivingBundle.getBoolean("isHelpRoom");
        companionGcmId = DatabaseHelper.getInstance(this).getGcmID(companionId);
        selfId = LiveUnite.getInstance().getPreferenceManager().getFbId();
        chatRoomId = createChatRoomId(selfId, companionId);
        Log.d("ChatRoom", " Friend Id: " + companionId + " dpUrl :" + companionDpUrl + " selfId :" + selfId);
        prepareChatRoom();

    }

    @Override
    protected void onResume() {

        super.onResume();
        resetUnreadCount(chatRoomId);
        registerTypingBroadcastReceiver();
        submitKeepLiveAction();

        if(!isHelpRoom)
            syncLastSeen();
    }

    @Override
    protected void onPause() {

        super.onPause();
        resetUnreadCount(chatRoomId);
        unRegisterTypingBroadcastReceiver();
        cancelLastStatusSyncTask();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(isHelpRoom)
            return false;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat_room,menu);

        MenuItem item = menu.findItem(R.id.blockUser);

        boolean blocked = DatabaseHelper.getInstance(ChatRoom.this).getCommChannel(companionId).equals(Constants.USER.VALUE_COMMUNICATION_CHANNEL_BLOCKED);

        if(blocked){
            item.setTitle("UnBlock");
        }else{
            item.setTitle("Block");
        }

        MenuItem nItem = menu.findItem(R.id.muteNotification);
        boolean mutedNotification = LiveUnite.getInstance().getPreferenceManager().isNotificationMuted();

        if(mutedNotification){
            nItem.setTitle("UnMute");
        }else{
            nItem.setTitle("Mute Notification");
        }



        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.deleteChat:
                deleteChatThread();
                break;
            case R.id.blockUser:

                boolean blocked = DatabaseHelper.getInstance(ChatRoom.this).getCommChannel(companionId).equals(Constants.USER.VALUE_COMMUNICATION_CHANNEL_BLOCKED);
                invalidateUser(blocked);

                break;

            case R.id.reportUser:
                showReportDialog();
                break;

            case R.id.muteNotification:

                LiveUnite.getInstance().getPreferenceManager().muteNotification(!LiveUnite.getInstance().getPreferenceManager().isNotificationMuted());

                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showReportDialog() {
        Intent reportIntent  = new Intent(this,ReportUserThemed.class);
        reportIntent.putExtra("cmpId",companionId);
        startActivity(reportIntent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {


        MenuItem item = menu.findItem(R.id.blockUser);

        boolean blocked = DatabaseHelper.getInstance(ChatRoom.this).getCommChannel(companionId).equals(Constants.USER.VALUE_COMMUNICATION_CHANNEL_BLOCKED);

        if(blocked){
            item.setTitle("UnBlock");
        }else{
            item.setTitle("Block");
        }

        MenuItem nItem = menu.findItem(R.id.muteNotification);
        boolean mutedNotification = LiveUnite.getInstance().getPreferenceManager().isNotificationMuted();

        if(mutedNotification){
            nItem.setTitle("UnMute");
        }else{
            nItem.setTitle("Mute Notification");
        }




        return super.onPrepareOptionsMenu(menu);
    }

    private void deleteChatThread() {
        ChatCentre.getInstance(this).submitAction(ChatCentre.FLAG_DELETE,chatRoomId);
        clearChatThread();
    }

    private void invalidateUser(boolean blocked){

        if(blocked){
            //unblock it
            Log.d("ChatRoom","UnBlocking user "+companionTitle);
            DatabaseHelper.getInstance(this).setCommChannel(companionId,Constants.USER.VALUE_COMMUNICATION_CHANNEL_OPEN);
        }else{
            // block it
            Log.d("ChatRoom","Blocking user "+companionTitle);
            DatabaseHelper.getInstance(this).setCommChannel(companionId,Constants.USER.VALUE_COMMUNICATION_CHANNEL_BLOCKED);
        }

    }

    private ChatRoomThreadAdapter.LongPressedListener listener = new ChatRoomThreadAdapter.LongPressedListener() {
        @Override
        public void onLongPressed(String chat, String seenTime, String deliveredTime) {
            makeDialog(chat,seenTime,deliveredTime);
        }
    };

    private void makeDialog(String chat, String seenTime, String deliveredTime) {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.chat_info_dialog,null,false);
        TextView chatContent = (TextView) view.findViewById(R.id.chatInfoDialogContent);
        TextView chatDel = (TextView) view.findViewById(R.id.chatInfoDialogDeliveryTime);
        TextView chatSeen = (TextView) view.findViewById(R.id.chatInfoDialogSeenTime);

        chatSeen.setText("Seen At : "+seenTime);
        chatDel.setText("Delivered At : "+ deliveredTime);
        chatContent.setText(chat);

        dialog.setCancelable(true);
        dialog.setContentView(view);
        dialog.show();

    }

    private void prepareChatRoom() {

        chatRoomThreadAdapter = new ChatRoomThreadAdapter(this);
        chatRoomThreadAdapter.setLongPressedListener(listener);
        recyclerView = (RecyclerView) findViewById(R.id.chat_thread_recycler_view);
        recyclerView.setAdapter(chatRoomThreadAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        toolbar = (Toolbar) findViewById(R.id.chatRoomToolbar);
        View toolbarInnerView = LayoutInflater.from(this).inflate(R.layout.chat_room_header_layout, null, false);
        chatRoomDp = (ImageView) toolbarInnerView.findViewById(R.id.chatRoomCompanionDp);
        chatRoomTitle = (TextView) toolbarInnerView.findViewById(R.id.chatRoomCompanionTitle);
        lastSeenStatus = (TextView) toolbarInnerView.findViewById(R.id.chatRoomCompanionLastSeenStatus);
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.LEFT;
        toolbar.addView(toolbarInnerView,layoutParams);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        //values binding
        Picasso.with(this).load(companionDpUrl).transform(new CircularImageTransformer()).into(chatRoomDp);
        chatRoomTitle.setText(companionTitle);

        if(!isHelpRoom)
            chatRoomDp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profileIntent = new Intent(ChatRoom.this,Profile.class);
                profileIntent.putExtra("fbId",companionId);
                profileIntent.putExtra("title",companionTitle);
                startActivity(profileIntent);
            }
        });

        // message send views

        final EditText messageBox = (EditText) findViewById(R.id.msg_editor_box);

        if(companionGcmId.length()>0)   // Send typing info to registered user only.
            messageBox.addTextChangedListener(this);

        TextView sendBtn = (TextView) findViewById(R.id.sendBtn);
        sendBtn.setTypeface(FontManager.getInstance(this).getTypeFace());
        sendBtn.setOnClickListener(new View.OnClickListener() {
            public String BLANK_FOR_NOW = "";

            @Override
            public void onClick(View view) {

                String time = LiveUniteTime.getInstance().getDateTime();
                LiveUnitePreferenceManager manager = LiveUnite.getInstance().getPreferenceManager();

                String selfTitle = manager.getFirstname() + " " + manager.getLastname();
                String dpUrl = "https://graph.facebook.com/" + manager.getFbId() + "/picture?width=144&height=144";
                String HelpQueryPrefix = isHelpRoom ? "HelpRoomQuery":"";
                String msg_id = "m_" + manager.getFbId() + "_" + System.currentTimeMillis();

                String msg = String.valueOf(messageBox.getText().toString()).trim().replace("\"","&quot;");
                if(msg.length()==0)
                    return;

                LiveUniteMessage __m = new LiveUniteMessage(
                        Constants.CHAT.MESSAGE_TYPE_OUTGOING,
                        msg_id,
                        chatRoomId,
                        selfId,
                        companionTitle,
                        selfTitle,
                        dpUrl,
                        companionId,
                        time,
                        BLANK_FOR_NOW,  //received time
                        BLANK_FOR_NOW,  // seen time
                        msg,
                        "FALSE",
                        "FALSE",
                        "FALSE"
                );

                Log.d("PushReceiverChatroom","ChatRoomId=>"+chatRoomId+" SenderId =>"+selfId);

                submitMessageSendAction(__m);
                messageBox.setText("");

            }
        });

        LiveUnite.getInstance().getPreferenceManager().clearNotifications();

    }

    private void submitMessageSendAction(LiveUniteMessage message) {

        ChatCentre.getInstance(this).submitAction(ChatCentre.FLAG_SUBMIT, getHandlerInstance(), message,isHelpRoom);

    }

    private void submitKeepLiveAction() {

        ChatCentre.getInstance(this).submitAction(ChatCentre.FLAG_KEEP_ALIVE, getHandlerInstance(),chatRoomId);

    }

    private Handler getHandlerInstance() {

        return new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.arg1) {
                    case Constants.CHAT.HANDLER_ARGS_FLAG_CLEAR_OLD:

                        clearChatThread();

                        break;
                    case Constants.CHAT.HANDLER_ARGS_FLAG_ADDED_NEW:

                        addNewMessageToChatThreadList((LiveUniteMessage) msg.obj);
                        resetUnreadCount(chatRoomId);

                        break;
                    case Constants.CHAT.HANDLER_ARGS_FLAG_RELOCATIONS:

                        invalidateMessageRelocationChatThreadList((LiveUniteMessage) msg.obj);

                        break;
                    case Constants.CHAT.HANDLER_ARGS_FLAG_DELETE:

                        clearChatThread();

                        break;
                    default:
                        break;
                }
            }
        };

    }

    private void clearChatThread() {

        ArrayList<LiveUniteMessage> emptyList = new ArrayList<>();
        chatRoomThreadAdapter.setMessages(emptyList);
    }

    private void invalidateMessageRelocationChatThreadList(LiveUniteMessage message) {
        chatRoomThreadAdapter.invalidateRelocations(message);
    }

    private void addNewMessageToChatThreadList(LiveUniteMessage message) {

        chatRoomThreadAdapter.addNewMessage(message);

        if (recyclerView.getAdapter().getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
        }

    }

    private Timer mLastSeenTimer;

    private TimerTask mLastSeenTask = new TimerTask() {
        @Override
        public void run() {
            // sync status and update ui
            Log.d("ChatRoomLastSeenSync", "Running Last seen Task..................");
            sync();
        }
    };

    private void sendTypingInfo() {

        final String jsonBody = new ModelToJsonConverter().getTypingPushModelJson(companionGcmId,selfId);

        StringRequest reportTypingReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_GCM_SERVER_SEND_MESSAGE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("ChatRoomTypingInfo", "typing Info response " + response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ChatRoomTypingInfo", "send typing info " + error);
                    }
                }){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String ,String> headerMap = new HashMap<>();
                headerMap.put(Constants.SERVER.KEY_AUTHORIZATION,Constants.SERVER.GOOGLE_SERVER_KEY);
                headerMap.put(Constants.SERVER.KEY_CONTENT_TYPE,"application/json");
                return headerMap;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.d("ChatRoomTypingInfo"," body => "+jsonBody);
                return jsonBody.getBytes();
            }

        };

        reportTypingReq.setRetryPolicy(new DefaultRetryPolicy(
                SYNC_LAST_SEEN_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(reportTypingReq, "typingReporterTask", this);

    }

    private void syncLastSeen() {

        if (mLastSeenTimer == null) {
            mLastSeenTimer = new Timer();
            mLastSeenTimer.scheduleAtFixedRate(mLastSeenTask,0,LAST_STATUS_SEEN_SYNC_INTERVAL);
        }

    }

    private void cancelLastStatusSyncTask() {

        Log.d("ChatRoomLastSeenSync", "removing last seen callbacks");
        VolleyUtils.getInstance().cancelPendingRequests("syncLastSeen");
        if (mLastSeenTimer != null)
            mLastSeenTimer.cancel();

    }

    private void sync() {

        StringRequest syncGCMIdsReq = new StringRequest(Request.Method.POST, Constants.SERVER.URL_GET_LAST_SEEN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("ChatRoomLastSeenSync", " syncLast Seen response " + response);
                        handleReponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ChatRoomLastSeenSync", "syncChat " + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                final String cmpId = companionId;
                HashMap<String, String> map = new HashMap<>();
                map.put("user_id", cmpId);
                return map;
            }
        };

        syncGCMIdsReq.setRetryPolicy(new DefaultRetryPolicy(
                SYNC_LAST_SEEN_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleyUtils.getInstance().addToRequestQueue(syncGCMIdsReq, "syncLastSeen", this);


    }

    private void handleReponse(String response) {
            /*
            * {
  "results": [
    {
      "user_id": "1828696364010305",
      "status": "2016-12-24 11:02:11"
    }
  ],
  "hasResult": true,
  "error": false,
  "message": "Success"
}
            * */

        try {
            JSONObject _seenObject = new JSONObject(response);

            // check for existence
            if (_seenObject.getBoolean("hasResult")) {
                JSONArray array = _seenObject.getJSONArray("results");
                //single value array
                String status = ((JSONObject) array.get(0)).getString("status");
                updateLastSeenStatus(status);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void updateLastSeenStatus(String status) {
        if (!isTyping) {
            if (status.equals("Online")) {
                lastSeenStatus.setText("Online");
            } else {
                String _s = new LiveUniteTimeFormatter(status).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_TOOLBAR);
                lastSeenStatus.setText(_s);
            }
        }
    }

    private String createChatRoomId(String selfId, String companionId) {

        String HelpQueryPrefix = isHelpRoom ? "HelpRoomQuery":"ChatCommons";
        return "cr_"+ HelpQueryPrefix+"_"+ selfId + companionId;
    }

    private void resetUnreadCount(String chatRoomId){

        ChatRoomModel model = DatabaseHelper.getInstance(this).getChatRoom(chatRoomId);
        if(model!=null)
            DatabaseHelper.getInstance(this).updateChatRoom(model);

    }

    private void unRegisterTypingBroadcastReceiver(){

        if(typingBroadcastReceiverRegistered){
            unregisterReceiver(typingInfoBroadcastReceiver);
            typingBroadcastReceiverRegistered = false;
        }

    }

    private void registerTypingBroadcastReceiver(){

        typingInfoBroadcastReceiver = new TypingInfoBroadcastReceiver();

        if(!typingBroadcastReceiverRegistered){
            registerReceiver(typingInfoBroadcastReceiver,new IntentFilter(Config.ACTION_TYPING_INFO));
            typingBroadcastReceiverRegistered = true;
        }
    }

    private Handler typingHandler = new Handler();
    private Runnable typingInfoSendRunnable = new Runnable() {
        @Override
        public void run() {
            if(!isHelpRoom)
                sendTypingInfo();
        }
    };

    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                typingHandler.postAtFrontOfQueue(typingInfoSendRunnable);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void onFocusChange(View view, boolean b) {

    }

    @Override
    public void onLongPressed(String chat, String seenTime, String deliveredTime) {

    }

    private class TypingInfoBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("ChatRoomTypingInfo"," typing sender.."+intent.getExtras().getString("senderId"));

                if (intent.getExtras().getString("senderId").equals(companionId)) {

                    Log.d("ChatRoomTypingInfo"," this is typing info for the current chat room");

                    isTyping = true;
                    lastSeenStatus.setTextColor(Color.RED);
                    lastSeenStatus.setText("typing...");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isTyping = false;
                            lastSeenStatus.setTextColor(Color.WHITE);
                        }
                    }, TYPING_INFO_RETAINING_TIMEOUT);

                }

        }
    }


}
