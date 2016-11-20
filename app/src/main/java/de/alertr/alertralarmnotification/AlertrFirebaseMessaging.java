package de.alertr.alertralarmnotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AlertrFirebaseMessaging extends FirebaseMessagingService {

    private static final String LOGTAG = "AlertrFirebaseMessaging";

    private byte[] encryption_key;

    private static byte[] decrypt(byte[] key, byte[] iv, byte[] enc_str) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(enc_str);
        return decrypted;
    }


    // Constructor is executed each time a message is received.
    public AlertrFirebaseMessaging() {

        // Get encryption key from the settings.
        encryption_key = Config.getInstance().getEncryption_key();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(LOGTAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(LOGTAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            if(data.containsKey("payload")) {

                // Extract channel from message.
                String channel = remoteMessage.getFrom();
                if(channel.substring(0, 8).compareTo("/topics/") == 0) {
                    channel = channel.substring(8);
                }

                // Check if received special global notification message => overwrite decryption key.
                boolean is_global_notification = false;
                if(channel.toLowerCase().equals("alertr_notification")) {
                    is_global_notification = true;

                    // DEBUG
                    Log.d(LOGTAG, "Received global notification message.");

                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        digest.reset();
                        encryption_key = digest.digest("alertr_notification_secret".getBytes());
                    }
                    catch(Throwable e) {
                        Log.e(LOGTAG, "Could not calculate global notification encryption key.");
                        e.printStackTrace();
                    }
                }

                // Decrypt received
                try {

                    // Split iv and encrypted message.
                    byte[] encrypted_msg = Base64.decode(data.get("payload"), Base64.DEFAULT);
                    byte[] iv = new byte[16];
                    byte[] encrypted_bytes = new byte[encrypted_msg.length - 16];
                    for(int i = 0; i < 16; i++) {
                        iv[i] = encrypted_msg[i];
                    }
                    for(int i = 0; i < encrypted_msg.length - 16; i++) {
                        encrypted_bytes[i] = encrypted_msg[i + 16];
                    }

                    // Decrypt received message and remove random bytes from message.
                    byte[] decrypted_bytes = decrypt(encryption_key, iv, encrypted_bytes);
                    byte[] decrypted_msg_bytes = new byte[decrypted_bytes.length - 4];
                    for(int i = 0; i < decrypted_bytes.length - 4; i++) {
                        decrypted_msg_bytes[i] = decrypted_bytes[i + 4];
                    }
                    String decrypted_str = new String(decrypted_msg_bytes);
                    JSONObject payload_obj = new JSONObject(decrypted_str);

                    // Build received message object.
                    long id = NotificationData.getInstance().getNextId();
                    String subject = payload_obj.getString("sbj"); // subject
                    String message = payload_obj.getString("msg"); // message
                    int time_triggered = payload_obj.getInt("tt"); // time_triggered utc
                    int time_sent = payload_obj.getInt("ts"); // time_sent utc
                    int time_received = (int)(System.currentTimeMillis()/1000);
                    boolean is_sensor_alert = payload_obj.getBoolean("is_sa"); // is_sensor_alert
                    int state;
                    if (is_sensor_alert) {
                        state = payload_obj.getInt("st"); // state
                    } else {
                        state = -1;
                    }

                    // Overwrite fields that are never used in a global notification message
                    // (i.e., a global notification message is never a sensor alert).
                    if(is_global_notification) {
                        is_sensor_alert = false;
                        state = -1;
                    }

                    ReceivedMessage new_message = new ReceivedMessage(id,
                            channel,
                            subject,
                            message,
                            time_triggered,
                            time_sent,
                            time_received,
                            is_sensor_alert,
                            state,
                            is_global_notification);

                    // Issue a display notification.
                    if(Config.getInstance().isNotification_enabled()) {
                        long time_in_ms = ((long) time_triggered) * 1000;
                        Date date = new Date(time_in_ms);

                        DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                                                DateFormat.SHORT);
                        String time_string = date_format.format(date);

                        // Distinguish between global notification and sensor alert.
                        if(is_global_notification) {
                            issueMsgNotification((int) id, "Notification: " + subject, "Issued on " + time_string);
                        }
                        else {
                            issueMsgNotification((int) id, subject, "Triggered on " + time_string);
                        }
                    }

                    // If a GUI exists, add it by using the GUI and updating it.
                    if(MainActivity.main_activity != null) {

                        // Only the main thread can change the UI => execute code as main thread.
                        MainActivity.main_activity.runOnUiThread(
                                new AddReceivedMessage(new_message)
                        );
                    }

                    // GUI is not shown => store data directly without updating GUI.
                    else {
                        // TODO do not know how to store the message into the same file as the others
                        NotificationData.getInstance().addReceivedMessage(new_message);
                    }
                }

                // Either received message was illegal or decryption failed.
                catch(org.json.JSONException e) {
                    Log.e(LOGTAG, "Decrypted message illegal.");
                    e.printStackTrace();
                    issueErrorNotification(0,
                            "Message Error",
                            "Decryption failed or received message illegal.");
                }

                // Error thrown when decryption options are wrong.
                catch(Throwable e) {
                    Log.e(LOGTAG, "Received payload illegal.");
                    e.printStackTrace();
                    issueErrorNotification(0,
                            "Message Error",
                            "Received illegal message.");
                }

                // TODO
                // For now, issue a warning notification if the main activity is closed
                // that we can not handle this case at the moment.
                if(MainActivity.main_activity == null) {
                    issueErrorNotification(0,
                            "Application Error",
                            "Messages can only be handled if app is started.");
                }
            }
            else {
                Log.e(LOGTAG, "Received message does not contain payload.");
                issueErrorNotification(0,
                        "Message Error",
                        "Received message does not contain payload.");
            }
        }
        else {
            Log.e(LOGTAG, "Received message does not contain data.");
            issueErrorNotification(0,
                    "Message Error",
                    "Received message does not contain data.");
        }
    }


    private void issueErrorNotification(int id, String title, String text) {

        // Build notification.
        NotificationCompat.Builder notification_builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text);

        // Check if notification is enabled before vibrate and make a sound.
        if(Config.getInstance().isNotification_enabled()) {
            // Set default notification sound.
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notification_builder.setSound(alarmSound);

            // Set vibration.
            notification_builder.setVibrate(new long[]{0, 500, 500, 500});

            // Set LED notification.
            notification_builder.setLights(Color.WHITE, 1000, 2000);
        }

        // Gets an instance of the NotificationManager service
        NotificationManager notification_manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        notification_manager.notify(id, notification_builder.build());

    }


    private void issueMsgNotification(int id, String title, String text) {

        // Build notification.
        NotificationCompat.Builder notification_builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text);

        // Set click action of notification.
        Intent result_intent = new Intent(this, MainActivity.class);
        PendingIntent result_pending_intent =
                PendingIntent.getActivity(
                        this,
                        0,
                        result_intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notification_builder.setContentIntent(result_pending_intent);

        /* TODO problem when activity does not exist then the data is not loaded
        Log for activity with only one element looks like this:
        09-16 11:29:51.290 10009-10044/de.alertr.alertralarmnotification D/AlertrFirebaseMessaging: From: /topics/test
09-16 11:29:51.290 10009-10044/de.alertr.alertralarmnotification D/AlertrFirebaseMessaging: Message data payload: {payload={"time_sent": 1474025391, "time_triggered": 1474025391, "is_sensor_alert": true, "state": 1, "message": "payload message\n\nwith newlines", "subject": "payload subject sensor alert looooooong subject"}}
09-16 11:29:51.291 10009-10044/de.alertr.alertralarmnotification D/NotificationData: loadStoredData()
09-16 11:29:51.301 10009-10044/de.alertr.alertralarmnotification D/NotificationData: addReceivedMessage()
09-16 11:29:51.301 10009-10044/de.alertr.alertralarmnotification D/NotificationData: storeData()

=> does not go through main activity since it does not exist

Why does the data not exist?

        */

        // Set default notification sound.
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification_builder.setSound(alarmSound);

        // Set vibration.
        notification_builder.setVibrate(new long[] { 0, 500, 500, 500 });

        // Set LED notification.
        notification_builder.setLights(Color.WHITE, 1000, 2000);

        // Gets an instance of the NotificationManager service
        NotificationManager notification_manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        notification_manager.notify(id, notification_builder.build());

    }

}
