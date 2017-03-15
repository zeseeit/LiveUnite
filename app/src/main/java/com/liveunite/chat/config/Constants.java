package com.liveunite.chat.config;

import android.os.Environment;

/**
 * Created by Ankit on 12/19/2016.
 */

public class Constants {

    public interface RETRY_MOMENTS{
        String DIR_CACHE_MOMENTS = Environment.getExternalStorageDirectory().getPath() + "/FailedMomentsUpload/";
    }

    public interface CHAT_ROOM{
        int FLAG_ADDED_NEW = 7;
        int FLAG_INVALIDATION  = 8;
        int FLAG_DELETE = 9;
        String HELP_CENTRE_FB_ID_odd = "<abhishek id>";
        String HELP_CENTRE_FB_ID_even = "<other id>";
    };

    public interface ACTIONS {
        final String DISPATCH_NEW = "com.live.unite.DISPATCH_NEW";

    }

    public interface SERVER {

        String BASE = "http://chat460.liveunite-37d35.appspot.com";
        String URL_SERVER_REGISTER_GCM = BASE+"/registerGcmClient.php";
        String URL_SYNC_GCM_IDS = BASE+"/getGcmRegistrations.php";
        String URL_UPDATE_LAST_SEEN = BASE+"/setLastSeen.php";
        String URL_GET_LAST_SEEN = BASE+"/getLastSeen.php";

        String URL_GCM_SERVER_SEND_MESSAGE = "https://fcm.googleapis.com/fcm/send";
        String GOOGLE_SERVER_KEY = "key=AIzaSyBufUjL3pjHjUdCEE1A2FOyZZy_38s666U";
        String KEY_AUTHORIZATION = "Authorization";
        String KEY_CONTENT_TYPE = "Content-Type";

        String URL_PROFILE_INFO = BASE+"/getUserProfile.php";
        String URL_USER_REPORT = BASE+"/reportUser";;
        String URL_UPLOAD_CHAT = BASE+"/saveMessage";
        String URL_DELETE_ACCOUNT = BASE + "/deleteAccount";
        String URL_REPORT_POST = BASE + "/reportPost";
        String SYNC_REPORTED_PIDS = BASE + "/getReportedPids";
    }

    public interface CHAT {
        int MESSAGE_TYPE_INCOMMING = 1;
        int MESSAGE_TYPE_OUTGOING = 2;

        int HANDLER_ARGS_FLAG_DELETE = 107;
        int HANDLER_ARGS_FLAG_RELOCATIONS = 108;
        int HANDLER_ARGS_FLAG_ADDED_NEW = 110;

        int FLAG_RELOCATION_TYPE_SENT = 111;
        int FLAG_RELOCATION_TYPE_DELIVERED = 112;
        int FLAG_RELOCATION_TYPE_SEEN = 113;
        int HANDLER_ARGS_FLAG_CLEAR_OLD = 119;
        public String TYPE_QUERY = "Query";
    }

    public interface USER {
        String KEY_USER_ID = "user_id_live_unite";
        String KEY_REPORT_TYPE_ONLINE = "online";
        String KEY_REPORT_TYPE_LAST_SEEN = "lastSeen";
        String VALUE_COMMUNICATION_CHANNEL_BLOCKED = "blocked";
        String VALUE_COMMUNICATION_CHANNEL_OPEN = "open";

    }

    public interface PUSH {
        int TYPE_NEW_MESSAGE = 1;
        int TYPE_MESSAGE_DELIVERED = 2;
        int TYPE_MESSAGE_SEEN = 3;
        int TYPE_TYPING = 4;
        int TYPE_TOKEN_REFRESH = 5;
    }

}

