package com.keultae.howltalk.chat;

import android.content.Intent;
import android.graphics.Rect;
import android.os.StrictMode;
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
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.keultae.howltalk.model.DataMessageModel;
import com.keultae.howltalk.model.MessageModel;
import com.keultae.howltalk.model.NotificationModel;
import com.keultae.howltalk.model.RoomModel;
import com.keultae.howltalk.model.UserModel;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupMessageActivity extends AppCompatActivity {
    private final String TAG = "GroupMessageActivity";

//    Map<String, UserModel> users = new HashMap<>();
    private String chatRoomId;
    private RoomModel roomModel;

    private String uid;
    private EditText editText;

    private DatabaseReference databaseReference;
    private ChildEventListener valueEventListener;
    private RecyclerView recyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private List<MessageModel> messages = new ArrayList<>();
//    private int peopleCount = 0;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);

        editText = (EditText)findViewById(R.id.groupMessageActivity_editText);

        // sendFcm() 호출시 네트웍 에러를 해결하기 위한 코드
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        roomModel = (RoomModel) getIntent().getSerializableExtra("roomModel");

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if(roomModel != null) {
            Log.d(TAG, "onCreate() uid=" + uid + ", chatRoomId=" + chatRoomId + ", roomModel=" + roomModel.toString());
        } else {
            Log.d(TAG, "onCreate() uid=" + uid + ", chatRoomId=" + chatRoomId + ", roomModel=null");
        }

        if(roomModel != null) {
            // 앱에서 호출하면 rooomModel를 넘겨줌
            init();
        } else {
            // 푸시 메시지로 받았을때는 roomModel이 없음
            FirebaseDatabase.getInstance().getReference().child("rooms").child(chatRoomId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Log.d(TAG, "onCreate() > onDataChange() ");
                            roomModel = dataSnapshot.getValue(RoomModel.class);

                            init();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.d(TAG, "onCancelled() databaseError="+databaseError.toString());
                        }
                    });
        }
    }

    boolean[] keyboardShow = {false};
    void init() {
        recyclerView = findViewById(R.id.groupMessageActivity_recyclerView);
        recyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));

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
            Map<String, String> names;

            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick()");

                MessageModel messageModel = new MessageModel();
                messageModel.uid = uid;
                messageModel.message = editText.getText().toString();
                messageModel.timestamp = ServerValue.TIMESTAMP;
                for(String tmpUid: roomModel.users.keySet()) {
                    if(!tmpUid.equals(uid)) {
                        messageModel.readUsers.put(tmpUid, false);
                    }
                }

                final String chattingId = FirebaseDatabase.getInstance().getReference().child("messages").child(chatRoomId).push().getKey();

                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("/rooms/" + chatRoomId + "/descTimestamp", Long.MAX_VALUE - System.currentTimeMillis() );
                childUpdates.put("/rooms/" + chatRoomId + "/lastMessage",  messageModel.message);
                childUpdates.put("/messages/" + chatRoomId + "/" + chattingId, messageModel.toMap());

                FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "메시지 저장 > onSuccess() > 생성 chattingId=" + chattingId);
                                // 채팅방 멤버들에게 메시지 전송
                                for(String tmpUid: roomModel.users.keySet()) {
                                    if(!tmpUid.equals(uid)) {
                                        // sendGcm(roomModel.users.get(tmpUid).pushToken);
                                        sendFcm(chatRoomId, editText.getText().toString(), roomModel.users.get(tmpUid).pushToken);
                                    }
                                }

                                editText.setText("");
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
//        notificationModel.notification.title = userName;
//        notificationModel.notification.text = editText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString();
        notificationModel.data.destinationRoom = chatRoomId;

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

    void sendFcm(String chatRoomId, String message, String pushToken) {
        HttpsURLConnection con;
        try {
            Gson gson = new Gson();
            String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            String json;
            DataMessageModel dataMessageModel = new DataMessageModel();
            dataMessageModel.to = pushToken;
            dataMessageModel.data.senderName = name;
            dataMessageModel.data.message = message;
            dataMessageModel.data.chatRoomId = chatRoomId;
            json = gson.toJson(dataMessageModel);

            String url = "https://fcm.googleapis.com/fcm/send";
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            URL obj = new URL(url);
            con = (HttpsURLConnection) obj.openConnection();

            //reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "key=AAAACA0vKRQ:APA91bFDND0ASLIdCBZps0Und5JHNGvbjKmjx-LeU_FDCp-jkkKGFIRtpju0il9OKHnctSIMSZwpAT31m8ROVVVImYUjmk04s8ZaXisGACq_oFQrVgqDwFjNdCI687sUKWj0ZdJ2tE0w");
            Log.d(TAG, "sendFcm() > json=" + json);

            //post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(json.getBytes("UTF-8"));
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            Log.d(TAG, "sendFcm() > responseCode=" + responseCode);

            StringBuffer response = new StringBuffer();

            if (responseCode != 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.d(TAG, "sendFcm() > response.toString()=" + response.toString());
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Map<String, Integer> messageKeys = new HashMap<>();

        public GroupMessageRecyclerViewAdapter() {
            getMessageList();
        }

        void getMessageList() {
            Log.d(TAG, "getMessageList() > chatRoomId=" + chatRoomId);

            databaseReference =  FirebaseDatabase.getInstance().getReference().child("messages").child(chatRoomId);
            valueEventListener = databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    String messageId = dataSnapshot.getKey();
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    message.messageId = messageId;
                    messages.add(message);
                    messageKeys.put(messageId, messages.size() - 1);
                    Log.d(TAG, "onChildAdded() s=" + s + ", messageId=" + messageId + ", message=" + message.toString());

                    // 맨 마지막으로 이동
                    recyclerView.scrollToPosition(messages.size() - 1);
                    // 메시지 갱신
                    notifyDataSetChanged();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    String messageId = dataSnapshot.getKey();
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    int index = messageKeys.get(messageId);
                    messages.set(index, message);
                    Log.d(TAG, "onChildChanged() s=" + s + ", messageId=" + messageId + ", comment.toString()=" + message.toString());
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
            Log.d(TAG, "onBindViewHolder() position=" + position + ", message=" + messages.get(position).message + ", messageId=" + messages.get(position).messageId);

            GroupMessageViewHolder messageViewHolder = (GroupMessageViewHolder) holder;

            messageViewHolder.textView_readCounter_left.setVisibility(View.INVISIBLE);
            messageViewHolder.textView_readCounter_right.setVisibility(View.INVISIBLE);

            if(messages.get(position).uid.equals(uid)) {
                // 내가 보낸 메시지
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);

                messageViewHolder.textView_message.setText(messages.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.textView_message.setTextSize(25);

                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);

                setReadCounter(position, messageViewHolder.textView_readCounter_left);
            } else {
                // 상대방이 보낸 메시지
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(roomModel.users.get(messages.get(position).uid).profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);

                messageViewHolder.textView_name.setText(roomModel.users.get(messages.get(position).uid).userName);

                messageViewHolder.textView_message.setText(messages.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setTextSize(25);

                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);

                setReadCounter(position, messageViewHolder.textView_readCounter_right);
            }
            long unixTime = (long)messages.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);
        }

        void setReadCounter(final int position, final TextView textView) {
            Log.d(TAG, "setReadCounter() position=" + position + ", chatRoomId=" + chatRoomId);

            // 다른 사람이 작성한 메시지에 대해서 읽음 표시를 하면 onChildChanged() 이벤트가 발생 한다.
            if( !messages.get(position).uid.equals(uid) ) {
                if (((boolean) messages.get(position).readUsers.get(uid)) == false) {
                    FirebaseDatabase.getInstance().getReference().child("messages").child(chatRoomId)
                            .child(messages.get(position).messageId).child("readUsers").child(uid).setValue(true)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "setReadCounter() > onComplete() > task.isSuccessful()=" + task.isSuccessful());
                                }
                            });
                }
            }

            int unreadCount = 0;
            final Set<String> keys = messages.get(position).readUsers.keySet();

            for (String key : keys) {
                if (((boolean) messages.get(position).readUsers.get(key)) == false) {
                    unreadCount++;
                }
            }
            Log.d(TAG, "setReadCounter() position=" + position + ", chatRoomUid=" + chatRoomId + ", unreadCount=" + unreadCount);
            if (unreadCount > 0) {
                textView.setVisibility(View.VISIBLE);
                textView.setText(String.valueOf(unreadCount));
            } else {
                textView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
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

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRoomId = intent.getStringExtra("chatRoomId");
        Log.d(TAG, "onNewIntent() uid="+uid);
        Log.d(TAG, "onNewIntent() chatRoomId="+chatRoomId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }
}
