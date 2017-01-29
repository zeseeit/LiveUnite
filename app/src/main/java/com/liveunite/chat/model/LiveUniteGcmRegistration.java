package com.liveunite.chat.model;

/**
 * Created by Ankit on 12/13/2016.
 */
public class LiveUniteGcmRegistration {
    public String receiverID;
    public String gcmID;
    public String commChannel;

    public LiveUniteGcmRegistration(String receiverID, String gcmID,String commChannel) {
        this.receiverID = receiverID;
        this.gcmID = gcmID;
        this.commChannel = commChannel;
    }

    public String getCommChannel() {
        return commChannel;
    }

    public void setCommChannel(String commChannel) {
        this.commChannel = commChannel;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

    public String getGcmID() {
        return gcmID;
    }

    public void setGcmID(String gcmID) {
        this.gcmID = gcmID;
    }
}
