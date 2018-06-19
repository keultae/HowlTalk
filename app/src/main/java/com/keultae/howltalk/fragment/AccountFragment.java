package com.keultae.howltalk.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.keultae.howltalk.LoginActivity;
import com.keultae.howltalk.R;
import com.keultae.howltalk.model.ChatModel;
import com.keultae.howltalk.model.UserModel;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.google.android.gms.flags.impl.SharedPreferencesFactory.getSharedPreferences;

public class AccountFragment extends Fragment {
    private final String TAG = "AccountFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // 로그인 ID 표시
        TextView textView = view.findViewById(R.id.accountFragment_textview_login_id);
        SharedPreferences sp = getActivity().getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        String loginId = sp.getString("login_id", null);
        Log.d("AccountFragment", "loginId=" + loginId);

        textView.setText(loginId);

        Button button = view.findViewById(R.id.accountFragment_button_comment);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showDialog(v.getContext());

//                FirebaseDatabase.getInstance().getReference().child("users").orderByKey().equalTo(FirebaseAuth.getInstance().getUid())
                // users 하위의 키로 정렬한 후 키가 uid인건만 검색(1건)
//                FirebaseDatabase.getInstance().getReference("users").orderByKey().equalTo(FirebaseAuth.getInstance().getUid())
                // userName의 값으로 오름차순 정렬 후 userName이 "갤럭시 A5"를 검색(1건)
//                FirebaseDatabase.getInstance().getReference("users").orderByChild("userName").equalTo("갤럭시 A5")
                // userName의 값으로 오름차순 정렬 후 userName이 "갤럭시"로 시작하는 노드를 검색(2건)
                /*
                FirebaseDatabase.getInstance().getReference("users").orderByChild("userName").startAt("갤럭시")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot node: dataSnapshot.getChildren()) {
                                    Log.d(TAG, "onCreateView() > node.getKey(): " + node.getKey());
                                    UserModel userModel = node.getValue(UserModel.class);
                                    Log.d(TAG, "onCreateView() > userModel: " + userModel.toString());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                */
                // chatrooms/PID/timestamp 값으로 오름차순 정렬
//                FirebaseDatabase.getInstance().getReference("chatrooms").orderByChild("timestamp")
                /*
                chatrooms/PID/users/UID 값으로 오름차순 정렬
                UID가 없는 노드가 가장 처음에 나오고, false(값이 여러개이면 키로 정렬), true(값이 여러개이면 키로 정렬), 숫자값(오름차순), 문자열(사전순, 오름차순), 객체(키에 따라 사전순, 오름차순)
                으로 검색되기 때문에 chatrooms 하위 모든 노트가 검색됨
                 */
//                FirebaseDatabase.getInstance().getReference("chatrooms").orderByChild("users/"+FirebaseAuth.getInstance().getUid())
                /*
                FirebaseDatabase.getInstance().getReference("chatrooms")
                        .orderByChild("users/"+FirebaseAuth.getInstance().getUid())
                        .equalTo(true)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot node: dataSnapshot.getChildren()) {
                                    Log.d(TAG, "onCreateView() > node.getKey(): " + node.getKey());
                                    ChatModel chatModel = node.getValue(ChatModel.class);
                                    Log.d(TAG, "onCreateView() > chatModel: " + chatModel.toString());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                */
            }
        });

        Button logout = view.findViewById(R.id.accountFragment_button_logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutAlertDialog(v.getContext());
            }
        });

        return view;
    }

    private void logoutAlertDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("로그아웃");
        builder.setMessage("로그아웃 하시겠습니까?");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 환경값을 로그아웃으로 설정
                SharedPreferences sp = getActivity().getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.remove("login_id");
                editor.commit();

                // 모든 액티비티를 닫고 로그인 액티비트로 이동
                Intent intent = new Intent(context, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                getActivity().finish();
            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.create();
        builder.show();
    }

    void showDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.dialog_comment, null);
        final EditText editText = (EditText) view.findViewById(R.id.commentDialog_edittext);
        builder.setView(view).setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String,Object> stringObjectMap = new HashMap<>();
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                stringObjectMap.put("comment", editText.getText().toString());
                FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(stringObjectMap);
            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }

}
