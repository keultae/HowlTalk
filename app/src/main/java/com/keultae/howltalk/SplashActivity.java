package com.keultae.howltalk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class SplashActivity extends AppCompatActivity {
    private final String TAG = "SplashActivity";
    private LinearLayout linearLayout;
    private FirebaseRemoteConfig firebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 안테나 창 보이지 않도록 설정
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        linearLayout = (LinearLayout)findViewById(R.id.splashactivity_linearlayout);

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        firebaseRemoteConfig.setConfigSettings(configSettings);
        firebaseRemoteConfig.setDefaults(R.xml.default_config);

        // cacheExpirationSeconds is set to cacheExpiration here, indicating the next fetch request
        // will use fetch data from the Remote Config service, rather than cached parameter values,
        // if cached parameter values are more than cacheExpiration seconds old.
        // See Best Practices in the README for more information.
        firebaseRemoteConfig.fetch(0)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
//                            Toast.makeText(SplashActivity.this, "Fetch Succeeded", Toast.LENGTH_SHORT).show();

                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.
                            firebaseRemoteConfig.activateFetched();
                        } else {
                            Toast.makeText(SplashActivity.this, "Fetch Failed", Toast.LENGTH_SHORT).show();
                        }
                        displayMessage();
                    }
                });
    }

    void displayMessage() {
        String splash_background = firebaseRemoteConfig.getString("splash_background");
        boolean caps = firebaseRemoteConfig.getBoolean("splash_message_caps");
        String splash_message = firebaseRemoteConfig.getString("splash_message");

//        linearLayout.setBackgroundColor(Color.parseColor(splash_background));

        if(caps) {
            AlertDialog.Builder builer = new AlertDialog.Builder(this);
            builer.setMessage(splash_message).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builer.create().show();
        } else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // res/values/strings.xml 값 가져옴
                    String appName = getResources().getString(R.string.app_name);
                    SharedPreferences sp = getSharedPreferences(appName, Context.MODE_PRIVATE);
                    String uid = sp.getString("uid", null);
                    String loginId = sp.getString("login_id", null);
                    String loginPw = sp.getString("login_pw", null);
                    Log.d(TAG, "uid = " + uid + ", login_id=" + loginId + "login_pw" + loginPw);

                    if(loginId != null && loginPw != null) {
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(loginId, loginPw).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    Log.d(TAG, "로그인 성공");
//                                    SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
//                                    SharedPreferences.Editor editor = sp.edit();
//                                    editor.putString("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
//                                    editor.putString("login_id", loginId.getText().toString());
//                                    editor.putString("login_pw", loginPw.getText().toString());
//                                    editor.commit();nex

//                                    Intent intent = new Intent(  SplashActivity .this, MainActivity.class);
//                                    startActivity(intent);
//                                    finish();
                                    startActivity(new Intent(getBaseContext(), MainActivity.class));
                                    finish();
                                } else {
                                    Log.d(TAG, "로그인 실패");
                                    // 로그인 실패한 부분
                                    Toast.makeText(SplashActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(getBaseContext(), LoginActivity.class));
                                    finish();
                                }
                            }
                        });
                    } else {
                        startActivity(new Intent(getBaseContext(), LoginActivity.class));
                        finish();
                    }


                    // 로그 성공시 메인 페이지로 이동
//                    if(loginId != null) {
//                        startActivity(new Intent(getBaseContext(), MainActivity.class));
//                    } else {
//                        startActivity(new Intent(getBaseContext(), LoginActivity.class));
//                    }
//                    finish();
                }
            }, 1000);
        }
    }
}