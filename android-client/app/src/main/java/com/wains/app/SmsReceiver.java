
package com.wains.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SmsReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                String format = bundle.getString("format");
                
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                        
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();
                        long timestamp = smsMessage.getTimestampMillis();
                        
                        Log.d(TAG, "Received SMS from: " + sender);
                        
                        // Log to Firebase if authenticated
                        logSmsToFirebase(sender, messageBody, timestamp);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing received SMS", e);
        }
    }
    
    private void logSmsToFirebase(String sender, String messageBody, long timestamp) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String uid = auth.getCurrentUser().getUid();
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                
                Map<String, Object> smsData = new HashMap<>();
                smsData.put("address", sender);
                smsData.put("body", messageBody);
                smsData.put("date", timestamp);
                smsData.put("type", "received");
                smsData.put("logged_at", System.currentTimeMillis());
                
                database.getReference("devices")
                    .child(uid)
                    .child("sms")
                    .child("received_" + timestamp)
                    .setValue(smsData);
                    
                Log.d(TAG, "SMS logged to Firebase");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging SMS to Firebase", e);
        }
    }
}
