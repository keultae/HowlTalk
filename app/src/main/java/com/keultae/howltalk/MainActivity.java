package com.keultae.howltalk;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.keultae.howltalk.chat.MessageActivity;
import com.keultae.howltalk.fragment.AccountFragment;
import com.keultae.howltalk.fragment.ChatFragment;
import com.keultae.howltalk.fragment.PeopleFragment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    String destinationUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView = (BottomNavigationView)findViewById(R.id.mainactivity_bottomnavigationview);

        // 초기화면을 PeopleFragment로 지정
        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new PeopleFragment()).commit();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_people:
                        dump(MainActivity.this);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new PeopleFragment()).commit();
                        return true;
                    case R.id.action_chat:
                        dump(MainActivity.this);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new ChatFragment()).commit();
                        return true;
                    case R.id.action_account:
                        dump(MainActivity.this);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new AccountFragment()).commit();
                        return true;

                }
                return false;
            }
        });

        updatePushTokenToServer();

        destinationUid = getIntent().getStringExtra("destinationUid");
        Log.d(TAG, "onCreate() > destinationUid=" + destinationUid);
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
//                        + ", " + sb.toString(), Toast.LENGTH_LONG).show();
    }

    void updatePushTokenToServer() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String token = FirebaseInstanceId.getInstance().getToken();

        Map<String, Object> map = new HashMap<>();
        map.put("pushToken", token);
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * 앱이 종료된 상태에서 푸시 메시지를 받은 후 "최근 실행 목록”에서 앱을 다시 시작하면 푸시 메시지를 받았을때 생성한 인텐트가 그대로 전달이 돼서
         * MainActivity가 표시되지 않고 "최근 실행 목록"에서 앱을 시작하면 항상 MessageActivity가 표시되는 문제가 있다.
         *
         * 푸시 메시지를 받아서 앱이 실행될때는 MessageActivity를 표시하고 "최근 실행 목록"에서 앱이 실행될떄는
         * MainActivity가 표시되도록 푸시 메시지를 받았을떄 생성한 Intent Extra 값을 무시하도록 처리해서 원하는 결과를 얻을 수 있었다.
         */
        Log.d(TAG, "onResume() > destinationUid=" + destinationUid);
        Log.d(TAG, "getIntent().getFlags()="+getIntent().getFlags());
        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK)) );
        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)) );
        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_SINGLE_TOP)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_SINGLE_TOP)) );
        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_CLEAR_TOP)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_CLEAR_TOP)) );

        int checkFlags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY;
        if( (getIntent().getFlags() & checkFlags) == checkFlags ) {
//            Toast.makeText(MainActivity.this, "최근 실행 목록에서 실행", Toast.LENGTH_LONG).show();
            getIntent().removeExtra("destinationUid");
            destinationUid = null;
        }

        if( destinationUid != null) {
            Intent i = new Intent(MainActivity.this, MessageActivity.class);
            i.putExtra("destinationUid", destinationUid);
            startActivity(i);

            getIntent().removeExtra("destinationUid");
            destinationUid = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        destinationUid = intent.getStringExtra("destinationUid");
        Log.d(TAG, "onNewIntent() > destinationUid=" + destinationUid);
    }
}
