package com.keultae.howltalk;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.keultae.howltalk.model.UserModel;

import static android.app.Activity.RESULT_OK;

public class SignupActivity extends AppCompatActivity {
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static final int PICK_FROM_ALBUM = 10;
    private EditText email;
    private EditText name;
    private EditText password;
    private Button signup;
    private String splash_background;
    private ImageView profile;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        String splash_background = mFirebaseRemoteConfig.getString(getString(R.string.rc_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(splash_background));
        }

        profile = (ImageView)findViewById(R.id.signupActivity_imageview_profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM);
            }
        });

        email = (EditText)findViewById(R.id.signupActivity_edittext_email);
        name = (EditText)findViewById(R.id.signupActivity_edittext_name);
        password = (EditText)findViewById(R.id.signupActivity_edittext_password);
        signup = (Button)findViewById(R.id.signupActivity_edittext_signup);
        signup.setBackgroundColor(Color.parseColor(splash_background));

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (email.getText().toString() == null || name.getText().toString() == null || password.getText().toString() == null) {
                    return;
                }

                if (imageUri == null) {
                    Toast.makeText(SignupActivity.this, "프로필 사진을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                final String uid = task.getResult().getUser().getUid();
                                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name.getText().toString()).build();

                                task.getResult().getUser().updateProfile(userProfileChangeRequest);

                                FirebaseStorage.getInstance().getReference().child("userImages").child(uid).putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                                        @SuppressWarnings("VisibleForTests")
//                                        String imageUrl = task.getResult().getDownloadUrl().toString();
//                                        String imageUrl = task.getResult().getStorage().getDownloadUrl().toString();

                                        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                                        StorageReference dateRef = storageRef.child("userImages").child(uid);
                                        String link = dateRef.getDownloadUrl().toString();
                                        dateRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                Log.d("SignupActivity", "uri.toString(): " + uri.toString());

                                                UserModel userModel = new UserModel();
                                                userModel.userName = name.getText().toString();
                                                userModel.profileImageUrl = uri.toString();
                                                userModel.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                                FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        SignupActivity.this.finish();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_FROM_ALBUM && resultCode == RESULT_OK) {
            profile.setImageURI(data.getData());    // 가운데 뷰를 바꿈
            imageUri = data.getData(); // 이미지 경로 원본
        }
    }
}
