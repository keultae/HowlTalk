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
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String title = remoteMessage.getData().get("title").toString();
            String text = remoteMessage.getData().get("text").toString();
            String destinationUid = null;
            String destinationRoom = null;

            if(remoteMessage.getData().get("destinationUid") == null) {
                Log.d(TAG, "destinationUid is null");
            } else {
                destinationUid = remoteMessage.getData().get("destinationUid").toString();
            }

            if(remoteMessage.getData().get("destinationRoom") == null) {
                Log.d(TAG, "destinationRoom is null");
            } else {
                destinationRoom = remoteMessage.getData().get("destinationRoom").toString();
            }
            sendNotification(title, text, destinationUid, destinationRoom);
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void sendNotification(String title, String text, String destinationUid, String destinationRoom) {
        Intent intent = null;

        if(destinationRoom != null) {
            // 단체 채팅 방
            intent = new Intent(this, GroupMessageActivity.class);
            intent.putExtra("destinationRoom", destinationRoom);
            Log.d(TAG, "sendNotification() destinationRoom: " + destinationRoom);
        } else {
            // 개인 채팅 방
            intent = new Intent(this, MessageActivity.class);
            intent.putExtra("destinationUid", destinationUid);
            Log.d(TAG, "sendNotification() destinationUid: " + destinationUid);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
