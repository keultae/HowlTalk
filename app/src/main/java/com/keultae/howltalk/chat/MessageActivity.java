package com.keultae.howltalk.chat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.keultae.howltalk.LoginActivity;
import com.keultae.howltalk.MainActivity;
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
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {
    private final String TAG = "MessageActivity";

    private String destinationUid;
    private Button button;
    private EditText editText;
    private String uid;
    private String chatRoomUid;

    private RecyclerView recyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    int peopleCount = 0;
    private SoftKeyboard softKeyboard;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        destinationUid = getIntent().getStringExtra("destinationUid");
        Log.d("MessageActivity", "onCreate() uid="+uid);
        Log.d("MessageActivity", "onCreate() destinationUid="+destinationUid);

        button = (Button)findViewById(R.id.messageActivity_button);
        editText = (EditText)findViewById(R.id.messageActivity_editText);

        recyclerView = (RecyclerView)findViewById(R.id.messageActivity_recyclerView);
        Log.d(TAG, "init recyclerView.getHeight()=" + recyclerView.getHeight());

        relativeLayout = (RelativeLayout) findViewById(R.id.messageActivity);
        InputMethodManager controlManager = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);
        softKeyboard = new SoftKeyboard(relativeLayout, controlManager);

        softKeyboard.setSoftKeyboardCallback(new SoftKeyboard.SoftKeyboardChanged()
        {
            @Override
            public void onSoftKeyboardHide()
            {
                new Handler(Looper.getMainLooper()).post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "키보드 내려왔을때, recyclerView.getHeight()=" + recyclerView.getHeight());
                        Log.d(TAG, "editText.getTop()="+editText.getTop());
                        // 딜레이를 주지 않아도 스크롤이 이동되고 스크롤을 이동하는 메소드를 호출하지 않아도 스크롤이 끝에 위치해 있음.
//                        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
                    }
                });
            }

            @Override
            public void onSoftKeyboardShow()
            {
                new Handler(Looper.getMainLooper()).post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "키보드 올라왔을때, recyclerView.getHeight()=" + recyclerView.getHeight());
                        Log.d(TAG, "editText.getTop()="+editText.getTop());

                        // 딜레이를 주지 않으면 스크롤이 끝으로 이동되지 않음.
                        // 100 일때, 아이맥 에뮬레이터에서는 되는데, A5 스마트폰에서는 안됨
                        // 200, 250 일때, 아이맥 에뮬레이터에서는 되는데, A5 스마트폰에서는 되다가 가끔 안됨
                        // 300 일때, 아이맥 에뮬레이터에서는 되는데, A5 스마트폰에서는 되다가 아주 가끔 안됨, 키보드가 올라가고 메시지가 약간 텀을 두고 끝으로 이동
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
//                                recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
                            }
                        }, 300);
                    }
                });
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid, true);
                chatModel.users.put(destinationUid, true);

                if(chatRoomUid == null) {
                    button.setEnabled(false);

                    // push() 임의의 이름(채탕방 명)
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom();
                        }
                    });
                } else {
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = editText.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP;
                    Log.d("MessageActivity", "setOnClickListener() 1");
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d("MessageActivity", "setOnClickListener() 2");
                                    sendGcm();
                                    Log.d("MessageActivity", "setOnClickListener() 3");
                                    editText.setText("");
                                }
                            });

                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("timestamp").setValue(System.currentTimeMillis());
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("order").setValue(Long.MAX_VALUE - System.currentTimeMillis());

                }
            }
        });
        checkChatRoom();
    }

    void sendGcm() {
        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = destinationUserModel.pushToken;
        notificationModel.notification.title = userName;
        notificationModel.notification.text = editText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString();
//        notificationModel.data.destinationUid = destinationUserModel.uid;

        // 푸시를 전송하는 기기의 UID를 보내줘야 수신하는 기기에서 상대편을 확인할 수 있다.
        notificationModel.data.destinationUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8")
                , gson.toJson(notificationModel));

        Log.d("MessageActivity", "sendGcm() destinationUserModel.pushToken=" + destinationUserModel.pushToken);
        Log.d("MessageActivity", "sendGcm() gson.toJson(notificationModel)=" + gson.toJson(notificationModel));

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

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

    }

    void checkChatRoom() {
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid)
                .equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot item: dataSnapshot.getChildren()) {
                    ChatModel chatModel = item.getValue(ChatModel.class);
                    if(chatModel.users.containsKey(destinationUid) && chatModel.users.size() == 2) {
                        chatRoomUid = item.getKey();    // 방 ID
                        button.setEnabled(true);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MessageActivity.this);
//                        linearLayoutManager.setStackFromEnd(true);
//                        linearLayoutManager.setReverseLayout(true);
                        recyclerView.setLayoutManager(linearLayoutManager);

                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<ChatModel.Comment> comments;

        public RecyclerViewAdapter() {
            comments = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    destinationUserModel = dataSnapshot.getValue(UserModel.class);
                    Gson gson = new Gson();
                    Log.d("MessageActivity", "RecyclerViewAdapter.RecyclerViewAdapter() gson.toJson(notificationModel)=" + gson.toJson(destinationUserModel));
                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

        void getMessageList() {
            databaseReference =  FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments");
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
                            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").updateChildren(readUsers)
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
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);

            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder = (MessageViewHolder) holder;

            // 내가 보낸 메시지
            if(comments.get(position).uid.equals(uid)) {
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                Log.d("CHECK", String.valueOf(messageViewHolder.linearLayout_destination));
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
                setReadCounter(position, messageViewHolder.textView_readCounter_left);
            } else {
                // 상대방이 보낸 메시지
                Glide.with(holder.itemView.getContext())
                        .load(destinationUserModel.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textView_name.setText(destinationUserModel.userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
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

        void setReadCounter(final int position, final TextView textView) {
            if(peopleCount == 0 ) {
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("users")
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

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textView_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public MessageViewHolder(View view) {
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

        Log.d("MessageActivity", "onBackPressed() valueEventListener="+valueEventListener);

        if(valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
        overridePendingTransition(R.anim.fromleft, R.anim.toright);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("MessageActivity", "onNewIntent()");

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        destinationUid = getIntent().getStringExtra("destinationUid");
        destinationUid = intent.getStringExtra("destinationUid");
        Log.d("MessageActivity", "onNewIntent() uid="+uid);
        Log.d("MessageActivity", "onNewIntent() destinationUid="+destinationUid);

        checkChatRoom();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MessageActivity", "onDestroy()");

        softKeyboard.unRegisterSoftKeyboardCallback();
    }
}
