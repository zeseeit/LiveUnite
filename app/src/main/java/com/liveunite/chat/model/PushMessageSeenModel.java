package com.liveunite.chat.model;

/**
 * Created by Ankit on 12/13/2016.
 */

public class PushMessageSeenModel {

   public String to;
   public String msg_id;
   public String seen_time;

    public PushMessageSeenModel(String to, String msg_id, String seen_time) {
        this.to = to;
        this.msg_id = msg_id;
        this.seen_time = seen_time;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(String msg_id) {
        this.msg_id = msg_id;
    }

    public String getSeen_time() {
        return seen_time;
    }

    public void setSeen_time(String seen_time) {
        this.seen_time = seen_time;
    }
}
