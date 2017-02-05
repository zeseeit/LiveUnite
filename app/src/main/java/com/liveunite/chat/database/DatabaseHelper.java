package com.liveunite.chat.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.model.ChatRoomModel;
import com.liveunite.chat.model.LiveUniteGcmRegistration;
import com.liveunite.chat.model.LiveUniteMessage;
import com.liveunite.utils.Constant;

import java.util.ArrayList;

/**
 * Created by Ankit on 12/6/2016.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 3;
    private static final String DB_NAME = "liveUniteDBApp";
    private static final String TABLE_GCM_REGISTRATIONS = "liveUniteGcmRegistrations";
    private static final String TABLE_MESSAGE = "liveUniteMessages";
    private static final String TABLE_CHAT_ROOMS = "chatRooms";
    //cols
    private static final String COL_CHAT_TYPE = "chatType";
    private static final String COL_CHAT_ID = "chatID";
    private static final String COL_CHAT_ROOM_ID_MESSAGE_TABLE = "chatRoomId";
    private static final String COL_SENDER_ID = "senderID";
    private static final String COL_SENDER_TITLE = "senderTitle";
    private static final String COL_RECEIVER_TITLE = "receiverTitle";
    private static final String COL_SENDER_DP_URL = "senderDpUrl";
    private static final String COL_RECEIVER_ID = "receiverID";
    private static final String COL_SENT_TIME = "sentTime";
    private static final String COL_RECEIVED_TIME = "receivedTime";
    private static final String COL_SEEN_TIME = "seenTime";
    private static final String COL_MESSAGE = "message";
    private static final String COL_IS_SENT = "isSent";
    private static final String COL_IS_SEEN = "isSeen";
    private static final String COL_IS_DELIVERED = "isDelivered";
    private static final String COL_GCM_ID = "gcmID";
    private static final String COL_COMM_CHANNEL = "commChannel";

    private static final String COL_CHAT_ROOM_ID = "chatRoomId";
    private static final String COL_CHAT_ROOM_TITLE = "chatRoomTitle";
    private static final String COL_CHAT_ROOM_THUMBNAIL_URL = "thumbnailUrl";
    private static final String COL_CHAT_UREAD_COUNT = "chatUnreadCount";
    private static final String COL_CHAT_ROOM_LAST_MESSAGE = "chatRoomLastMessage";
    private static final String COL_CHAT_ROOM_LAST_MESSAGE_TIME = "lastMessageTime";
    private static final String COL_CHAT_ROOM_ACTIVITY = "currentActivity";
    private static final String COL_CHAT_ROOM_LAST_MESSAGE_STATUS = "lastMessageStatus";
    private static final String COL_CHAT_ROOM_ORDER = "chatRoomOrder";
    private static final String COL_CHAT_SELF_ID = "selfId";
    private static final String COL_CHAT_COMPANION_ID = "companionId";

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private final String CREATE_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE + "(" +
            COL_CHAT_TYPE + " INT," +
            COL_CHAT_ID + " TEXT," +
            COL_CHAT_ROOM_ID_MESSAGE_TABLE + " TEXT," +
            COL_SENDER_ID + " TEXT," +
            COL_RECEIVER_TITLE + " TEXT," +
            COL_SENDER_TITLE + " TEXT," +
            COL_SENDER_DP_URL + " TEXT," +
            COL_RECEIVER_ID + " TEXT," +
            COL_SENT_TIME + " TEXT," +
            COL_RECEIVED_TIME + " TEXT," +
            COL_SEEN_TIME + " TEXT," +
            COL_MESSAGE + " TEXT," +
            COL_IS_SENT + " TEXT," +
            COL_IS_SEEN + " TEXT," +
            COL_IS_DELIVERED + " TEXT);";


    private final String CREATE_GCM_REGISTRATIONS = "CREATE TABLE IF NOT EXISTS " + TABLE_GCM_REGISTRATIONS + "(" +
            COL_RECEIVER_ID + " TEXT," +
            COL_COMM_CHANNEL + " TEXT," +
            COL_GCM_ID + " TEXT);";


    private final String CREATE_CHAT_ROOMS = "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_ROOMS + "(" +
            COL_CHAT_ROOM_ID + " TEXT," +
            COL_CHAT_ROOM_TITLE + " TEXT," +
            COL_CHAT_ROOM_THUMBNAIL_URL + " TEXT," +
            COL_CHAT_UREAD_COUNT + " TEXT," +
            COL_CHAT_ROOM_LAST_MESSAGE + " TEXT," +
            COL_CHAT_ROOM_LAST_MESSAGE_TIME + " TEXT," +
            COL_CHAT_ROOM_ACTIVITY + " TEXT," +
            COL_CHAT_ROOM_LAST_MESSAGE_STATUS + " TEXT," +
            COL_CHAT_ROOM_ORDER + " INT," +
            COL_CHAT_SELF_ID + " TEXT," +
            COL_CHAT_COMPANION_ID + " TEXT);";

    private static Context context;
    private static DatabaseHelper mInstance;
    private ChatCentre.DataBaseEventListener dataBaseListener;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;

    }

    public static DatabaseHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DatabaseHelper(context);
        }
        return mInstance;

    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_MESSAGE_TABLE);
        //Log.d(TAG, "Created Message Table");
        sqLiteDatabase.execSQL(CREATE_GCM_REGISTRATIONS);
        //Log.d(TAG, "Created GCM Registrations Table");
        sqLiteDatabase.execSQL(CREATE_CHAT_ROOMS);
        //Log.d(TAG, "Created Chat Rooms");

    }

    private ContentValues getCVObject(LiveUniteMessage message) {

        ContentValues contentValues = new ContentValues();
        //Log.d(TAG, "msg:" + message.message + " type:" + message.chatType + " sent:" + message.isSent + " del:" + message.isDelivered + " seen " + message.isSeen);
        contentValues.put(COL_CHAT_TYPE, message.chatType);
        contentValues.put(COL_CHAT_ID, message.chatId);
        contentValues.put(COL_CHAT_ROOM_ID_MESSAGE_TABLE, message.chatRoomID);
        contentValues.put(COL_SENDER_ID, message.senderId);
        contentValues.put(COL_SENT_TIME, message.sentTime);
        contentValues.put(COL_RECEIVER_TITLE, message.receiverTitle);
        contentValues.put(COL_SENDER_TITLE, message.senderTitle);
        contentValues.put(COL_SENDER_DP_URL, message.senderDpUrl);
        contentValues.put(COL_RECEIVER_ID, message.receiverId);
        contentValues.put(COL_SEEN_TIME, message.seenTime);
        contentValues.put(COL_RECEIVED_TIME, message.receivedTime);
        contentValues.put(COL_MESSAGE, message.message);
        contentValues.put(COL_IS_SENT, message.isSent);
        contentValues.put(COL_IS_SEEN, message.isSeen);
        contentValues.put(COL_IS_DELIVERED, message.isDelivered);

        return contentValues;

    }

    private ContentValues getCVObject(LiveUniteGcmRegistration registration) {
        ContentValues values = new ContentValues();
        values.put(COL_RECEIVER_ID, registration.getReceiverID());
        values.put(COL_GCM_ID, registration.getGcmID());
        values.put(COL_COMM_CHANNEL, registration.getCommChannel());
        return values;
    }

    private ContentValues getCVObject(ChatRoomModel model) {

        ContentValues values = new ContentValues();

        values.put(COL_CHAT_ROOM_ID, model.chatRoomId);
        values.put(COL_CHAT_ROOM_TITLE, model.chatRoomTitle);
        values.put(COL_CHAT_UREAD_COUNT, model.unreadCount);
        values.put(COL_CHAT_ROOM_LAST_MESSAGE, model.lastMessage);
        values.put(COL_CHAT_ROOM_LAST_MESSAGE_TIME, model.lastMessageTime);
        values.put(COL_CHAT_ROOM_ACTIVITY, model.chatRoomActivity);
        values.put(COL_CHAT_ROOM_LAST_MESSAGE_STATUS, model.lastMessageStatus);
        values.put(COL_CHAT_ROOM_ORDER, model.chatRoomOrder);
        values.put(COL_CHAT_SELF_ID, model.selfId);
        values.put(COL_CHAT_COMPANION_ID, model.companionId);

        return values;
    }

    //dump gcm registrations
    public void writeGcmRegistrations(ArrayList<LiveUniteGcmRegistration> registrations) {

        SQLiteDatabase db = getWritableDatabase();

        for (LiveUniteGcmRegistration gcmRegistration : registrations) {

            if (getGcmID(gcmRegistration.receiverID).length() == 0) {
                long id = db.insert(TABLE_GCM_REGISTRATIONS, null, getCVObject(gcmRegistration));
                //Log.d(TAG, "Written Gcm Id " + id);
            } else {
                //Log.d(TAG, "Updating the old registrations");
                updateGcmRegistration(gcmRegistration);
            }

        }


    }

    private void updateGcmRegistration(LiveUniteGcmRegistration registration) {

        SQLiteDatabase db = getWritableDatabase();
        String selection = COL_RECEIVER_ID + " = ?";
        String[] selectionArgs = {registration.receiverID};

        long id = db.update(TABLE_GCM_REGISTRATIONS, getCVObject(registration), selection, selectionArgs);

        if (id > 0) {
            //Log.d(TAG, "Successfully Updated Gcm Id");
        }

    }

    public String getGcmID(String receiverID) {

        //Log.d(TAG, " getGcmID for " + receiverID);
        SQLiteDatabase db = getReadableDatabase();
        String selection = COL_RECEIVER_ID + " = ?";
        String[] cols = {COL_RECEIVER_ID, COL_GCM_ID};
        String[] selectionArgs = {receiverID};
        String gcmID = "";
        Cursor cursor = db.query(TABLE_GCM_REGISTRATIONS, cols, selection, selectionArgs, null, null, null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            //Log.d(TAG, "gcm cursor count " + cursor.getCount());
            gcmID = cursor.getString(cursor.getColumnIndex(COL_GCM_ID));
        }
        //Log.d(TAG, "getGcmID() returning " + gcmID);
        return gcmID;
    }

    public String getCommChannel(String receiverID) {

        SQLiteDatabase db = getReadableDatabase();
        String selection = COL_RECEIVER_ID + " = ?";
        String[] cols = {COL_RECEIVER_ID, COL_GCM_ID, COL_COMM_CHANNEL};
        String[] selectionArgs = {receiverID};
        String commChannel = "";
        Cursor cursor = db.query(TABLE_GCM_REGISTRATIONS, cols, selection, selectionArgs, null, null, null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            commChannel = cursor.getString(cursor.getColumnIndex(COL_COMM_CHANNEL));
        }
        //Log.d(TAG, "getCommChannel() returning " + commChannel);
        return commChannel;
    }

    private LiveUniteGcmRegistration getGcmRegistration(String rec) {
        SQLiteDatabase db = getReadableDatabase();
        String selection = COL_RECEIVER_ID + " = ?";
        String[] cols = {COL_RECEIVER_ID, COL_GCM_ID, COL_COMM_CHANNEL};
        String[] selectionArgs = {rec};
        LiveUniteGcmRegistration registration = null;

        Cursor cursor = db.query(TABLE_GCM_REGISTRATIONS, cols, selection, selectionArgs, null, null, null);

        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            registration = new LiveUniteGcmRegistration(
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_GCM_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_COMM_CHANNEL)));
        }

        return registration;

    }

    public void setCommChannel(String receiverID, String channelMode) {

        SQLiteDatabase db = getWritableDatabase();
        String selection = COL_RECEIVER_ID + " = ?";
        String[] selectionArgs = {receiverID};
        LiveUniteGcmRegistration t_reg = getGcmRegistration(receiverID);
        t_reg.setCommChannel(channelMode);

        long id = db.update(TABLE_GCM_REGISTRATIONS, getCVObject(t_reg), selection, selectionArgs);

        if (id > 0) {
            //Log.d(TAG, "Successfully Updated CommChannel");
        }
    }

    // dump single message to db
    public void writeMessage(LiveUniteMessage message) {

        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(TABLE_MESSAGE, null, getCVObject(message));
        if (id > 0) {

            if (dataBaseListener != null)
                dataBaseListener.onMessageAdded(message);

            //Log.d(TAG, "Inserted Messages");
        } else {
            //Log.d(TAG, "Cannot Insert Messages");
        }
        //Log.d("LiveUniteDatabase", "===================INSERT MSG=======================");
        getDatabaseSnapshot();

    }

    // read all message from db
    public ArrayList<LiveUniteMessage> readMessages() {

        ArrayList<LiveUniteMessage> messageArrayList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] selectionArgs = {};

        String[] cols = {COL_CHAT_TYPE,
                COL_CHAT_ID,
                COL_CHAT_ROOM_ID_MESSAGE_TABLE,
                COL_SENDER_ID,
                COL_RECEIVER_TITLE,
                COL_SENDER_TITLE,
                COL_SENDER_DP_URL,
                COL_RECEIVER_ID,
                COL_SEEN_TIME,
                COL_RECEIVED_TIME,
                COL_SENT_TIME,
                COL_MESSAGE,
                COL_IS_SEEN,
                COL_IS_SENT,
                COL_IS_DELIVERED};

        Cursor cursor = db.query(TABLE_MESSAGE, cols, null, null, null, null, null);

        boolean hasMore = cursor.getCount() > 0;
        cursor.moveToFirst();

        while (hasMore) {
            messageArrayList.add(new LiveUniteMessage(
                    cursor.getInt(cursor.getColumnIndex(COL_CHAT_TYPE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ID_MESSAGE_TABLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_DP_URL)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_SENT_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVED_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_SEEN_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SENT)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SEEN)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_DELIVERED))));


            hasMore = cursor.moveToNext();
        }

        //Log.d(TAG, "Read Messages Count " + messageArrayList.size());
        return messageArrayList;
    }

    // read a single message
    public LiveUniteMessage getMessage(String msg_id) {

        LiveUniteMessage msg = null;
        SQLiteDatabase db = getReadableDatabase();
        String[] selectionArgs = {msg_id};
        String selections = COL_CHAT_ID + " = ?";
        String[] cols = {COL_CHAT_TYPE, COL_CHAT_ID, COL_CHAT_ROOM_ID_MESSAGE_TABLE, COL_SENDER_ID, COL_RECEIVER_TITLE, COL_SENDER_TITLE, COL_SENDER_DP_URL, COL_RECEIVER_ID, COL_SEEN_TIME, COL_SENT_TIME, COL_RECEIVED_TIME, COL_MESSAGE, COL_IS_SEEN, COL_IS_SENT, COL_IS_DELIVERED};

        Cursor cursor = db.query(TABLE_MESSAGE, cols, selections, selectionArgs, null, null, null);

        boolean hasMore = cursor.getCount() > 0;
        cursor.moveToFirst();

        if (hasMore) {
            msg = new LiveUniteMessage(cursor.getInt(cursor.getColumnIndex(COL_CHAT_TYPE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ID_MESSAGE_TABLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_DP_URL)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_SENT_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVED_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_SEEN_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SENT)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SEEN)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_DELIVERED)));
        }

        //Log.d(TAG, "Read Messages ID: " + msg.chatId);
        return msg;

    }

    public LiveUniteMessage getMessage(String msg_id, int messageType) {

        LiveUniteMessage msg = null;
        SQLiteDatabase db = getReadableDatabase();

        String[] selectionArgs = {msg_id, "" + messageType};
        String selections = COL_CHAT_ID + " = ? AND " + COL_CHAT_TYPE + " = ? ";
        String[] cols = {COL_CHAT_TYPE, COL_CHAT_ID, COL_CHAT_ROOM_ID_MESSAGE_TABLE, COL_SENDER_ID, COL_RECEIVER_TITLE, COL_SENDER_TITLE, COL_SENDER_DP_URL, COL_RECEIVER_ID, COL_SEEN_TIME, COL_SENT_TIME, COL_RECEIVED_TIME, COL_MESSAGE, COL_IS_SEEN, COL_IS_SENT, COL_IS_DELIVERED};

        Cursor cursor = db.query(TABLE_MESSAGE, cols, selections, selectionArgs, null, null, null);

        boolean hasAny = cursor.getCount() > 0;
        cursor.moveToFirst();
        if (hasAny) {
            msg = new LiveUniteMessage(cursor.getInt(cursor.getColumnIndex(COL_CHAT_TYPE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ID_MESSAGE_TABLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_DP_URL)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_SENT_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVED_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_SEEN_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SENT)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SEEN)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_DELIVERED)));

            //Log.d(TAG, " query for type:" + messageType + " found " + msg.chatType);
        }


        ////Log.d(TAG, "Read Messages ID: " + msg.chatId);
        return msg;

    }

    // udate sent status
    public void updateMessageSentStatus(String msg_id) {

        SQLiteDatabase db = getWritableDatabase();
        LiveUniteMessage message = getMessage(msg_id, Constants.CHAT.MESSAGE_TYPE_OUTGOING); // UPDATE SENT STATUS IS ONLY FOR OUTGOINGS
        message.setIsSent("TRUE");
        String selection = COL_CHAT_ID + " = ? AND " + COL_CHAT_TYPE + " = ?";
        String[] selectionArgs = {msg_id, Constants.CHAT.MESSAGE_TYPE_OUTGOING + ""};

        long id = db.update(TABLE_MESSAGE, getCVObject(message), selection, selectionArgs);
        if (id > 0) {
            if (dataBaseListener != null)
                dataBaseListener.onMessageRelocationOccurred(message);
            //Log.d(TAG, "Successfully Updated Sent Status");
        }

        //Log.d("LiveUniteDatabase", "===================UPDATE SENT=======================");
        getDatabaseSnapshot();

    }


    // udate delivery status
    public void updateMessageDeliveryStatus(LiveUniteMessage message) {

        //Since message delivery update is for outgoing message only,
        // So update only for outgoing message with same message id;
        SQLiteDatabase db = getWritableDatabase();
        String selection = COL_CHAT_ID + " = ? AND " + COL_CHAT_TYPE + " = ?";
        String[] selectionArgs = {message.chatId, "" + Constants.CHAT.MESSAGE_TYPE_OUTGOING};  //Delivery Status is only for outgoings

        message.isDelivered = "TRUE";

        long id = db.update(TABLE_MESSAGE, getCVObject(message), selection, selectionArgs);

        if (id > 0) {
            if (dataBaseListener != null)
                dataBaseListener.onMessageRelocationOccurred(message);
            //Log.d(TAG, "Successfully Updated Delivery Status");
        }

        //Log.d("LiveUniteDatabase", "===================UPDATE DEL=======================");
        getDatabaseSnapshot();
    }

    public ArrayList<LiveUniteMessage> getPendings() {

        ArrayList<LiveUniteMessage> messageArrayList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String selection = COL_IS_SENT + " = ? AND " + COL_CHAT_TYPE + " =?";
        String[] selectionArgs = {"FALSE", Constants.CHAT.MESSAGE_TYPE_OUTGOING + ""};

        String[] cols = {COL_CHAT_TYPE, COL_CHAT_ID, COL_CHAT_ROOM_ID_MESSAGE_TABLE, COL_SENDER_ID, COL_RECEIVER_TITLE, COL_SENDER_TITLE, COL_SENDER_DP_URL, COL_RECEIVER_ID, COL_SENT_TIME, COL_SEEN_TIME, COL_RECEIVED_TIME, COL_MESSAGE, COL_IS_SEEN, COL_IS_SENT, COL_IS_DELIVERED};

        Cursor cursor = db.query(TABLE_MESSAGE, cols, selection, selectionArgs, null, null, null);

        boolean hasMore = cursor.getCount() > 0;
        cursor.moveToFirst();

        while (hasMore) {
            messageArrayList.add(new LiveUniteMessage(cursor.getInt(cursor.getColumnIndex(COL_CHAT_TYPE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ID_MESSAGE_TABLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_DP_URL)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_SENT_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVED_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_SEEN_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SEEN)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SENT)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_DELIVERED))));

            hasMore = cursor.moveToNext();
        }

        //Log.d(TAG, "Read Messages Count " + messageArrayList.size());
        return messageArrayList;
    }

    public int getUnreadCount() {

        int unreadCount = 0;

        SQLiteDatabase db = getReadableDatabase();

        String[] cols = {
                COL_CHAT_ROOM_ID,
                COL_CHAT_ROOM_TITLE,
                COL_CHAT_UREAD_COUNT,
                COL_CHAT_ROOM_THUMBNAIL_URL,
                COL_CHAT_ROOM_LAST_MESSAGE,
                COL_CHAT_ROOM_LAST_MESSAGE_TIME,
                COL_CHAT_ROOM_ACTIVITY,
                COL_CHAT_ROOM_LAST_MESSAGE_STATUS,
                COL_CHAT_ROOM_ORDER,
                COL_CHAT_SELF_ID,
                COL_CHAT_COMPANION_ID};

        Cursor cursor = db.query(TABLE_CHAT_ROOMS, cols, null, null, null, null, null);

        boolean hasMore = cursor.getCount() > 0;
        cursor.moveToFirst();

        while (hasMore) {
            unreadCount += Integer.parseInt(cursor.getString(cursor.getColumnIndex(COL_CHAT_UREAD_COUNT)));
            hasMore = cursor.moveToNext();
        }

        //Log.d("DatabaseHelper", " total unread " + unreadCount);
        return unreadCount;

    }

    // udate seen status
    public void updateMessageSeenStatus(LiveUniteMessage message) {


        //Since message seen update is for outgoing message only,
        // So update only for outgoing message with same message id;
        SQLiteDatabase db = getWritableDatabase();
        String selection = COL_CHAT_ID + " = ? AND " + COL_CHAT_TYPE + " = ?";
        String[] selectionArgs = {message.chatId, message.chatType + ""};
        message.isSeen = "TRUE";
        long id = db.update(TABLE_MESSAGE, getCVObject(message), selection, selectionArgs);
        if (id > 0) {
            if (dataBaseListener != null)
                dataBaseListener.onMessageRelocationOccurred(message);
            //Log.d(TAG, "Successfully Updated Seen Status");
        }

        //Log.d("LiveUniteDatabase", "===================UPDATE SEEN=======================");
        getDatabaseSnapshot();

    }

    public void deleteMessage(String sender_id) {

        SQLiteDatabase db = getWritableDatabase();
        String selction = COL_SENDER_ID + " = ? OR " + COL_RECEIVER_ID + " = ?";
        String[] selectionArgs = {sender_id, sender_id};

        long id = db.delete(TABLE_MESSAGE, selction, selectionArgs);
        if (id > 0) {
            if (dataBaseListener != null) {
                dataBaseListener.onMessageDelete(sender_id);
            }
            //Log.d(TAG, "Successfully Deleted Messages");
        }

    }

    public void writeChatRoom(ChatRoomModel model) {
        SQLiteDatabase db = getWritableDatabase();

        long id = db.insert(TABLE_CHAT_ROOMS, null, getCVObject(model));
        if (id > 0) {
            //Log.d(TAG, "Inserted ChatRoom");
        }

        if (dataBaseListener != null)
            dataBaseListener.onChatRoomAdded(model);

    }

    public void updateChatRoom(ChatRoomModel model) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = COL_CHAT_ROOM_ID + " = ?";
        String[] selectionArgs = {model.chatRoomId};

        long id = db.update(TABLE_CHAT_ROOMS, getCVObject(model), selection, selectionArgs);

        if (id > 0) {
            //Log.d(TAG, "Successfully Updated ChatRoom");
        }

        if (dataBaseListener != null)
            dataBaseListener.onChatRoomInvalidationOccured(model);

    }

    public ChatRoomModel getChatRoom(String chatRoomId) {

        SQLiteDatabase db = getReadableDatabase();
        ChatRoomModel chatRoom = null;
        String selection = COL_CHAT_ROOM_ID + " = ?";
        String[] cols = {
                COL_CHAT_ROOM_ID,
                COL_CHAT_ROOM_TITLE,
                COL_CHAT_UREAD_COUNT,
                COL_CHAT_ROOM_THUMBNAIL_URL,
                COL_CHAT_ROOM_LAST_MESSAGE,
                COL_CHAT_ROOM_LAST_MESSAGE_TIME,
                COL_CHAT_ROOM_ACTIVITY,
                COL_CHAT_ROOM_LAST_MESSAGE_STATUS,
                COL_CHAT_ROOM_ORDER,
                COL_CHAT_SELF_ID,
                COL_CHAT_COMPANION_ID};

        String[] selectionArgs = {chatRoomId};

        Cursor cursor = db.query(TABLE_CHAT_ROOMS, cols, selection, selectionArgs, null, null, null);
        cursor.moveToFirst();
        boolean hasAny = cursor.getCount() > 0;

        if (hasAny) {
            chatRoom = new ChatRoomModel(
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_THUMBNAIL_URL)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_UREAD_COUNT)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_LAST_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_LAST_MESSAGE_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ACTIVITY)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_LAST_MESSAGE_STATUS)),
                    Integer.parseInt(cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ORDER))),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_SELF_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_COMPANION_ID)));
        }

        return chatRoom;

    }

    public ArrayList<ChatRoomModel> getChatRooms() {

        ArrayList<ChatRoomModel> chatRooms = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String[] cols = {
                COL_CHAT_ROOM_ID,
                COL_CHAT_ROOM_TITLE,
                COL_CHAT_UREAD_COUNT,
                COL_CHAT_ROOM_THUMBNAIL_URL,
                COL_CHAT_ROOM_LAST_MESSAGE,
                COL_CHAT_ROOM_LAST_MESSAGE_TIME,
                COL_CHAT_ROOM_ACTIVITY,
                COL_CHAT_ROOM_LAST_MESSAGE_STATUS,
                COL_CHAT_ROOM_ORDER,
                COL_CHAT_SELF_ID,
                COL_CHAT_COMPANION_ID};

        String orderBy = COL_CHAT_ROOM_ORDER + " ASC";
        Cursor cursor = db.query(TABLE_CHAT_ROOMS, cols, null, null, null, null, orderBy);

        cursor.moveToFirst();
        boolean hasMore = cursor.getCount() > 0;

        while (hasMore) {

            chatRooms.add(new ChatRoomModel(
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_THUMBNAIL_URL)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_UREAD_COUNT)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_LAST_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_LAST_MESSAGE_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ACTIVITY)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_LAST_MESSAGE_STATUS)),
                    Integer.parseInt(cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ORDER))),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_SELF_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_COMPANION_ID))));

            hasMore = cursor.moveToNext();

        }

        //Log.d(TAG, "read " + chatRooms.size() + " Chat Rooms");
        return chatRooms;
    }

    public void deleteChatRoom(String roomId) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = COL_CHAT_ROOM_ID + " =?";
        String[] selectionArgs = {roomId};

        long id = db.delete(TABLE_CHAT_ROOMS, selection, selectionArgs);
        if (id > 0) {
            //Log.d(TAG, "deleted chat room");
        }

        deleteMessagesFromChatRoom(roomId);

        if (dataBaseListener != null)
            dataBaseListener.onChatRoomDeleted(roomId);

    }

    public void getDatabaseSnapshot() {
        ArrayList<LiveUniteMessage> messages = readMessages();
        //Log.d("LiveUniteDatabase", "===================NOW=======================");
        for (LiveUniteMessage ms : messages) {
            //Log.d("LiveUniteDatabase", "chatRoomId: " + ms.chatRoomID + " msg " + ms.message + " type: " + ms.chatType + " isSent " + ms.isSent + " isDel " + ms.isDelivered + " isSeen " + ms.isSeen);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        if (oldVersion < newVersion) {
            sqLiteDatabase.execSQL("DROP TABLE " + TABLE_CHAT_ROOMS);
            sqLiteDatabase.execSQL("DROP TABLE " + TABLE_GCM_REGISTRATIONS);
            sqLiteDatabase.execSQL("DROP TABLE " + TABLE_MESSAGE);
            onCreate(sqLiteDatabase);
        }

    }

    public void setOnDataBaseEventListener(ChatCentre.DataBaseEventListener dataBaseEventListener) {
        this.dataBaseListener = dataBaseEventListener;
    }

    public ArrayList<LiveUniteMessage> readMessages(String chatRoomId) {

        ArrayList<LiveUniteMessage> messageArrayList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] selectionArgs = {chatRoomId};
        String selection = COL_CHAT_ROOM_ID_MESSAGE_TABLE + " = ?";
        String[] cols = {COL_CHAT_TYPE, COL_CHAT_ID, COL_CHAT_ROOM_ID_MESSAGE_TABLE, COL_SENDER_ID, COL_RECEIVER_TITLE, COL_SENDER_TITLE, COL_SENDER_DP_URL, COL_RECEIVER_ID, COL_SEEN_TIME, COL_RECEIVED_TIME, COL_SENT_TIME, COL_MESSAGE, COL_IS_SEEN, COL_IS_SENT, COL_IS_DELIVERED};

        Cursor cursor = db.query(TABLE_MESSAGE, cols, selection, selectionArgs, null, null, null, null);

        boolean hasMore = cursor.getCount() > 0;
        cursor.moveToFirst();

        while (hasMore) {
            messageArrayList.add(new LiveUniteMessage(
                    cursor.getInt(cursor.getColumnIndex(COL_CHAT_TYPE)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_CHAT_ROOM_ID_MESSAGE_TABLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_SENDER_DP_URL)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVER_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_SENT_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_RECEIVED_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_SEEN_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_MESSAGE)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SENT)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_SEEN)),
                    cursor.getString(cursor.getColumnIndex(COL_IS_DELIVERED))));


            hasMore = cursor.moveToNext();
        }

        return messageArrayList;

    }

    public void deleteMessagesFromChatRoom(String chatRoomId) {

        SQLiteDatabase db = getWritableDatabase();
        String selction = COL_CHAT_ROOM_ID_MESSAGE_TABLE + " = ?";
        String[] selectionArgs = {chatRoomId};

        long id = db.delete(TABLE_MESSAGE, selction, selectionArgs);

        //Log.d(TAG, "Deleted Chat for Chat Room " + chatRoomId);

    }
}
