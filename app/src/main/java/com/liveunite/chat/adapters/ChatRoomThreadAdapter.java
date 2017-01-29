package com.liveunite.chat.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.R;
import com.liveunite.chat.activities.ChatRoom;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.converters.FontManager;
import com.liveunite.chat.converters.LiveUniteTimeFormatter;
import com.liveunite.chat.converters.ModelToJsonConverter;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.chat.model.LiveUniteMessage;
import com.liveunite.chat.model.PushMessageSeenModel;
import com.liveunite.utils.Constant;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by Ankit on 12/19/2016.
 */

public class ChatRoomThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static ArrayList<LiveUniteMessage> messages;
    private static Context context;
    private int REPORT_SEND_TIMEOUT = 1 * 60 * 1000;     // 1 minute
    public static LongPressedListener longPressedListener;

    public ChatRoomThreadAdapter(Context context) {
        this.messages = new ArrayList<>();
        this.context = context;
    }

    public void setMessages(ArrayList<LiveUniteMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addNewMessage(LiveUniteMessage message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void invalidateRelocations(LiveUniteMessage message) {
        //WID: find the message with same chatId and replace the

        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).chatId.equals(message.chatId) && messages.get(i).chatType == message.chatType) {
                Log.d("Adapter", " inval: sent-" + message.isSent + " del-" + message.isDelivered);
                messages.set(i, message);
                notifyItemChanged(i);
                break;
            }
        }

    }

    public void setLongPressedListener(LongPressedListener listener) {
        this.longPressedListener = listener;
    }

    public static class IncommingMessageViewHolder extends RecyclerView.ViewHolder {

        TextView content;
        TextView timestamp;

        public IncommingMessageViewHolder(View itemView) {
            super(itemView);

            content = (TextView) itemView.findViewById(R.id.chat_text);
            timestamp = (TextView) itemView.findViewById(R.id.incoming_chat_timestamp);
        }
    }

    public static class OutgoingMessageViewHolder extends RecyclerView.ViewHolder {

        TextView content;
        TextView timestamp;
        TextView sentStatus;

        public OutgoingMessageViewHolder(View itemView) {
            super(itemView);

            content = (TextView) itemView.findViewById(R.id.chat_text);
            timestamp = (TextView) itemView.findViewById(R.id.outgoing_chat_timestamp);
            sentStatus = (TextView) itemView.findViewById(R.id.chat_sending_status);
            sentStatus.setTypeface(FontManager.getInstance(context).getTypeFace());

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(longPressedListener!=null){
                        int pos = getAdapterPosition();
                        try {
                            String seenTime = new LiveUniteTimeFormatter(messages.get(pos).seenTime).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_MESSAGE_TIP);
                            String deliveredTime = new LiveUniteTimeFormatter(messages.get(pos).receivedTime).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_MESSAGE_TIP);
                            longPressedListener.onLongPressed(messages.get(pos).message,seenTime,deliveredTime);
                            return true;
                        }catch (Exception e){
                            Log.d("ChatRoomThreadAdapter","TimeFormat Exception");
                        }

                        longPressedListener.onLongPressed(messages.get(pos).message,"","");
                    }
                    return true;
                }
            });

        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == Constants.CHAT.MESSAGE_TYPE_INCOMMING) {
            View incommingView = LayoutInflater.from(context).inflate(R.layout.message_in_layout, parent, false);
            return new IncommingMessageViewHolder(incommingView);
        } else {
            View outgoingView = LayoutInflater.from(context).inflate(R.layout.message_out_layout, parent, false);
            return new OutgoingMessageViewHolder(outgoingView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        LiveUniteMessage data = messages.get(position);

        if (holder instanceof OutgoingMessageViewHolder) {
            // bind outgoing view
            String msg = data.getMessage().replace("&quot;","\"");
            ((OutgoingMessageViewHolder) holder).content.setText(msg);
            String formattedTime = new LiveUniteTimeFormatter(data.getSentTime()).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_MESSAGE_TIP);
            ((OutgoingMessageViewHolder) holder).timestamp.setText(formattedTime);

            // seen status
            if (data.isSent.equals("TRUE")) {
                String sentIcon = context.getResources().getString(R.string.message_submitted_icon);

                ((OutgoingMessageViewHolder) holder).sentStatus.setTextColor(Color.WHITE);
                ((OutgoingMessageViewHolder) holder).sentStatus.setText(sentIcon);

                if (data.isDelivered.equals("TRUE")) {

                    String deliveredIcon = context.getResources().getString(R.string.message_delivered_icon);
                    ((OutgoingMessageViewHolder) holder).sentStatus.setTextColor(Color.WHITE);
                    ((OutgoingMessageViewHolder) holder).sentStatus.setText(deliveredIcon);
                }

                if (data.isSeen.equals("TRUE")) {

                    ((OutgoingMessageViewHolder) holder).sentStatus.setTextColor(Color.RED);
                    String deliveredIcon = context.getResources().getString(R.string.message_delivered_icon);
                    ((OutgoingMessageViewHolder) holder).sentStatus.setText(deliveredIcon);

                }
            } else {
                // pending delivery icon
                String pendingDeliveryIcon = context.getResources().getString(R.string.pending_delivery_icon);
                ((OutgoingMessageViewHolder) holder).sentStatus.setTextColor(Color.WHITE);
                ((OutgoingMessageViewHolder) holder).sentStatus.setText(pendingDeliveryIcon);

            }


        } else {
            // bind incomming view
            String msg = data.getMessage().replace("&quot;","\"");
            ((IncommingMessageViewHolder) holder).content.setText(msg);
            String formattedTime = new LiveUniteTimeFormatter(data.getReceivedTime()).getFormattedTimeStamp(LiveUniteTimeFormatter.FORMAT_MESSAGE_TIP);
            ((IncommingMessageViewHolder) holder).timestamp.setText(formattedTime);

        }

    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {

        int type = (messages.get(position).chatType == Constants.CHAT.MESSAGE_TYPE_INCOMMING) ? Constants.CHAT.MESSAGE_TYPE_INCOMMING : Constants.CHAT.MESSAGE_TYPE_OUTGOING;
        return type;

    }

    public interface LongPressedListener{
        void onLongPressed(String chat,String seenTime,String deliveredTime);
    }

}