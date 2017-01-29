package com.liveunite.chat.model;

/**
 * Created by Ankit on 12/13/2016.
 */

public class PushMessageModel {

    public String to;
    public int pushType;
    public String senderId;
    public String senderTitle;
    public String senderDpUrl;
    public String message;
    public String sentTime;
    public String msg_id;
    public String type;

    public PushMessageModel(String to, int pushType, String senderId, String senderTitle, String senderDpUrl, String message, String sentTime, String msg_id,String type) {

        this.type= type;
        this.to = to;
        this.pushType = pushType;
        this.senderId = senderId;
        this.senderTitle = senderTitle;
        this.senderDpUrl = senderDpUrl;
        this.message = message;
        this.sentTime = sentTime;
        this.msg_id = msg_id;

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getPushType() {
        return pushType;
    }

    public void setPushType(int pushType) {
        this.pushType = pushType;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderTitle() {
        return senderTitle;
    }

    public void setSenderTitle(String senderTitle) {
        this.senderTitle = senderTitle;
    }

    public String getSenderDpUrl() {
        return senderDpUrl;
    }

    public void setSenderDpUrl(String senderDpUrl) {
        this.senderDpUrl = senderDpUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentTime() {
        return sentTime;
    }

    public void setSentTime(String sentTime) {
        this.sentTime = sentTime;
    }

    public String getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(String msg_id) {
        this.msg_id = msg_id;
    }
}
