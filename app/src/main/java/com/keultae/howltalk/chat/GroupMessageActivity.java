package com.keultae.howltalk.chat;

import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.keultae.howltalk.R;
import com.keultae.howltalk.model.ChatModel;
import com.keultae.howltalk.model.NotificationModel;
import com.keultae.howltalk.model.UserModel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupMessageActivity extends AppCompatActivity {
    private final String TAG = "GroupMessageActivity";

    Map<String, UserModel> users = new HashMap<>();
    String destinationRoom;
    String uid;
    EditText editText;

    private DatabaseReference databaseReference;
//    private ValueEventListener valueEventListener;
    private ChildEventListener valueEventListener;
    private RecyclerView recyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    List<ChatModel.Comment> comments = new ArrayList<>();
    int peopleCount = 0;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);

        destinationRoom = getIntent().getStringExtra("destinationRoom");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        editText = (EditText)findViewById(R.id.groupMessageActivity_editText);
        Log.d(TAG, "onCreate() uid=" + uid + ", destinationRoom=" + destinationRoom);

        // TODO: 전체 유저를 모두 가져오는 것을 채팅방에 관련된 사용자 데이터만 가져오도록 수정 필요
        FirebaseDatabase.getInstance().getReference().child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot item: dataSnapshot.getChildren()) {
                            users.put(item.getKey(), item.getValue(UserModel.class));
                            Log.d(TAG, "onCreate() item.getKey()=" + item.getKey() + ", item.getValue(UserModel.class)=" + item.getValue(UserModel.class).toString());
                        }
                        init();
                        recyclerView = findViewById(R.id.groupMessageActivity_recyclerView);
                        recyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
                        recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


    }

    boolean[] keyboardShow = {false};
    void init() {
        // keyboard가 보이면 채팅 메시지 마지막이 보이도록 지정
        relativeLayout = (RelativeLayout) findViewById(R.id.groupMessageActivity);
        relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                relativeLayout.getWindowVisibleDisplayFrame(r);
                int heightDiff = recyclerView.getRootView().getHeight() - (r.bottom - r.top);
                Log.d(TAG, "onGlobalLayout() > recyclerView.getRootView().getHeight()=" + recyclerView.getRootView().getHeight() + ", rect=" + r.toString() + ", heightDiff=" + heightDiff);
                if(heightDiff > 252) {
                    if(!keyboardShow[0]) {
                        Log.d(TAG, "키보드 보임");
                        // 메시지가 없을때는 건너뜀
                        if(recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) {
                            recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
                        }
                        keyboardShow[0] = true;
                    }
                } else {
                    Log.d(TAG, "키보드 숨김");
                    keyboardShow[0] = false;
                }
            }
        });

        Button button = (Button)findViewById(R.id.groupMessageActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            Map<String, Boolean> usersMap;

            @Override
            public void onClick(View v) {
                // 채팅방의 유저들을 구함
                // TODO: 매번 가져오지 말고 변경이 있을때만 가져오도록 수정 필
                FirebaseDatabase.getInstance().getReference().child("chatrooms")
                        .child(destinationRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onClick() > onDataChange(1)");
                        usersMap = (Map<String, Boolean>) dataSnapshot.getValue();

                        // 채팅방 멤버에서 본인을 삭제
                        usersMap.remove(uid);

                        ChatModel.Comment comment = new ChatModel.Comment();
                        comment.uid = uid;
                        comment.message = editText.getText().toString();
                        comment.timestamp = ServerValue.TIMESTAMP;
                        // 안 읽은 사람
                        for(String item: usersMap.keySet()) {
                            comment.readUsers.put(item, false);
                        }

                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments").push().setValue(comment)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        // 채팅방 멤버들에게 메시지 전송
                                        for(String item: usersMap.keySet()) {
                                            sendGcm(users.get(item).pushToken);
                                        }

                                        editText.setText("");
                                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("timestamp").setValue(System.currentTimeMillis());
                                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("order").setValue(Long.MAX_VALUE - System.currentTimeMillis());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d(TAG, "onClick() > onCancelled(1)");
                    }
                });
            }
        });
    }

    void sendGcm(String pushToken) {
        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = pushToken;
        notificationModel.notification.title = userName;
        notificationModel.notification.text = editText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString();
        notificationModel.data.destinationRoom = destinationRoom;

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8")
                , gson.toJson(notificationModel));

        Log.d(TAG, "sendGcm() > destinationUserModel.pushToken=" + pushToken);
        Log.d(TAG, "sendGcm() > gson.toJson(notificationModel)=" + gson.toJson(notificationModel));

        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .addHeader("Authorization", "key=AAAACA0vKRQ:APA91bFDND0ASLIdCBZps0Und5JHNGvbjKmjx-LeU_FDCp-jkkKGFIRtpju0il9OKHnctSIMSZwpAT31m8ROVVVImYUjmk04s8ZaXisGACq_oFQrVgqDwFjNdCI687sUKWj0ZdJ2tE0w")
                .url("https://gcm-http.googleapis.com/gcm/send")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "sendGcm() > newCall() > onFailure()" + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "sendGcm() > newCall() > onResponse()");
            }
        });

    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Map<String, Integer> commentKeys = new HashMap<>();

        public GroupMessageRecyclerViewAdapter() {
            getMessageList();
        }

        void getMessageList() {
            Log.d(TAG, "getMessageList() > destinationRoom=" + destinationRoom);
            /*
            databaseReference =  FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments");
            valueEventListener =  databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    comments.clear();
                    Map<String, Object> readUsers = new HashMap<>();

                    for(DataSnapshot item: dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment_orgin = item.getValue(ChatModel.Comment.class);
                        ChatModel.Comment comment_modify = item.getValue(ChatModel.Comment.class);
                        comment_modify.readUsers.put(uid, true);

                        readUsers.put(key, comment_modify);
                        comments.add(comment_orgin);
                    }

                    // timeStamp가 불완전하게 입력되는 무한루프 버그 처리
                    // 메세지 읽음 표시 3 => 1:28
                    if(comments.size() > 0 ) {
                        if (comments.get(comments.size() - 1).readUsers.containsKey(uid)) {
                            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments").updateChildren(readUsers)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            notifyDataSetChanged();
                                            recyclerView.scrollToPosition(comments.size() - 1);
                                        }
                                    });
                        } else {
                            // 메시지 갱신
                            notifyDataSetChanged();
                            // 맨 마지막으로 이동
                            recyclerView.scrollToPosition(comments.size() - 1);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            */


            databaseReference =  FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments");
            valueEventListener = databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    String key = dataSnapshot.getKey();
                    ChatModel.Comment comment = dataSnapshot.getValue(ChatModel.Comment.class);
                    comment.key = key;
                    comments.add(comment);
                    commentKeys.put(key, comments.size() - 1);
                    Log.d(TAG, "onChildAdded() s=" + s + ", key=" + key + ", comment=" + comment.toString());

                    // 맨 마지막으로 이동
                    recyclerView.scrollToPosition(comments.size() - 1);
                    // 메시지 갱신
                    notifyDataSetChanged();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    String key = dataSnapshot.getKey();
                    ChatModel.Comment comment = dataSnapshot.getValue(ChatModel.Comment.class);
                    int index = commentKeys.get(key);
                    comments.set(index, comment);
                    Log.d(TAG, "onChildChanged() s=" + s + ", key=" + key + ", comment.toString()=" + comment.toString());
                    // 메시지 갱신
                    notifyDataSetChanged();
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved()");
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    Log.d(TAG, "onChildMoved() s=" + s);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled()");
                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            
            return new GroupMessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Log.d(TAG, "onBindViewHolder() position=" + position + ", message=" + comments.get(position).message + ", key=" + comments.get(position).key);

            GroupMessageViewHolder messageViewHolder = (GroupMessageViewHolder) holder;

            if(comments.get(position).uid.equals(uid)) {
                // 내가 보낸 메시지
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);

                setReadCounter(position, messageViewHolder.textView_readCounter_left);
            } else {
                // 상대방이 보낸 메시지
                Glide.with(holder.itemView.getContext())
                        .load(users.get(comments.get(position).uid) .profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textView_name.setText(users.get(comments.get(position).uid).userName);

                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);

                setReadCounter(position, messageViewHolder.textView_readCounter_right);
            }
            long unixTime = (long)comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);
        }

        void setReadCounterOld(final int position, final TextView textView) {
            if(peopleCount == 0 ) {
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("users")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();
                                peopleCount = users.size();
                                int count = peopleCount - comments.get(position).readUsers.size();
                                if (count > 0) {
                                    textView.setVisibility(View.VISIBLE);
                                    textView.setText(String.valueOf(count));
                                } else {
                                    textView.setVisibility(View.INVISIBLE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            } else {
                int count = peopleCount - comments.get(position).readUsers.size();
                if (count > 0) {
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(String.valueOf(count));
                } else {
                    textView.setVisibility(View.INVISIBLE);
                }
            }
        }

        void setReadCounter(final int position, final TextView textView) {
            Log.d(TAG, "setReadCounter() position=" + position + ", destinationRoom=" + destinationRoom);

            // 다른 사람이 작성한 메시지에 대해서 읽음 표시를 하면 onChildChanged() 이벤트가 발생 한다.
            if( !comments.get(position).uid.equals(uid) ) {
                if (((boolean) comments.get(position).readUsers.get(uid)) == false) {
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments")
                            .child(comments.get(position).key).child("readUsers").child(uid).setValue(true)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "setReadCounter() > onComplete() > task.isSuccessful()=" + task.isSuccessful());
                                }
                            });
                }
            }

            int unreadCount = 0;
            final Set<String> keys = comments.get(position).readUsers.keySet();

            for (String key : keys) {
                if (((boolean) comments.get(position).readUsers.get(key)) == false) {
                    unreadCount++;
                }
            }
            Log.d(TAG, "setReadCounter() position=" + position + ", chatRoomUid=" + destinationRoom + ", unreadCount=" + unreadCount);
            if (unreadCount > 0) {
                textView.setVisibility(View.VISIBLE);
                textView.setText(String.valueOf(unreadCount));
            } else {
                textView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class GroupMessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textView_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public GroupMessageViewHolder(View view) {
                super(view);
                textView_message = (TextView)view.findViewById(R.id.messageItem_textview_message);
                textView_name = (TextView)view.findViewById(R.id.messageItem_textview_name);
                imageView_profile = (ImageView)view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = (LinearLayout)view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayout_main = (LinearLayout)view.findViewById(R.id.messageItem_linearlayout_main);
                textView_timestamp = (TextView)view.findViewById(R.id.messageItem_textview_timestamp);
                textView_readCounter_left = (TextView)view.findViewById(R.id.messageItem_textview_readCounter_left);
                textView_readCounter_right = (TextView)view.findViewById(R.id.messageItem_textview_readCounter_right);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Log.d(TAG, "onBackPressed() valueEventListener="+valueEventListener);

        if(valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
        overridePendingTransition(R.anim.fromleft, R.anim.toright);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }
}
