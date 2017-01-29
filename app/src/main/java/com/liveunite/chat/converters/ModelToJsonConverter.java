package com.liveunite.chat.converters;

import com.liveunite.chat.config.Constants;
import com.liveunite.chat.model.PushMessageDeliveryModel;
import com.liveunite.chat.model.PushMessageModel;
import com.liveunite.chat.model.PushMessageSeenModel;

/**
 * Created by Ankit on 12/13/2016.
 */

public class ModelToJsonConverter {


    public ModelToJsonConverter() {

    }

    public String getNewMessagePushModelJson(PushMessageModel pushMessageModel){

        String json = "{" +
                "\"to\":\""+ pushMessageModel.to+ "\"," +
                "\"data\":{ \"push_type\":"+ Constants.PUSH.TYPE_NEW_MESSAGE+","+
                "\"payload\":{ \"sender_id\":\""+pushMessageModel.senderId+"\"," +
                "\"msg\":\""+pushMessageModel.message+"\"," +
                "\"type\":\""+pushMessageModel.type+"\"," +
                "\"sender_title\":\""+pushMessageModel.senderTitle+"\"," +
                "\"sender_dp_url\":\""+pushMessageModel.senderDpUrl+"\"," +
                "\"sent_time\":\""+pushMessageModel.sentTime+"\"," +
                "\"msg_id\":\""+pushMessageModel.msg_id+"\"}}}";

        return json;
    }

    public String getMessageDeliveryModelJson(PushMessageDeliveryModel messageDeliveryModel){
        String json = "{" +
                "\"to\":\""+ messageDeliveryModel.to+ "\"," +
                "\"data\":{ \"push_type\":"+ Constants.PUSH.TYPE_MESSAGE_DELIVERED+","+
                "\"payload\":{ \"msg_id\":\""+messageDeliveryModel.msg_id+"\"," +
                "\"delivered_time\":\""+messageDeliveryModel.deliveryTime+"\"}}}";

        return json;
    }

    public String getMessageSeenModelJson(PushMessageSeenModel messageSeenModel){
        String json = "{" +
                "\"to\":\""+ messageSeenModel.to+ "\"," +
                "\"data\":{ \"push_type\":"+ Constants.PUSH.TYPE_MESSAGE_SEEN+","+
                "\"payload\":{ \"msg_id\":\""+messageSeenModel.msg_id+"\"," +
                "\"seen_time\":\""+messageSeenModel.seen_time+"\"}}}";

        return json;
    }

    public String getTypingPushModelJson(String to,String senderId){

        String json = "{" +
                "\"to\":\""+ to+ "\"," +
                "\"data\":{ \"push_type\":"+ Constants.PUSH.TYPE_TYPING+","+
                "\"payload\":{ \"sender_id\":\""+senderId+"\"" +
                "}}}";

        return json;

    }

    public String getTokenRefreshModelJson(String to){
        String json = "{" +
                "\"to\":\""+to+ "\"," +
                "\"data\":{ \"push_type\":"+ Constants.PUSH.TYPE_TOKEN_REFRESH+"}}";

        return json;
    }


}
