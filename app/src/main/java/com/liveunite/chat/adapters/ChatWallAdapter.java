package com.liveunite.chat.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.liveunite.R;
import com.liveunite.chat.activities.ChatRoom;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.helper.CircularImageTransformer;
import com.liveunite.chat.model.ChatRoomModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by Ankit on 12/19/2016.
 */

public class ChatWallAdapter extends RecyclerView.Adapter<ChatWallAdapter.ViewHolder> {

    static ArrayList<ChatRoomModel> chatRooms;
    private Context context;
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contexual_menu_chat_wall, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("Delete Chat Room");
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.contextualDelete:
                    deleteChatRoom();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    private int longPressedPos;

    private void deleteChatRoom() {

        Log.d("ChatWallAdapter", " deleting chat Room " + chatRooms.get(longPressedPos).chatRoomTitle);
        ChatCentre.getInstance(context).submitAction(ChatCentre.FLAG_DELETE_CHAT_ROOM, chatRooms.get(longPressedPos).chatRoomId);
        notifyDataSetChanged();
    }

    public ChatWallAdapter(Context context) {
        this.context = context;
        this.chatRooms = new ArrayList<>();

    }

    public void setChatRooms(ArrayList<ChatRoomModel> chatRooms) {
        this.chatRooms = chatRooms;
        notifyDataSetChanged();

    }

    public void addChatRoom(ChatRoomModel roomModel) {
        this.chatRooms.add(roomModel);
        Log.d("ChatWallAdapter", " added chat room");
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_room_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ChatRoomModel chatRoomModel = chatRooms.get(position);

        //bind data
        holder.chatRoomTitle.setText(chatRoomModel.chatRoomTitle);
        String msg = chatRoomModel.lastMessage.replace("&quot;","\"");
        holder.message.setText(msg);
        holder.time.setText(chatRoomModel.lastMessageTime);

        // if unread count is >0
        if (Integer.parseInt(chatRoomModel.unreadCount) > 0) {
            holder.unReadCount.setVisibility(View.VISIBLE);
            holder.unReadCount.setText(chatRoomModel.unreadCount);
        } else {
            holder.unReadCount.setVisibility(View.GONE);
        }

        String dpUrl = "https://graph.facebook.com/" + chatRoomModel.companionId + "/picture?width=144&height=144";
        Picasso.with(context).load(dpUrl).transform(new CircularImageTransformer()).into(holder.thumbnail);

    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    public void removeChatRoom(String obj) {
        for(int i =0;i<chatRooms.size();i++){
            if(chatRooms.get(i).chatRoomId.equals(obj)){
                chatRooms.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }

    }

    public void invalidateChatRoom(ChatRoomModel invalidatedChatRoom) {

        for(int i=0;i<chatRooms.size();i++){
                if(chatRooms.get(i).chatRoomId.equals(invalidatedChatRoom.chatRoomId)){
                    chatRooms.remove(i);
                    chatRooms.add(invalidatedChatRoom);
                    notifyDataSetChanged();
                    return;
                }
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView chatRoomTitle;
        TextView unReadCount;
        TextView time;
        TextView message;
        ImageView thumbnail;
        TextView status;

        public ViewHolder(final View itemView) {
            super(itemView);

            chatRoomTitle = (TextView) itemView.findViewById(R.id.chatRoomTitle);
            unReadCount = (TextView) itemView.findViewById(R.id.unReadCounter);
            time = (TextView) itemView.findViewById(R.id.chatRoomTime);
            message = (TextView) itemView.findViewById(R.id.lastMessage);
            thumbnail = (ImageView) itemView.findViewById(R.id.chatRoomThumbnail);
            status = (TextView) itemView.findViewById(R.id.lastMessageStatus);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int position = getAdapterPosition();
                    ChatRoomModel model = chatRooms.get(position);
                    Intent chatRoomIntent = new Intent(context, ChatRoom.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("fbId", model.companionId);
                    bundle.putString("title", model.chatRoomTitle);
                    bundle.putBoolean("isHelpRoom",false);
                    String dpUrl = "https://graph.facebook.com/" + model.companionId + "/picture?width=144&height=144";
                    bundle.putString("dpUrl", dpUrl);
                    chatRoomIntent.putExtras(bundle);
                    model.unreadCount = "0";
                    DatabaseHelper.getInstance(context).updateChatRoom(model);
                    context.startActivity(chatRoomIntent);

                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    longPressedPos = getAdapterPosition();
                    if (mActionMode != null) {
                        return false;
                    }

                    mActionMode = ((AppCompatActivity) context).startSupportActionMode(mActionModeCallback);
                    itemView.setSelected(true);
                    return true;
                }
            });

        }
    }

    public interface ChatRoomDeleteListener{
        void onDelete();
    }

}