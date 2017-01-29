package com.liveunite.chat.model;

/**
 * Created by Ankit on 12/15/2016.
 */

public class ChatRoomModel {

    public String chatRoomId;
    public String chatRoomTitle;
    public String chatRoomThumbnailUrl;
    public String unreadCount;
    public String lastMessage;
    public String lastMessageTime;
    public String chatRoomActivity;
    public String lastMessageStatus;
    public int chatRoomOrder;
    public String selfId;
    public String companionId;

    public ChatRoomModel(String chatRoomId, String chatRoomTitle, String chatRoomThumbnailUrl, String unreadCount, String lastMessage, String lastMessageTime, String chatRoomActivity, String lastMessageStatus, int getChatRoomOrder, String selfId, String companionId) {
        this.chatRoomId = chatRoomId;
        this.chatRoomTitle = chatRoomTitle;
        this.chatRoomThumbnailUrl = chatRoomThumbnailUrl;
        this.unreadCount = unreadCount;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.chatRoomActivity = chatRoomActivity;
        this.lastMessageStatus = lastMessageStatus;
        this.chatRoomOrder = getChatRoomOrder;
        this.selfId = selfId;
        this.companionId = companionId;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getChatRoomTitle() {
        return chatRoomTitle;
    }

    public void setChatRoomTitle(String chatRoomTitle) {
        this.chatRoomTitle = chatRoomTitle;
    }

    public String getChatRoomThumbnailUrl() {
        return chatRoomThumbnailUrl;
    }

    public void setChatRoomThumbnailUrl(String chatRoomThumbnailUrl) {
        this.chatRoomThumbnailUrl = chatRoomThumbnailUrl;
    }

    public String getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(String unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getChatRoomActivity() {
        return chatRoomActivity;
    }

    public void setChatRoomActivity(String chatRoomActivity) {
        this.chatRoomActivity = chatRoomActivity;
    }

    public String getLastMessageStatus() {
        return lastMessageStatus;
    }

    public void setLastMessageStatus(String lastMessageStatus) {
        this.lastMessageStatus = lastMessageStatus;
    }

    public int getChatRoomOrder() {
        return chatRoomOrder;
    }

    public void setChatRoomOrder(int chatRoomOrder) {
        this.chatRoomOrder = chatRoomOrder;
    }

    public String getSelfId() {
        return selfId;
    }

    public void setSelfId(String selfId) {
        this.selfId = selfId;
    }

    public String getCompanionId() {
        return companionId;
    }

    public void setCompanionId(String companionId) {
        this.companionId = companionId;
    }
}
