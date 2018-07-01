package com.keultae.howltalk.fragment;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.keultae.howltalk.R;
import com.keultae.howltalk.chat.GroupMessageActivity;
import com.keultae.howltalk.model.RoomModel;
import com.keultae.howltalk.model.UserModel;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ChatFragment extends Fragment {
    private final String TAG = "ChatFragment";
    private ChatRecyclerViewAdapter chatRecyclerViewAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        chatRecyclerViewAdapter = new ChatRecyclerViewAdapter();
        recyclerView.setAdapter(chatRecyclerViewAdapter);

        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        private String uid;
        private Map<String, UserModel> userModelMap = new HashMap<>();
        private List<RoomModel> roomModelList = new ArrayList<>();

        public ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "ChatRecyclerViewAdapter() > uid=" + uid);
        }

        /**
         * 채팅방에 사용자 이미지를 표시하기 위해서 모든 사용자 정보를 가져옴
         */
        public void readUsers() {
            FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userModelMap.clear();

                    for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        userModelMap.put(snapshot.getKey(), snapshot.getValue(UserModel.class));
                    }
                    Log.i(TAG, "readUsers() > onDataChange() userModelMap.size()=" + userModelMap.size());

                    readChatRooms();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "readUsers() > onCancelled()" + databaseError.toString());
                }
            });
        }

        /**
         * 내가 속한 채팅방을 모두 읽음
         * 가장 최근에 메시지를 주고 받은 채팅방순으로 정렬
         */
        public void readChatRooms() {
            FirebaseDatabase.getInstance().getReference().child("rooms").orderByChild("users/"+uid).equalTo(true)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            roomModelList.clear();
                            for(DataSnapshot item: dataSnapshot.getChildren()) {
                                RoomModel roomModel = item.getValue(RoomModel.class);
                                roomModel.roomId = item.getKey();

//                                Log.d(TAG, "readChatRooms() > item.getKey()=" + item.getKey() + ", roomModel=" + roomModel.toString());

                                roomModelList.add(roomModel);
                            }
                            // 채팅 메시지가 최신인 채팅룸을 제일 먼저 표시
                            Collections.sort(roomModelList, new Descending());
                            Log.i(TAG, "readChatRooms() > roomModelList.size()=" + roomModelList.size());

                            notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "readChatRooms() > " + databaseError.toString());
                        }
                    });
        }

        class Descending implements Comparator<RoomModel> {

            @Override
            public int compare(RoomModel roomModel, RoomModel t1) {
                return (int) (roomModel.descTimestamp - t1.descTimestamp);    // 내림차순
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);

            // ViewHolder 메모리 절약하기 위해서 사용
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            final CustomViewHolder customViewHolder = (CustomViewHolder) holder;
            Log.d(TAG, "onBindViewHolder() > position=" + position + ", roomModelList.get(position)=" + roomModelList.get(position).toString());

            StringBuilder sb = new StringBuilder();
            String names = null;
            String profileImageUrl = null;

            if(roomModelList.get(position).users.size() == 2) {
                // 1:1 채팅
                for (String key : roomModelList.get(position).users.keySet()) {
                    if (uid.equals(key)) {
                        continue;
                    }
                    profileImageUrl = userModelMap.get(key).profileImageUrl;
                    names = userModelMap.get(key).userName;
                }
            } else {
                // 멀티 채팅
                for (String key : roomModelList.get(position).users.keySet()) {
                    if (uid.equals(key)) {
                        continue;
                    }
                    sb.append(userModelMap.get(key).userName);
                    sb.append(", ");
                }
                profileImageUrl = userModelMap.get(roomModelList.get(position).lastUid).profileImageUrl;
                names = sb.toString().substring(0, sb.length()-2);
                // 멀티 채팅인 경우 본인을 포함한 인원수를 표시
                names += " [" + roomModelList.get(position).users.size() + "]";
            }

            Glide.with(customViewHolder.itemView.getContext())
                    .load(profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(customViewHolder.imageView);
            customViewHolder.textView_title.setText(names);

            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, UserModel> roomUserModelMap = new HashMap<>(); // 채팅방 사용자
                    Log.d(TAG, "onClick()");

                    for (String key : roomModelList.get(position).users.keySet()) {
                        roomUserModelMap.put(key, userModelMap.get(key));
                    }

                    Intent intent = null;
                    intent = new Intent(getActivity().getBaseContext(), GroupMessageActivity.class);
                    intent.putExtra("roomId", roomModelList.get(position).roomId);
                    intent.putExtra("roomModel", roomModelList.get(position));
                    intent.putExtra("roomUserModelMap", (Serializable) roomUserModelMap);

                    ActivityOptions activityOptions = null;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        activityOptions = ActivityOptions.makeCustomAnimation(getActivity().getBaseContext(), R.anim.fromright, R.anim.toleft);
                        startActivity(intent, activityOptions.toBundle());
                    }
                }
            });

            customViewHolder.textView_last_message.setText(roomModelList.get(position).lastMessage);

            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            long unixTime = Long.MAX_VALUE - roomModelList.get(position).descTimestamp;
            Date date = new Date(unixTime);
            customViewHolder.textView_timestamp.setText(simpleDateFormat.format(date));
        }

        @Override
        public int getItemCount() {
            return roomModelList.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView_title;
            public TextView textView_last_message;
            public TextView textView_timestamp;

            public CustomViewHolder(View view) {
                super(view);

                imageView = (ImageView) view.findViewById(R.id.chatitem_imageview);
                textView_title = (TextView) view.findViewById(R.id.chatitem_textview_title);
                textView_last_message = (TextView) view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timestamp = (TextView)view.findViewById(R.id.chatitem_textview_timestamp);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        // 액티비티가 표시될때 최신 정보로 갱신하기 위해서 onResume()에서 사용자 정보를 읽음
        chatRecyclerViewAdapter.readUsers();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }
}
