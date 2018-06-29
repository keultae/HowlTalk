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
import com.keultae.howltalk.chat.MessageActivity;
import com.keultae.howltalk.model.MessageModel;
import com.keultae.howltalk.model.RoomModel;
import com.keultae.howltalk.model.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeopleFragment extends Fragment{
    private final String TAG = "PeopleFragment";

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
                startActivity(new Intent(v.getContext(), SelectFriendActivity.class));
            }
        });
        return view;
    }

    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<UserModel> userModels;
        UserModel myUserModel;    // 본인

        public PeopleFragmentRecyclerViewAdapter() {
            userModels = new ArrayList<>();
            final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userModels.clear();

                    for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        Log.d(TAG, "PeopleFragmentRecyclerViewAdapter() > userModel=" + userModel.toString());

                        if(userModel.uid.equals(myUid)) {
                            myUserModel = userModel;
                            continue;
                        }
                        userModels.add(userModel);
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
            Glide.with
                    (holder.itemView.getContext())
                    .load(userModels.get(position).profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(customViewHolder.imageView);

            customViewHolder.textView.setText(userModels.get(position).userName);

            if(userModels.get(position).comment != null) {
                customViewHolder.textView_comment.setText(userModels.get(position).comment);
                customViewHolder.textView_comment.setBackgroundResource(R.drawable.rightbubble);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent intent = new Intent(getView().getContext(), MessageActivity.class);
//                    intent.putExtra("destinationUid", userModels.get(position).uid);
//                    intent.putExtra("destinationUserName", userModels.get(position).userName);
//
//                    ActivityOptions activityOptions = null;
//                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
//                        activityOptions = ActivityOptions.makeCustomAnimation(getView().getContext(), R.anim.fromright, R.anim.toleft);
//                        startActivity(intent, activityOptions.toBundle());
//                    }
                    startChattingActivity(myUserModel, userModels.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "getItemCount() > userModels.size()=" + userModels.size());
            return userModels.size();
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
        void startChattingActivity(final UserModel userModel, final UserModel destinationUserModel) {
            Log.d(TAG, "startChattingActivity()");

            // 내가 포함된 채팅방을 검색
            FirebaseDatabase.getInstance().getReference().child("rooms").orderByChild("users/"+userModel.uid+"/uid")
                    .equalTo(userModel.uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String chatRoomUid = null;

                    // 조건에 맞는 검색 결과가 없으면 dataSnapshot.getChildrenCount()가 0 이 됨
                    Log.d(TAG, "startChattingActivity() > onDataChange() dataSnapshot.getChildrenCount()=" + dataSnapshot.getChildrenCount() +
                            ", destinationUid=" + destinationUserModel.uid);

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        RoomModel roomModel = item.getValue(RoomModel.class);
                        Log.d(TAG, roomModel.toString());
                        // 내가 선택한 상대방이 있는 1:1 채팅방
                        if (roomModel.users.containsKey(destinationUserModel.uid) && roomModel.users.size() == 2) {
                            chatRoomUid = item.getKey();    // 방 ID
                            Log.d(TAG, "startChattingActivity() > onDataChange() > 검색 chatRoomUid=" + chatRoomUid);
                            break;
                        }
                    }

                    if( chatRoomUid == null ) {
                        final String roomId = FirebaseDatabase.getInstance().getReference().child("rooms").push().getKey();
                        final String chattingId = FirebaseDatabase.getInstance().getReference().child("messages").child(roomId).push().getKey();
                        String initMessage = destinationUserModel.userName + "을 초대합니다.";
                        RoomModel roomModel = new RoomModel();
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
                        childUpdates.put("/rooms/" + roomId, roomModel.toMap());
                        childUpdates.put("/messages/" + roomId + "/" + chattingId, messageModel.toMap());

                        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "startChattingActivity() > onSuccess() > 생성 roomId=" + roomId);
                                    }
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "startChattingActivity() > onCancelled()");
                }
            });
        }
    }
}
