package com.example.vinylvolumecontrol

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import timber.log.Timber


class ForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        //System.out.println("Starting service ...");
        Timber.d("Hello!")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        val httpServer = HttpServer(audioManager, getApplicationContext())
        httpServer.start()
        val channelId: String = "VinylVolumeControl"
        createNotificationChannel(channelId)
        val notificationManager = NotificationManagerCompat.from(getApplicationContext())
        val notification: Notification
        val notificationIntent = Intent(getApplicationContext(), MainActivity::class.java)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent =
            PendingIntent.getActivity(getApplicationContext(), 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        notification = NotificationCompat.Builder(this, channelId)
            .setOngoing(true)
            .setContentTitle("getString(R.string.ongoing_notification_title)")
            .setContentText("getString(R.string.ongoing_notification_text)")
            .setSmallIcon(R.drawable.btn_dialog)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.btn_dialog))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
//        ContextCompat.startForegroundService()
        startForeground(42, notification)
        Timber.d("got to end of service start!")
        //System.out.println("Service started.");
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "myName",//getString(R.string.running_indicator),
                NotificationManager.IMPORTANCE_MIN
            )
            val notificationManager: NotificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //System.out.println("Stopping Service...");
        HttpServer.stopServer()
        //System.out.println("Service stopped");
    }
}
