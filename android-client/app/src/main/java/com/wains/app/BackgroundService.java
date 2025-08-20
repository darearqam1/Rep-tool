
package com.wains.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "LearnerToolChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference deviceRef;
    private Timer heartbeatTimer;
    private String deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Background service created");
        
        deviceId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        
        createNotificationChannel();
        initializeFirebase();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Background service started");
        
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        startHeartbeat();
        
        // Return START_STICKY to restart service if killed
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Background service destroyed");
        
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
        
        if (deviceRef != null) {
            Map<String, Object> offlineStatus = new HashMap<>();
            offlineStatus.put("status", "offline");
            offlineStatus.put("last_seen", System.currentTimeMillis());
            deviceRef.updateChildren(offlineStatus);
        }
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance();
            
            // Authenticate anonymously
            mAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Background service authenticated");
                    deviceRef = mDatabase.getReference("devices").child(deviceId);
                } else {
                    Log.e(TAG, "Background service authentication failed");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }
    }

    private void startHeartbeat() {
        heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendHeartbeat();
            }
        }, 0, 60000); // Send heartbeat every minute
    }

    private void sendHeartbeat() {
        if (deviceRef != null) {
            Map<String, Object> heartbeat = new HashMap<>();
            heartbeat.put("status", "online");
            heartbeat.put("last_seen", System.currentTimeMillis());
            heartbeat.put("service_type", "background");
            
            deviceRef.updateChildren(heartbeat);
            Log.d(TAG, "Heartbeat sent");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Learner Tool Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background monitoring service");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Notification.Builder builder;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        return builder
            .setContentTitle("Learner Tool")
            .setContentText("Background monitoring active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build();
    }
}
