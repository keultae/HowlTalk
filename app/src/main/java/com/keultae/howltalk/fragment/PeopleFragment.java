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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeopleFragment extends Fragment{
    private final String TAG = "PeopleFragment";

    private List<UserModel> allUserModels = new ArrayList<>();  // 본인을 제외한 모든 사용자
    private List<UserModel> chatRoomUserModels = new ArrayList<>();;  // 1:1 채팅방의 사용자
    private UserModel myUserModel;          // 본인

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);

        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new PeopleFragmentRecyclerViewAdapter());

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
        public PeopleFragmentRecyclerViewAdapter() {
            final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    allUserModels.clear();

                    for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        Log.d(TAG, "PeopleFragmentRecyclerViewAdapter() > userModel=" + userModel.toString());

                        if(userModel.uid.equals(myUid)) {
                            myUserModel = userModel;
                            continue;
                        }
                        allUserModels.add(userModel);
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
                    .load(allUserModels.get(position).profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(customViewHolder.imageView);

            customViewHolder.textView.setText(allUserModels.get(position).userName);

            if(allUserModels.get(position).comment != null) {
                customViewHolder.textView_comment.setText(allUserModels.get(position).comment);
                customViewHolder.textView_comment.setBackgroundResource(R.drawable.rightbubble);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchOrCreateRoom(position, myUserModel, allUserModels.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "getItemCount() > allUserModels.size()=" + allUserModels.size());
            return allUserModels.size();
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

        void startMessageActivity(int position, String chatRoomId, RoomModel roomModel) {
            Log.d(TAG, "startMessageActivity() position="+position+", chatRoomId="+chatRoomId+", roomModel="+roomModel.toString());
//            Intent intent = new Intent(getView().getContext(), MessageActivity.class);
            Intent intent = new Intent(getView().getContext(), GroupMessageActivity.class);
            intent.putExtra("chatRoomId", chatRoomId);
            intent.putExtra("roomModel", roomModel);

            ActivityOptions activityOptions = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                activityOptions = ActivityOptions.makeCustomAnimation(getView().getContext(), R.anim.fromright, R.anim.toleft);
                startActivity(intent, activityOptions.toBundle());
            }
        }

        /**
         * 1:1 채팅방 ID를 찾고 없으면 생성한 후 채팅 액티비티로 이동
         */
        void searchOrCreateRoom(final int position, final UserModel userModel, final UserModel destinationUserModel) {
            Log.d(TAG, "searchOrCreateRoom()");

            // 내가 포함된 채팅방을 검색
            FirebaseDatabase.getInstance().getReference().child("rooms").orderByChild("users/"+userModel.uid+"/uid")
                    .equalTo(userModel.uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String chatRoomId = null;

                    // 조건에 맞는 검색 결과가 없으면 dataSnapshot.getChildrenCount()가 0 이 됨
                    Log.d(TAG, "searchOrCreateRoom() > onDataChange() dataSnapshot.getChildrenCount()=" + dataSnapshot.getChildrenCount() +
                            ", destinationUid=" + destinationUserModel.uid);

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        RoomModel roomModel = item.getValue(RoomModel.class);
                        Log.d(TAG, roomModel.toString());
                        // 내가 선택한 상대방이 있는 1:1 채팅방
                        if (roomModel.users.containsKey(destinationUserModel.uid) && roomModel.users.size() == 2) {
                            chatRoomId = item.getKey();    // 방 ID
                            Log.d(TAG, "searchOrCreateRoom() > onDataChange() > 검색 chatRoomUid=" + chatRoomId);

                            // TODO: users의 최신 데이터로 rooms/./users를 업데이트
                            // 푸시토큰의 값이 바뀔 수 있으므로 업데이트 필요
//                            for(String tmpUid: roomModel.users.keySet()) {
//                                roomModel.users.put(tmpUid, allUserModels.get(tmpUid));
//                            }

                            startMessageActivity(position, chatRoomId, roomModel);
                            break;
                        }
                    }

                    if( chatRoomId == null ) {
                        chatRoomId = FirebaseDatabase.getInstance().getReference().child("rooms").push().getKey();
                        final String chattingId = FirebaseDatabase.getInstance().getReference().child("messages").child(chatRoomId).push().getKey();
                        String initMessage = destinationUserModel.userName + "을 초대합니다.";
                        final RoomModel roomModel = new RoomModel();
                        roomModel.descTimestamp = Long.MAX_VALUE - System.currentTimeMillis();
                        roomModel.lastMessage = initMessage;

                        userModel.comment = null;
                        roomModel.users.put(userModel.uid, userModel);
                        destinationUserModel.comment = null;
                        roomModel.users.put(destinationUserModel.uid, destinationUserModel);

                        MessageModel messageModel = new MessageModel();
                        messageModel.uid = userModel.uid;
                        messageModel.timestamp = System.currentTimeMillis();
                        messageModel.message = initMessage;
                        messageModel.readUsers.put(destinationUserModel.uid, false);

                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put("/rooms/" + chatRoomId, roomModel.toMap());
                        childUpdates.put("/messages/" + chatRoomId + "/" + chattingId, messageModel.toMap());

                        final String finalChatRoomId = chatRoomId;
                        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "searchOrCreateRoom() > onSuccess() > 생성 roomId=" + finalChatRoomId);

                                        startMessageActivity(position, finalChatRoomId, roomModel);
                                    }
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "searchOrCreateRoom() > onCancelled()");
                }
            });
        }
    }
}
