package com.keultae.howltalk.fragment;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeopleFragment extends Fragment{
    private final String TAG = "PeopleFragment";

    private PeopleFragmentRecyclerViewAdapter peopleFragmentRecyclerViewAdapter;
    private List<UserModel> userModelList = new ArrayList<>();      // 본인을 제외한 모든 사용자
    private UserModel myUserModel;          // 본인


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);
        Log.d(TAG, "onCreateView()");

        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        peopleFragmentRecyclerViewAdapter = new PeopleFragmentRecyclerViewAdapter();
        recyclerView.setAdapter(peopleFragmentRecyclerViewAdapter);

        FloatingActionButton floatingActionButton = (FloatingActionButton) view.findViewById(R.id.peoplefragment_floatingButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getView().getContext(), SelectFriendActivity.class);
                intent.putExtra("userModel", myUserModel);

                ActivityOptions activityOptions = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    activityOptions = ActivityOptions.makeCustomAnimation(getView().getContext(), R.anim.fromright, R.anim.toleft);
                    startActivity(intent, activityOptions.toBundle());
                }
            }
        });
        return view;
    }

    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        public PeopleFragmentRecyclerViewAdapter() {
            Log.d(TAG, "PeopleFragmentRecyclerViewAdapter() uid="+uid);
        }

        public void readUsers() {
            FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userModelList.clear();

                    for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);

                        if(userModel.uid.equals(uid)) {
                            myUserModel = userModel;
                            continue;
                        }
                        userModelList.add(userModel);
                    }
                    Log.i(TAG, "readUsers() > onDataChange() userModelList.size()=" + userModelList.size());

                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "readUsers() > onCancelled() " + databaseError.toString());
                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder() > viewType=" + viewType);
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            Log.d(TAG, "onBindViewHolder() > position=" + position);
            CustomViewHolder customViewHolder = (CustomViewHolder)holder;

            // URL의 이미지를 View에 지정
            Glide.with(holder.itemView.getContext())
                    .load(userModelList.get(position).profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(customViewHolder.imageView);

            customViewHolder.textView.setText(userModelList.get(position).userName);

            if(userModelList.get(position).comment != null) {
                customViewHolder.textView_comment.setText(userModelList.get(position).comment);
//                customViewHolder.textView_comment.setBackgroundResource(R.drawable.rightbubble);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchOrCreateRoom(position, userModelList.get(position));
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

            public CustomViewHolder(View itemView) {
                super(itemView);

                imageView = (ImageView)itemView.findViewById(R.id.frienditem_imageview);
                textView = (TextView) itemView.findViewById(R.id.frienditem_textview);
                textView_comment = (TextView) itemView.findViewById(R.id.frienditem_textview_comment);
            }
        }

        /**
         * 1:1 채팅방 ID를 찾고 없으면 생성한 후 채팅 액티비티로 이동
         */
        void searchOrCreateRoom(final int position, final UserModel destinationUserModel) {
            // 내가 포함된 채팅방을 검색
            FirebaseDatabase.getInstance().getReference().child("rooms").orderByChild("users/"+myUserModel.uid)
                    .equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final Map<String, UserModel> roomUserModelMap = new HashMap<>(); // 1:1 채팅방의 사용자
                    String roomId = null;

                    // 조건에 맞는 검색 결과가 없으면 dataSnapshot.getChildrenCount()가 0 이 됨
                    Log.d(TAG, "searchOrCreateRoom() > onDataChange() dataSnapshot.getChildrenCount()=" + dataSnapshot.getChildrenCount() +
                            ", destinationUid=" + destinationUserModel.uid);

                    // 채팅 액티비티 호출시 인텐트에 저장할 채팅 사용자 데이터
                    myUserModel.comment = null;
                    roomUserModelMap.put(myUserModel.uid, myUserModel);
                    destinationUserModel.comment = null;
                    roomUserModelMap.put(destinationUserModel.uid, destinationUserModel);

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        RoomModel roomModel = item.getValue(RoomModel.class);
                        // 내가 선택한 상대방이 있는 1:1 채팅방
                        if (roomModel.users.containsKey(destinationUserModel.uid) && roomModel.users.size() == 2) {
                            roomId = item.getKey();    // 방 ID
                            Log.i(TAG, "searchOrCreateRoom() > onDataChange() > 검색 roomId=" + roomId);

                            startGroupMessageActivity(position, roomId, roomModel, roomUserModelMap);
                            break;
                        }
                    }

                    if( roomId == null ) {
                        // 채팅방 ID 생성
                        roomId = FirebaseDatabase.getInstance().getReference().child("rooms").push().getKey();
                        // 채팅 메시지 ID 생성
                        String messageId = FirebaseDatabase.getInstance().getReference().child("messages").child(roomId).push().getKey();
                        String initMessage = destinationUserModel.userName + "을 초대합니다.";
                        final RoomModel roomModel = new RoomModel();

                        // 채팅방 데이터
                        roomModel.descTimestamp = Long.MAX_VALUE - System.currentTimeMillis();
                        roomModel.lastMessage = initMessage;
                        roomModel.lastUid = uid;
                        roomModel.users.put(myUserModel.uid, true);
                        roomModel.users.put(destinationUserModel.uid, true);

                        // 메시지 데이터
                        MessageModel messageModel = new MessageModel();
                        messageModel.uid = myUserModel.uid;
                        messageModel.timestamp = System.currentTimeMillis();
                        messageModel.message = initMessage;
                        messageModel.readUsers.put(destinationUserModel.uid, false);

                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put("/rooms/" + roomId, roomModel.toMap());
                        childUpdates.put("/messages/" + roomId + "/" + messageId, messageModel.toMap());

                        final String finalRoomId = roomId;
                        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.i(TAG, "searchOrCreateRoom() > onSuccess() > 생성 chatRoomUid=" + finalRoomId);

                                        startGroupMessageActivity(position, finalRoomId, roomModel, roomUserModelMap);
                                    }
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "searchOrCreateRoom() > onCancelled() " + databaseError.toString());
                }
            });
        }


        void startGroupMessageActivity(int position, String roomId, RoomModel roomModel, Map<String, UserModel> roomUserModelMap) {
            Log.d(TAG, "startGroupMessageActivity() position="+position+", roomId="+roomId+", roomModel="+roomModel.toString());
            Intent intent = new Intent(getView().getContext(), GroupMessageActivity.class);
            intent.putExtra("roomId", roomId);
            intent.putExtra("roomModel", roomModel);
            intent.putExtra("roomUserModelMap", (Serializable) roomUserModelMap);

            ActivityOptions activityOptions = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                activityOptions = ActivityOptions.makeCustomAnimation(getView().getContext(), R.anim.fromright, R.anim.toleft);
                startActivity(intent, activityOptions.toBundle());
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
        peopleFragmentRecyclerViewAdapter.readUsers();
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
