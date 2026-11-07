package com.dailyroutine.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.dailyroutine.MainActivity;
import com.example.dailyactivities.R;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "daily_routine_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);
        
        // যদি অ্যালার্ম এবং পুশ নোটিফিকেশন দুইটাই বন্ধ থাকে, তবে কিছুই করার দরকার নেই
        if (!prefs.isTaskAlarmEnabled() && !prefs.isPushNotifEnabled()) {
            return;
        }

        String taskName = intent.getStringExtra("task_name");
        
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alarm for daily tasks");
            channel.enableLights(true);
            channel.enableVibration(true);
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
            channel.setSound(alarmSound, audioAttributes);
            
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("কাজের সময় হয়েছে!")
            .setContentText("এখন শুরু করুন: " + taskName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi);

        // শুধুমাত্র 'কাজের অ্যালার্ম' অন থাকলে শব্দ এবং ভাইব্রেশন হবে
        if (prefs.isTaskAlarmEnabled()) {
            builder.setSound(alarmSound)
                   .setVibrate(new long[]{0, 1000, 500, 1000});
        } else {
            // যদি পুশ বিজ্ঞপ্তি অন থাকে কিন্তু অ্যালার্ম অফ থাকে, তবে সাইলেন্ট নোটিফিকেশন যাবে
            builder.setSound(null);
            builder.setVibrate(null);
        }

        // যদি পুশ বিজ্ঞপ্তি অন থাকে তবেই নোটিফিকেশন শো করবে
        // তবে যদি অ্যালার্ম অন থাকে, তাহলেও শো করবে (সিস্টেমের প্রয়োজনে)
        if (prefs.isPushNotifEnabled() || prefs.isTaskAlarmEnabled()) {
            if (manager != null) {
                manager.notify((int) System.currentTimeMillis(), builder.build());
            }
        }
    }
}