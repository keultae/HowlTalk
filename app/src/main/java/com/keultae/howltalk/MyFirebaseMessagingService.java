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
            intent = new Intent(this, MessageActivity.class);
            intent.putExtra("destinationUid", destinationUid);
        } else {
            // 단체 채팅 방
            Log.d(TAG, "sendNotification() chatRoomId: " + chatRoomId);
            intent = new Intent(this, GroupMessageActivity.class);
            intent.putExtra("chatRoomId", chatRoomId);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
