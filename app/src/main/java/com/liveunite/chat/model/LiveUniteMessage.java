package com.liveunite.chat.model;

/**
 * Created by Ankit on 12/6/2016.
 */

public class LiveUniteMessage {

    public int chatType;    // outgoing/incoming
    public String chatId;
    public String chatRoomID;
    public String senderId;
    public String senderTitle;
    public String receiverTitle;
    public String senderDpUrl;
    public String receiverId;
    public String sentTime;
    public String receivedTime;
    public String seenTime;
    public String message;
    public String isSent;
    public String isSeen;
    public String isDelivered;

    public LiveUniteMessage(int chatType, String chatId, String chatRoomID, String senderId,String receiverTitle ,String senderTitle, String senderDpUrl, String receiverId, String sentTime, String receivedTime, String seenTime, String message, String isSent, String isSeen, String isDelivered) {
        this.chatType = chatType;
        this.chatId = chatId;
        this.chatRoomID = chatRoomID;
        this.senderId = senderId;
        this.receiverTitle = receiverTitle;
        this.senderTitle = senderTitle;
        this.senderDpUrl = senderDpUrl;
        this.receiverId = receiverId;
        this.sentTime = sentTime;
        this.receivedTime = receivedTime;
        this.seenTime = seenTime;
        this.message = message;
        this.isSent = isSent;
        this.isSeen = isSeen;
        this.isDelivered = isDelivered;
    }

    public String getReceiverTitle() {
        return receiverTitle;
    }

    public void setReceiverTitle(String receiverTitle) {
        this.receiverTitle = receiverTitle;
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getChatRoomID() {
        return chatRoomID;
    }

    public void setChatRoomID(String chatRoomID) {
        this.chatRoomID = chatRoomID;
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

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getSentTime() {
        return sentTime;
    }

    public void setSentTime(String sentTime) {
        this.sentTime = sentTime;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getSeenTime() {
        return seenTime;
    }

    public void setSeenTime(String seenTime) {
        this.seenTime = seenTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIsSent() {
        return isSent;
    }

    public void setIsSent(String isSent) {
        this.isSent = isSent;
    }

    public String getIsSeen() {
        return isSeen;
    }

    public void setIsSeen(String isSeen) {
        this.isSeen = isSeen;
    }

    public String getIsDelivered() {
        return isDelivered;
    }

    public void setIsDelivered(String isDelivered) {
        this.isDelivered = isDelivered;
    }
}
