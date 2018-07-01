package com.keultae.howltalk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class LoginActivity extends AppCompatActivity {
    private final String TAG = "LoginActivity";
    private EditText id;
    private EditText password;

    private Button login;
    private Button signup;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_login);
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        String splash_background = firebaseRemoteConfig.getString(getString(R.string.rc_color));
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signOut();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().setStatusBarColor(Color.parseColor(splash_background));
//        }
        id = findViewById(R.id.loginActivity_edittext_id);
        password = findViewById(R.id.loginActivity_edittext_password);


        login = (Button)findViewById(R.id.loginActivity_button_login);
        signup = (Button)findViewById(R.id.loginActivity_button_signup);

//        login.setBackgroundColor(Color.parseColor(splash_background));
//        signup.setBackgroundColor(Color.parseColor(splash_background));

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginEvent();
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                Log.d(TAG, "onAuthStateChanged() > user=" + user.toString());

                /*
                // 로그인
                if(user != null) {
                    SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("login_id", id.getText().toString());
                    Log.d(TAG, "onAuthStateChanged() > login_id=" + id.getText().toString());
                    editor.commit();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else { // 로그아웃
                    Log.d(TAG, "onAuthStateChanged() > 로그아웃");
                }
                */
            }
        };
    }

    void loginEvent() {
        Log.d(TAG, "loginEvent()");
        String loginId = id.getText().toString().trim();
        String loginPassword = password.getText().toString().trim();

        if(loginId.length() == 0) {
            Toast toast = Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if(loginPassword.length() == 0) {
            Toast toast = Toast.makeText(this, "패스워드를 입력하세요.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(loginId, loginPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    Log.d(TAG, "로그인 성공");
                    SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    editor.putString("login_id", id.getText().toString());
                    editor.putString("login_pw", password.getText().toString());
                    editor.commit();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d(TAG, "로그인 실패");
                    // 로그인 실패한 부분
                    Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");

//        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");

        firebaseAuth.removeAuthStateListener(authStateListener);
    }
}
