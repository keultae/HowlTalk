package com.keultae.howltalk;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
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
import com.keultae.howltalk.chat.GroupMessageActivity;
import com.keultae.howltalk.fragment.AccountFragment;
import com.keultae.howltalk.fragment.ChatFragment;
import com.keultae.howltalk.fragment.PeopleFragment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    String roomId;

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
//                        dump(MainActivity.this);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new PeopleFragment()).commit();
                        return true;
                    case R.id.action_chat:
//                        dump(MainActivity.this);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new ChatFragment()).commit();
                        return true;
                    case R.id.action_account:
//                        dump(MainActivity.this);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new AccountFragment()).commit();
                        return true;

                }
                return false;
            }
        });

        updatePushTokenToServer();

        // 푸시에서 보낸 인텐트
        roomId = getIntent().getStringExtra("roomId");
        Log.d(TAG, "onCreate() roomId="+roomId);
    }

    /**
     * 앱의 백 스택 정보와 액티비티 정보를 가져옴
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
    }

    void updatePushTokenToServer() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String token = FirebaseInstanceId.getInstance().getToken();

        Map<String, Object> map = new HashMap<>();
        map.put("pushToken", token);
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * 앱이 종료된 상태에서 푸시 메시지를 수산해서 앱을 실행한 후 "최근 실행 목록”에서 앱을 다시 시작하면
         * 푸시 메시지를 수신했을때 인텐트에 "최근 실행 목록”에서 앱을 실행 했다는 플래그가 전달이 돼서
         * MainActivity가 표시되지 않고 GroupMessageActivity 표시되는 문제가 발생한다.
         *
         * "최근 실행 목록”에서 앱을 시작하면 앱이 실행 됐을때 인텐트를 보관했다가 그대로 다시 전달하는것 같다.
         *
         *   "최근 실행 목록”에서 앱을 실행하는 것은 바탕화면에서 앱 아이콘을 클릭하여 앱을 실행하는 것과 동일하므로
         * "최근 실행 목록”에서 앱을 실행했는지 플래그를 체크하여 맞다면 인텐트와 플래그를 초기화해야 한다.
         * 이 상태에서 푸시 메시지를 수신하면 푸시에서 전송한 인텐트는 사용할 수 있지만, 플래그는 초기화 되었기 때문에 원하는 액티비티를 띄울 수 있다.
         */
        Log.d(TAG, "onResume() > roomId=" + roomId);
//        Log.d(TAG, "getIntent().getFlags()="+getIntent().getFlags());
//        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK)) );
//        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)) );
//        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_SINGLE_TOP)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_SINGLE_TOP)) );
//        Log.d(TAG, "(getIntent().getFlags() & (Intent.FLAG_ACTIVITY_CLEAR_TOP)="+ (getIntent().getFlags() & (Intent.FLAG_ACTIVITY_CLEAR_TOP)) );

        // "최근 실행 목록”에서 앱을 실행헀는지 검사
        int checkFlags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY;
        if( (getIntent().getFlags() & checkFlags) == checkFlags ) {
            Toast.makeText(MainActivity.this, "최근 실행 목록에서 실행 roomId=" + roomId, Toast.LENGTH_LONG).show();

            // "최근 실행 목록”에서 앱을 실행헀다는 플래그 값을 초기화
            getIntent().setFlags(0);

            // "최근 실행 목록”에서 앱을 실행할때 푸시 메시지 화면으로
            getIntent().removeExtra("roomId");
            roomId = null;
        }

        // 푸시 메시지를 수신
        if(roomId != null) {
            Intent i = new Intent(MainActivity.this, GroupMessageActivity.class);
            i.putExtra("roomId", roomId);
            startActivity(i);

            getIntent().removeExtra("roomId");
            roomId = null;
        }
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // 푸시에서 보낸 인텐트
        roomId = intent.getStringExtra("roomId");
        Log.d(TAG, "onNewIntent() roomId=" + roomId);
    }
}
