package com.keultae.howltalk.fragment;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.keultae.howltalk.chat.MessageActivity;
import com.keultae.howltalk.model.ChatModel;
import com.keultae.howltalk.model.UserModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class ChatFragment extends Fragment {
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));

        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<ChatModel> chatModels = new ArrayList<>();
        private List<String> keys = new ArrayList<>();
        private String uid;
        private ArrayList<String> destinationUsers = new ArrayList<>();

        public ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Log.d("", "uid=" + uid);

            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            chatModels.clear();
                            for(DataSnapshot item: dataSnapshot.getChildren()) {
                                Log.d("", "item=" + item.toString());
                                chatModels.add(item.getValue(ChatModel.class));
                                keys.add(item.getKey());
                            }
                            Log.d("", "chatModels.size():" + chatModels.size());
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);

            // ViewHolder 메모리 절약하기 위해서 사용
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            final CustomViewHolder customViewHolder = (CustomViewHolder)holder;
            String destinationUid = null;

            Log.d("onBindViewHolder()", "position=" + position);

            // 챗방에 있는 유저를 모두 체크
            for(String user: chatModels.get(position).users.keySet()) {
                if(!user.equals(uid)) {
                    destinationUid = user;
                    destinationUsers.add(destinationUid);
                }
            }
            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
                            Glide.with(customViewHolder.itemView.getContext())
                                    .load(userModel.profileImageUrl)
                                    .apply(new RequestOptions().circleCrop())
                                    .into(customViewHolder.imageView);

                            customViewHolder.textView_title.setText(userModel.userName);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

            // 메시지를 내림 차순으로 정렬 후 마지막 메시지의 키 값을 가져옴
            Map<String, ChatModel.Comment> commentMap = new TreeMap<>(Collections.reverseOrder());
            Log.d("", "comments=" + chatModels.get(position).comments.toString());
            commentMap.putAll(chatModels.get(position).comments);

            if(commentMap.keySet().toArray().length > 0) {
                String lastMesssageKey = (String) commentMap.keySet().toArray()[0];
                Log.d("", "lastMesssageKey=" + lastMesssageKey);
                Log.d("", "message=" + chatModels.get(position).comments.get(lastMesssageKey).message);
                customViewHolder.textView_last_message.setText(chatModels.get(position).comments.get(lastMesssageKey).message);

                // TimeStamp
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                long unixTime = (long) chatModels.get(position).comments.get(lastMesssageKey).timestamp;
                Date date = new Date(unixTime);
                customViewHolder.textView_timestamp.setText(simpleDateFormat.format(date));
            }
            // 단체 채팅방 3 01:42 동영상에는 아래 코드가 있는데, 따라서 입력하면 오류가 발생
            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = null;
                    if(chatModels.get(position).users.size() > 2) {
                        intent = new Intent(v.getContext(), GroupMessageActivity.class);
                        intent.putExtra("destinationRoom", keys.get(position));
                    } else {
                        intent = new Intent(v.getContext(), MessageActivity.class);
                        intent.putExtra("destinationUid", destinationUsers.get(position));
                    }

                    ActivityOptions activityOptions = null;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        activityOptions = ActivityOptions.makeCustomAnimation(v.getContext(), R.anim.fromright, R.anim.toleft);
                        startActivity(intent, activityOptions.toBundle());
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return chatModels.size();
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
}
