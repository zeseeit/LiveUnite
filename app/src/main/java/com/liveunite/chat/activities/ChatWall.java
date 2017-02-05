package com.liveunite.chat.activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.liveunite.R;
import com.liveunite.chat.adapters.ChatWallAdapter;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.database.DatabaseHelper;
import com.liveunite.chat.helper.CircularImageTransformer;
import com.liveunite.chat.model.ChatRoomModel;
import com.liveunite.chat.model.LiveUniteMessage;
import com.liveunite.infoContainer.Singleton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChatWall extends AppCompatActivity{
    private static final String TAG = ChatWall.class.getSimpleName();
    private Handler mHandler;
    private RecyclerView chatRoomRecycler;
    private ChatWallAdapter adapter;
    private RelativeLayout supportLayout;
    private ImageView helpCenterThumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat_wall);
        Toolbar toolbar = (Toolbar) findViewById(R.id.chatWallToolbar);
        toolbar.setTitle("Direct Message");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        initView();
        setAdapter();

    }

    @Override
    protected void onResume() {
        super.onResume();

        submitSyncAction();
    }

    private void submitSyncAction() {

        ChatCentre.getInstance(this).submitAction(ChatCentre.FLAG_KEEP_LIVE_CHAT_WALL, getHandler());

    }

    private void initView() {

        chatRoomRecycler = (RecyclerView) findViewById(R.id.chat_room_recycler_view);
        adapter = new ChatWallAdapter(this);
        chatRoomRecycler.setAdapter(adapter);
        chatRoomRecycler.setLayoutManager(new LinearLayoutManager(this));
        supportLayout = (RelativeLayout) findViewById(R.id.helpRelativeLayout);
        helpCenterThumbnail = (ImageView) findViewById(R.id.helpCenterThumb);
        String url = "http://liveunite.com/wp-content/uploads/2016/06/LogoMakr-2.png";
        Picasso.with(this).load(url).transform(new CircularImageTransformer()).into(helpCenterThumbnail);

        supportLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//
//                String helpCentreId ="899612170173914" ;///(Integer.parseInt(Singleton.getInstance().getUserDetails().getFbId().substring(0,3))%2==0)?Constants.CHAT_ROOM.HELP_CENTRE_FB_ID_even:Constants.CHAT_ROOM.HELP_CENTRE_FB_ID_odd;
//                Intent chatRoomIntent = new Intent(ChatWall.this, ChatRoom.class);
//                Bundle bundle = new Bundle();
//                bundle.putString("fbId", helpCentreId);    //  fbId of Help Centre
//                bundle.putString("title", "Team LiveUnite");
//                String dpUrl = "http://liveunite.com/wp-content/uploads/2016/06/LogoMakr-2.png";
//                bundle.putString("dpUrl", dpUrl);
//                bundle.putBoolean("isHelpRoom",true);
//                chatRoomIntent.putExtras(bundle);
//                //Toast.makeText(ChatWall.this,"Under Construction",Toast.LENGTH_LONG).show();
//                startActivity(chatRoomIntent);

                Toast.makeText(ChatWall.this,"Preparing For Your Help",Toast.LENGTH_LONG).show();

            }
        });
    }

    private void setAdapter() {

        adapter = new ChatWallAdapter(this);
        chatRoomRecycler.setAdapter(adapter);
        chatRoomRecycler.setLayoutManager(new LinearLayoutManager(this));

    }

    private Handler getHandler() {

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                int type = msg.arg1;
                switch (type) {

                    case Constants.CHAT.HANDLER_ARGS_FLAG_CLEAR_OLD:

                        clearChatWall();

                        break;

                    case Constants.CHAT_ROOM.FLAG_ADDED_NEW:

                        adapter.addChatRoom((ChatRoomModel) msg.obj);

                        break;

                    case Constants.CHAT_ROOM.FLAG_INVALIDATION:

                        invalidateChatRoom((ChatRoomModel) msg.obj);

                        break;

                    case Constants.CHAT_ROOM.FLAG_DELETE:

                        adapter.removeChatRoom((String) msg.obj);
                        break;

                    default:
                        break;
                }

            }
        };

        return mHandler;
    }

    private void invalidateChatRoom(ChatRoomModel obj) {
        adapter.invalidateChatRoom(obj);
    }

    private void clearChatWall() {
        ArrayList<ChatRoomModel> emptyList = new ArrayList<>();
        adapter.setChatRooms(emptyList);
    }

}
