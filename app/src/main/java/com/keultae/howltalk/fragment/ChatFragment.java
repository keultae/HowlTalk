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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class ChatFragment extends Fragment {
    private final String TAG = "ChatFragment";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
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
        private List<ChatModel> chatModels = new ArrayList<>();
        private List<String> keys = new ArrayList<>();
        private String uid;
        private ArrayList<String> destinationUsers = new ArrayList<>();

        public ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Log.d(TAG, "ChatRecyclerViewAdapter() > uid=" + uid);

            /*
//            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid)
            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("order")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            chatModels.clear();
                            for(DataSnapshot item: dataSnapshot.getChildren()) {
                                ChatModel chatModel = item.getValue(ChatModel.class);
                                Log.d(TAG, "ChatRecyclerViewAdapter() > chatModel=" + chatModel.toString());
                                Log.d(TAG, "ChatRecyclerViewAdapter() > item.getKey()=" + item.getKey());

                                // 본인이 포함된 채팅방만 보여줌
//                                if(chatModel.users.get(uid) != null) {
                                    chatModels.add(chatModel);
                                    keys.add(item.getKey());
//                                }
                            }
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                    */
//            refrech();
        }

        public void refrech() {
//            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("order")
//            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByKey()
//            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByValue() // orderByKey()와 결과 동일
            // 모든 값이 검색됨, uid가 null 값이 가장 앞에 나오고 uid 값이 true이면 키 순서로 정렬
//            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid)
            // uid 키의 값이 모두 true이므로 uid가 있는 값만 검색됨
            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            chatModels.clear();
                            for(DataSnapshot item: dataSnapshot.getChildren()) {
                                ChatModel chatModel = item.getValue(ChatModel.class);
                                Log.d(TAG, "ChatRecyclerViewAdapter() > chatModel=" + chatModel.toString());
                                Log.d(TAG, "ChatRecyclerViewAdapter() > item.getKey()=" + item.getKey());

                                // 본인이 포함된 채팅방만 보여줌
                                if(chatModel.users.get(uid) != null) {
                                    chatModels.add(chatModel);
                                    keys.add(item.getKey());
                                }
                            }

                            // 첫번째와 마지막 값을 바꿈
//                            ChatModel cm = chatModels.get(0);
//                            chatModels.set(0, chatModels.get(chatModels.size()-1));
//                            chatModels.set(chatModels.size()-1, cm);
                            Collections.sort(chatModels, new Descending());
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
        }

        class Descending implements Comparator<ChatModel> {

            @Override
            public int compare(ChatModel chatModel, ChatModel t1) {
                return (int) (chatModel.order - t1.order);    // 내림차순
//                return (int) (t1.order - chatModel.order);  // 오름차순
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
            final CustomViewHolder customViewHolder = (CustomViewHolder)holder;
            String destinationUid = null;

            Log.d(TAG, "onBindViewHolder() > position=" + position);

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
            Log.d(TAG, "onBindViewHolder() > comments=" + chatModels.get(position).comments.toString());
            commentMap.putAll(chatModels.get(position).comments);

            if(commentMap.keySet().toArray().length > 0) {
                String lastMesssageKey = (String) commentMap.keySet().toArray()[0];
                Log.d(TAG, "onBindViewHolder() > lastMesssageKey=" + lastMesssageKey);
                Log.d(TAG, "onBindViewHolder() > message=" + chatModels.get(position).comments.get(lastMesssageKey).message);
                customViewHolder.textView_last_message.setText(chatModels.get(position).comments.get(lastMesssageKey).message);

                // TimeStamp
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
//                long unixTime = (long) chatModels.get(position).comments.get(lastMesssageKey).timestamp;
                // 1529112081051, 1529200078125, 1529223003360
                long unixTime = chatModels.get(position).timestamp;
                Date date = new Date(unixTime);
                customViewHolder.textView_timestamp.setText(simpleDateFormat.format(date));
            }
            // 단체 채팅방 3 01:42 동영상에는 아래 코드가 있는데, 따라서 입력하면 오류가 발생
            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = null;
//                    if(chatModels.get(position).users.size() > 2) {
//                        intent = new Intent(v.getContext(), GroupMessageActivity.class);
//                        intent.putExtra("destinationRoom", keys.get(position));
//                    } else {
//                        intent = new Intent(v.getContext(), MessageActivity.class);
//                        intent.putExtra("destinationUid", destinationUsers.get(position));
//                    }

                    intent = new Intent(v.getContext(), GroupMessageActivity.class);
                    intent.putExtra("destinationRoom", keys.get(position));

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

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        chatRecyclerViewAdapter.refrech();
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
