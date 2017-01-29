package com.liveunite.chat.model;

/**
 * Created by Ankit on 12/13/2016.
 */

public class PushMessageDeliveryModel {
   public String to;
   public String msg_id;
   public String deliveryTime;

    public PushMessageDeliveryModel(String to, String msg_id, String deliveryTime) {
        this.to = to;
        this.msg_id = msg_id;
        this.deliveryTime = deliveryTime;
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

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }
}
