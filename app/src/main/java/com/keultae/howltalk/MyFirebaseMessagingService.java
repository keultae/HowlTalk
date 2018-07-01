package com.keultae.howltalk;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.keultae.howltalk.chat.GroupMessageActivity;

import java.util.Iterator;
import java.util.List;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived()");

//        dump(getApplication().getBaseContext());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "onMessageReceived() > message data payload: " + remoteMessage.getData());

            String senderName = remoteMessage.getData().get("senderName").toString();
            String message = remoteMessage.getData().get("message").toString();
            String roomId = remoteMessage.getData().get("roomId").toString();

            // 최상위 액티비티가 GroupMessageActivity이면 알림 리스트에 푸시가 왔음을 표시하지 않음.
            ActivityManager activity_manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> task_info = activity_manager.getRunningTasks(9999);

            if(!task_info.get(0).topActivity.getClassName().endsWith("GroupMessageActivity")) {
                sendNotification(senderName, message, roomId);
            } else {
                Log.d(TAG, "onMessageReceived() 최상위 액티비티가 GroupMessageActivity 이므로 알림 메시지를 표시하지 않음.");
            }
        }
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
        Log.d(TAG, "dump()\r\n" + sb.toString());
    }

    private void sendNotification(String title, String text, String roomId) {
        Intent intent = null;

        Log.d(TAG, "sendNotification() roomId=" + roomId + ", title="+title+", text="+text);
        intent = new Intent(this, MainActivity.class);
        intent.putExtra("roomId", roomId);

        /**
         * 호출되는 액티비티가 태스크의 최상단에 있으면 새로운 인스턴스를 생성하지 않습니다.
         */
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        /**
         * 스택에 호출하려는 액티비티의 인스턴스가 있으면 새로운 인스턴스를 생성하는 대신에 기존 액티비티를 포그라운드로 가져옵니다.
         * 스택에서 포그라운드로 가져오려는 액티비티 상위의 액티비티를 모두 삭제합니다.
         */
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        /* intent 설정한 값이 제대로 설정 되었는지 확인하기 위한 코드로 설정한데로 잘됨
        Log.d(TAG, "intent.getFlags()="+intent.getFlags());
        Log.d(TAG, "(intent.getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK)="+ (intent.getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK)) );
        Log.d(TAG, "(intent.getFlags() & (Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)="+ (intent.getFlags() & (Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)) );
        Log.d(TAG, "(intent.getFlags() & (Intent.FLAG_ACTIVITY_SINGLE_TOP)="+ (intent.getFlags() & (Intent.FLAG_ACTIVITY_SINGLE_TOP)) );
        Log.d(TAG, "(intent.getFlags() & (Intent.FLAG_ACTIVITY_CLEAR_TOP)="+ (intent.getFlags() & (Intent.FLAG_ACTIVITY_CLEAR_TOP)) );

        if( (intent.getFlags() & (Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)) ==
                (Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP) ) {
            Log.d(TAG, "OK");
        }
        */

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

//        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
//        PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
//        wakelock.acquire(5000);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
