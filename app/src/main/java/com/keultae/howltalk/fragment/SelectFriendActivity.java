package com.keultae.howltalk.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.keultae.howltalk.R;
import com.keultae.howltalk.chat.GroupMessageActivity;
import com.keultae.howltalk.model.MessageModel;
import com.keultae.howltalk.model.RoomModel;
import com.keultae.howltalk.model.UserModel;

import java.io.Serializable;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectFriendActivity extends AppCompatActivity {
    private final String TAG = "SelectFriendActivity";

    private List<UserModel> userModelList = new ArrayList<>();
    private Map<String, UserModel> userModelMap = new HashMap<>();

    private RoomModel roomModel = new RoomModel();
    private UserModel userModel;
    private Map<String, UserModel> chatRoomUserModels = new HashMap<>(); // 1:1 채팅방의 사용자

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);

        userModel = (UserModel) getIntent().getSerializableExtra("userModel");
        Log.d(TAG, "onCreate() userModel=" + userModel.toString());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.selectFriendActivity_recyclerview);
        recyclerView.setAdapter(new SelectFriendRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button button = (Button)findViewById(R.id.selectFriendActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String roomId = FirebaseDatabase.getInstance().getReference().child("rooms").push().getKey();
                final String chattingId = FirebaseDatabase.getInstance().getReference().child("messages").child(roomId).push().getKey();

                StringBuilder sb = new StringBuilder();
                for(String tmpUid: roomModel.users.keySet()) {
                    sb.append(userModelMap.get(tmpUid).userName);
                    sb.append(",");
                    chatRoomUserModels.put(tmpUid, userModelMap.get(tmpUid));
                }
                String initMessage = sb.toString().substring(0, sb.toString().length()-1)+ "을 초대합니다.";
                roomModel.roomId = roomId;
                roomModel.descTimestamp = Long.MAX_VALUE - System.currentTimeMillis();
                roomModel.lastMessage = initMessage;

                String uid = FirebaseAuth.getInstance().getUid();
                roomModel.users.put(uid, true);

                MessageModel messageModel = new MessageModel();
                messageModel.uid = uid;
                messageModel.timestamp = System.currentTimeMillis();
                messageModel.message = initMessage;
                for(String key: roomModel.users.keySet()) {
                    if(!key.equals(uid)) {
                        messageModel.readUsers.put(key, false);
                    }
                }

                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("/rooms/" + roomId, roomModel.toMap());
                childUpdates.put("/messages/" + roomId + "/" + chattingId, messageModel.toMap());

                FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "onClick() > onSuccess() > 생성 roomId=" + roomId);

                                Intent intent = new Intent(v.getContext(), GroupMessageActivity.class);
                                intent.putExtra("chatRoomId", roomId);
                                intent.putExtra("roomModel", roomModel);
                                intent.putExtra("chatRoomUserModels", (Serializable) chatRoomUserModels);

                                ActivityOptions activityOptions = null;
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    activityOptions = ActivityOptions.makeCustomAnimation(v.getContext(), R.anim.fromright, R.anim.toleft);
                                    startActivity(intent, activityOptions.toBundle());
                                }
                                finish();
                            }
                        });
            }
        });
    }

    class SelectFriendRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public SelectFriendRecyclerViewAdapter() {

            final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userModelList.clear();
                    for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        Log.d("PeopleFragment", "snapshot=" + snapshot.toString());
                        UserModel userModel = snapshot.getValue(UserModel.class);

                        if(userModel.uid.equals(myUid)) {
                            continue;
                        }
                        userModelList.add(userModel);
                        userModelMap.put(snapshot.getKey(), userModel);
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_select, parent, false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            Glide.with
                    (holder.itemView.getContext())
                    .load(userModelList.get(position).profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(((CustomViewHolder)holder).imageView);

            ((CustomViewHolder)holder).textView.setText(userModelList.get(position).userName);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onBindViewHolder() > onClick()");
                    /*
                    Intent intent = new Intent(view.getContext(), GroupMessageActivity.class);
                    intent.putExtra("roomId", roomModel.roomId);

                    ActivityOptions activityOptions = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright, R.anim.toleft);
                        startActivity(intent, activityOptions.toBundle());
                    }
                    */
                }
            });

            if(userModelList.get(position).comment != null) {
                ((CustomViewHolder)holder).textView_comment.setText(userModelList.get(position).comment);
            }
            ((CustomViewHolder) holder).checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // 체크 된 상태
                    if(isChecked) {
                        roomModel.users.put(userModelList.get(position).uid, true);
                    } else {
                        roomModel.users.remove(userModelList.get(position).uid);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return userModelList.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_comment;
            public CheckBox checkBox;

            public CustomViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView)itemView.findViewById(R.id.frienditem_imageview);
                textView = (TextView) itemView.findViewById(R.id.frienditem_textview);
                textView_comment = (TextView) itemView.findViewById(R.id.frienditem_textview_comment);
                checkBox = (CheckBox) itemView.findViewById(R.id.frienditem_checkbox);
            }
        }
    }
}
