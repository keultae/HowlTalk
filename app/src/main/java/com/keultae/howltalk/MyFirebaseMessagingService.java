package com.keultae.howltalk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.keultae.howltalk.chat.GroupMessageActivity;
import com.keultae.howltalk.chat.MessageActivity;
import com.keultae.howltalk.model.NotificationModel;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived()");

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "message data payload: " + remoteMessage.getData());
            String senderName = remoteMessage.getData().get("senderName").toString();
            String message = remoteMessage.getData().get("message").toString();
            String destinationUid = null;
            String chatRoomId = null;

            if(remoteMessage.getData().get("destinationUid") != null) {
                destinationUid = remoteMessage.getData().get("destinationUid").toString();
            }

            if(remoteMessage.getData().get("chatRoomId") != null) {
                chatRoomId = remoteMessage.getData().get("chatRoomId").toString();
            }

            sendNotification(senderName, message, destinationUid, chatRoomId);
        }
    }

    private void sendNotification(String title, String text, String destinationUid, String chatRoomId) {
        Intent intent = null;

        if( destinationUid != null ) {
            // 1:1 채팅 방
            Log.d(TAG, "sendNotification() destinationUid: " + destinationUid);
//            intent = new Intent(this, MessageActivity.class);
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("destinationUid", destinationUid);
        } else {
            // 단체 채팅 방
            Log.d(TAG, "sendNotification() chatRoomId: " + chatRoomId);
            intent = new Intent(this, GroupMessageActivity.class);
            intent.putExtra("chatRoomId", chatRoomId);
        }

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
