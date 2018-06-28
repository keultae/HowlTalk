package com.keultae.howltalk.chat;

import android.app.ActivityManager;
import android.content.Context;
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
import android.widget.Toast;

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
import com.keultae.howltalk.MainActivity;
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
import java.util.Iterator;
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

public class MessageActivity extends AppCompatActivity {
    private final String TAG = "MessageActivity";

    private String destinationUid;
    private String destinationUserName;
    private Button button;
    private EditText editText;
    private String uid;
    private String chatRoomUid;

    private RecyclerView recyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ChildEventListener valueEventListener;
    int peopleCount = 0;
//    private SoftKeyboard softKeyboard;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // sendFcm() 호출시 네트웍 에러를 해결하기 위한 코드
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        destinationUid = getIntent().getStringExtra("destinationUid");
        destinationUserName = getIntent().getStringExtra("destinationUserName");
        Log.d("MessageActivity", "onCreate() uid="+uid + ", destinationUid="+destinationUid + ", destinationUserName="+destinationUserName);

        button = (Button)findViewById(R.id.messageActivity_button);
        editText = (EditText)findViewById(R.id.messageActivity_editText);
        recyclerView = (RecyclerView)findViewById(R.id.messageActivity_recyclerView);
        relativeLayout = (RelativeLayout) findViewById(R.id.messageActivity);

        // keyboard가 보이면 채팅 메시지 마지막이 보이도록 지정
        final boolean[] keyboardShow = {false};
        relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                relativeLayout.getWindowVisibleDisplayFrame(r);
                int heightDiff = recyclerView.getRootView().getHeight() - (r.bottom - r.top);
                Log.d(TAG, "recyclerView.getRootView().getHeight()=" + recyclerView.getRootView().getHeight() + ", rect=" + r.toString() + ", heightDiff=" + heightDiff);
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

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick() chatRoomUid=" + chatRoomUid + ", uid=" + uid + ", destinationUid=" + destinationUid);
                // 메시지 전송이 정상적으로 될때까지 다시 전송하지 못하도록 막음
                button.setEnabled(false);

                MessageModel messageModel = new MessageModel();
                messageModel.uid = uid;
                messageModel.message = editText.getText().toString();
                messageModel.timestamp = ServerValue.TIMESTAMP;
                messageModel.readUsers.put(destinationUid, false);

                final String chattingId = FirebaseDatabase.getInstance().getReference().child("messages").child(chatRoomUid).push().getKey();

                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("/rooms/" + chatRoomUid + "/descTimestamp", Long.MAX_VALUE - System.currentTimeMillis() );
                childUpdates.put("/rooms/" + chatRoomUid + "/lastMessage",  messageModel.message);
                childUpdates.put("/messages/" + chatRoomUid + "/" + chattingId, messageModel.toMap());

                FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "메시지 저장 > onSuccess() > 생성 chattingId=" + chattingId);
                            }
                        });
            }
        });

        checkChatRoomOrCreate();
    }


    /**
     * 1:1 채팅방 ID를 찾고 없으면 생성
     */
    void checkChatRoomOrCreate() {
        Log.d(TAG, "checkChatRoomOrCreate()");

        FirebaseDatabase.getInstance().getReference().child("rooms").orderByChild("user/uids/"+uid)
                .equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 조건에 맞는 검색 결과가 없으면 dataSnapshot.getChildrenCount()가 0 이 됨
                Log.d(TAG, "checkChatRoomOrCreate() > onDataChange() dataSnapshot.getChildrenCount()=" + dataSnapshot.getChildrenCount() +
                        ", destinationUid=" + destinationUid);

                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    RoomModel roomModel = item.getValue(RoomModel.class);
                    Log.d(TAG, roomModel.toString());
                    if (roomModel.user.uids.containsKey(destinationUid) && roomModel.user.uids.size() == 2) {
                        chatRoomUid = item.getKey();    // 방 ID
                        Log.d(TAG, "checkChatRoomOrCreate() > onDataChange() > 검색 chatRoomUid=" + chatRoomUid);

                        init();
                        break;
                    }
                }

                if( chatRoomUid == null ) {
                    final String roomId = FirebaseDatabase.getInstance().getReference().child("rooms").push().getKey();
                    final String chattingId = FirebaseDatabase.getInstance().getReference().child("messages").child(roomId).push().getKey();
                    String initMessage = destinationUserName + "을 초대합니다.";
                    RoomModel roomModel = new RoomModel();
                    roomModel.descTimestamp = Long.MAX_VALUE - System.currentTimeMillis();
                    roomModel.lastMessage = initMessage;

                    roomModel.user.uids.put(uid, true);
                    roomModel.user.uids.put(destinationUid, true);

                    roomModel.user.names.put(uid, FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                    roomModel.user.names.put(destinationUid, destinationUserName);

                    MessageModel messageModel = new MessageModel();
                    messageModel.uid = uid;
                    messageModel.timestamp = System.currentTimeMillis();
                    messageModel.message = initMessage;
                    messageModel.readUsers.put(destinationUid, false);

                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/rooms/" + roomId, roomModel.toMap());
                    childUpdates.put("/messages/" + roomId + "/" + chattingId, messageModel.toMap());

                    FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    chatRoomUid = roomId;
                                    Log.d(TAG, "checkChatRoomOrCreate() > onSuccess() > 생성 roomId=" + chatRoomUid);

                                    init();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "checkChatRoomOrCreate() > onCancelled() > chatRoomUid=" + chatRoomUid);
            }
        });

        Map<String, Object> childUpdates = new HashMap<>();
        UserModel userModel;

        userModel = new UserModel();
        userModel.profileImageUrl = "https://firebasestorage.googleapis.com/v0/b/howltalk-b6b11.appspot.com/o/userImages%2FBDzAYwXZrwVmsB8AgQLjNVLP6sE3?alt=media&token=4d772032-fe0c-4793-bb0e-61125bb56ad6";
        userModel.pushToken = "eNpf5pwHfRM:APA91bG_donXGmUX1MNFbQD4EqOlRFozpnZ1GSLYAAeP4TPrpfyt39Kkvb17aH6Kv8F3VZZY66sTYUf8TyyK5tYIOm8qZ2DXuvij-EBq-3Y3nULl54abWx4whwabQi-2OgZhuNK6wBH5vEQQfc6WMrSLE1-ypmESig";
        userModel.uid = "BDzAYwXZrwVmsB8AgQLjNVLP6sE3";
        userModel.userName = "A5";
        childUpdates.put("/test/users/BDzAYwXZrwVmsB8AgQLjNVLP6sE3", userModel.toMap());

        userModel = new UserModel();
        userModel.profileImageUrl = "https://firebasestorage.googleapis.com/v0/b/howltalk-b6b11.appspot.com/o/userImages%2FBDzAYwXZrwVmsB8AgQLjNVLP6sE3?alt=media&token=4d772032-fe0c-4793-bb0e-61125bb56ad6";
        userModel.pushToken = "eNpf5pwHfRM:APA91bG_donXGmUX1MNFbQD4EqOlRFozpnZ1GSLYAAeP4TPrpfyt39Kkvb17aH6Kv8F3VZZY66sTYUf8TyyK5tYIOm8qZ2DXuvij-EBq-3Y3nULl54abWx4whwabQi-2OgZhuNK6wBH5vEQQfc6WMrSLE1-ypmESig";
        userModel.uid = "KHOp1dIuPoaT7NhKfrOP48kqgxC3";
        userModel.userName = "Tab";
        childUpdates.put("/test/users/KHOp1dIuPoaT7NhKfrOP48kqgxC3", userModel.toMap());

        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "배열 저장 > onSuccess()");

                        FirebaseDatabase.getInstance().getReference().child("test/users").orderByChild("uid")
                                .equalTo("BDzAYwXZrwVmsB8AgQLjNVLP6sE3").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                // 조건에 맞는 검색 결과가 없으면 dataSnapshot.getChildrenCount()가 0 이 됨
                                Log.d(TAG, "배열 검색 > onDataChange(), dataSnapshot.getChildrenCount()=" + dataSnapshot.getChildrenCount());

                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                    UserModel userModel = item.getValue(UserModel.class);
                                    Log.d(TAG, "item.getKey()=" + item.getKey() + ", userModel=" + userModel.toString());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.d(TAG, "배열 검색 > onCancelled()");
                            }
                        });
                    }
                });


    }

    void init() {
        // 메시지 전송 버튼을 활성화
        button.setEnabled(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MessageActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(new RecyclerViewAdapter());
    }

    /**
     * 1:1 채팅방의 방ID를 찾음
     */
    void checkChatRoom() {
        Log.d(TAG, "checkChatRoom()");
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid)
                .equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 조건에 맞는 검색 결과가 없으면 dataSnapshot.getChildrenCount()가 0 이 됨
                Log.d(TAG, "checkChatRoom() > onDataChange() dataSnapshot.getChildrenCount()=" + dataSnapshot.getChildrenCount() +
                ", destinationUid=" + destinationUid);

                for(DataSnapshot item: dataSnapshot.getChildren()) {
                    ChatModel chatModel = item.getValue(ChatModel.class);
                    if(chatModel.users.containsKey(destinationUid) && chatModel.users.size() == 2) {
                        chatRoomUid = item.getKey();    // 방 ID
                        button.setEnabled(true);
                        Log.d(TAG, "checkChatRoom() > onDataChange() > chatRoomUid=" + chatRoomUid);

                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MessageActivity.this);
                        recyclerView.setLayoutManager(linearLayoutManager);
                        recyclerView.setAdapter(new RecyclerViewAdapter());

                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "checkChatRoom() > onCancelled() > chatRoomUid=" + chatRoomUid);
            }
        });
    }

    void sendGcm() {
        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = destinationUserModel.pushToken;
//        notificationModel.notification.title = userName;
//        notificationModel.notification.text = editText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString();
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
                Log.d(TAG, "sendGcm() > onFailure() " + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "sendGcm() > onResponse() " + response.toString());

            }
        });

    }

    void sendFcm(String chatRoomUid, String message) throws Exception {
        Gson gson = new Gson();
        String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        String json;
        DataMessageModel dataMessageModel = new DataMessageModel();
        dataMessageModel.to = destinationUserModel.pushToken;
        dataMessageModel.data.senderName = name;
        dataMessageModel.data.message = message;
        dataMessageModel.data.chatRoomId = chatRoomUid;
        // 푸시를 전송하는 기기의 UID를 보내줘야 수신하는 기기에서 상대편을 확인할 수 있다.
        dataMessageModel.data.destinationUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        json = gson.toJson(dataMessageModel);

        String url = "https://fcm.googleapis.com/fcm/send";
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

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

        if(responseCode != 200){
            BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            Log.d(TAG, "sendFcm() > response.toString()=" + response.toString());
        }
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<MessageModel> messages;
        Map<String, Integer> messageIds;

        public RecyclerViewAdapter() {
            messages = new ArrayList<>();
            messageIds = new HashMap<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    destinationUserModel = dataSnapshot.getValue(UserModel.class);
//                    Gson gson = new Gson();
//                    Log.d(TAG, "RecyclerViewAdapter.RecyclerViewAdapter() gson.toJson(notificationModel)=" + gson.toJson(destinationUserModel));
                    Log.d(TAG, "RecyclerViewAdapter.RecyclerViewAdapter() destinationUserModel=" + destinationUserModel.toString());
                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

        void getMessageList() {
            Log.d(TAG, "RecyclerViewAdapter.getMessageList()");

            databaseReference =  FirebaseDatabase.getInstance().getReference().child("messages").child(chatRoomUid);
            valueEventListener = databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    String messageId = dataSnapshot.getKey();
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    message.messageId = messageId;
                    messages.add(message);
                    messageIds.put(messageId, messages.size() - 1);
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
                    int index = messageIds.get(messageId);
                    messages.set(index, message);
                    Log.d(TAG, "onChildChanged() s=" + s + ", messageId=" + messageId + ", message=" + message.toString());
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
            Log.d(TAG, "onCreateViewHolder()");

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);

            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Log.d(TAG, "onBindViewHolder() position=" + position + ", message=" + messages.get(position).message);

            MessageViewHolder messageViewHolder = (MessageViewHolder) holder;

            // 내가 보낸 메시지
            if(messages.get(position).uid.equals(uid)) {
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);

                messageViewHolder.textView_message.setText(messages.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);

                setReadCounter(position, messageViewHolder.textView_readCounter_left);
            } else {
                // 상대방이 보낸 메시지
                // 사진
                Glide.with(holder.itemView.getContext())
                        .load(destinationUserModel.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textView_name.setText(destinationUserModel.userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);

                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(messages.get(position).message);
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
            Log.d(TAG, "setReadCounter() position=" + position + ", chatRoomUid=" + chatRoomUid);

            // 다른 사람이 작성한 메시지에 대해서 읽음 표시를 하면 onChildChanged() 이벤트가 발생 한다.
            if( !messages.get(position).uid.equals(uid) ) {
                if (((boolean) messages.get(position).readUsers.get(uid)) == false) {
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments")
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
            Log.d(TAG, "setReadCounter() position=" + position + ", chatRoomUid=" + chatRoomUid + ", unreadCount=" + unreadCount);
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

    /**
     * 앱의 백스택 정보와 액티비티 정보를 가져옴
     */
    public void dump(Context context) {
        StringBuffer sb = new StringBuffer();

        // 프로그라운드와 백그라운드에서 실행 중인 앱의 최상위 액티비티 정보를 가져온다.
        ActivityManager activity_manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> task_info = activity_manager.getRunningTasks(9999);
        for(int i=0; i<task_info.size(); i++) {
            sb.append("[" + i + "] activity:"+ task_info.get(i).topActivity.getPackageName() + " >> " + task_info.get(i).topActivity.getClassName());
            sb.append("\r\n");
        }

        // 앱이 실행 됐을때 기본 액티비티와 최상위 액티비티 정보, 백스택의 액티비티 개수를 가져온다.
        // 중간 액티비티 목록을 확인하지는 못함
        ActivityManager m = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfoList = m.getRunningTasks(10);
        Iterator<ActivityManager.RunningTaskInfo> itr = runningTaskInfoList.iterator();
        while (itr.hasNext()) {
            ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) itr.next();
            int id = runningTaskInfo.id;
            CharSequence desc = runningTaskInfo.description;
            int numOfActivities = runningTaskInfo.numActivities;
            String baseActivity = runningTaskInfo.baseActivity.getShortClassName();
            String topActivity = runningTaskInfo.topActivity.getShortClassName();

            sb.append("id=" +id + ", desc="+desc+ ", numOfActivities="+numOfActivities+
                    ", topActivity="+topActivity + ", baseActivity=" + baseActivity);
            sb.append("\r\n");
        }
        Log.d(TAG, sb.toString());

//        Toast.makeText(context, "uid=" + FirebaseAuth.getInstance().getCurrentUser().getUid()
//                + ", " + sb.toString(), Toast.LENGTH_LONG).show();
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
        destinationUid = intent.getStringExtra("destinationUid");
        Log.d(TAG, "onNewIntent() uid="+uid);
        Log.d(TAG, "onNewIntent() destinationUid="+destinationUid);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }
}
