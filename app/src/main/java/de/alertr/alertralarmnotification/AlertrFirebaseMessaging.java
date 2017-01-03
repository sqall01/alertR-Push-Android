package de.alertr.alertralarmnotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AlertrFirebaseMessaging extends FirebaseMessagingService {

    private static final String LOGTAG = "AlertrFirebaseMessaging";
    public static AlertrFirebaseMessaging alertr_firebase_messaging = null;

    private static byte[] decrypt(byte[] key, byte[] iv, byte[] enc_str) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(enc_str);
        return decrypted;
    }

    // Decrypts an received encrypted message and returns an decrypted message.
    public static ReceivedMessage decrypt_encrypted_msg(ReceivedEncryptedMessage new_enc_msg,
                                                        byte[] encryption_key)
                                                        throws Exception {

        ReceivedMessage new_message = null;

        // Decrypt received
        try {
            // Split iv and encrypted message.
            byte[] encrypted_msg = Base64.decode(new_enc_msg.getPayload(), Base64.DEFAULT);
            byte[] iv = new byte[16];
            byte[] encrypted_bytes = new byte[encrypted_msg.length - 16];
            for (int i = 0; i < 16; i++) {
                iv[i] = encrypted_msg[i];
            }
            for (int i = 0; i < encrypted_msg.length - 16; i++) {
                encrypted_bytes[i] = encrypted_msg[i + 16];
            }

            // Decrypt received message and remove random bytes from message.
            byte[] decrypted_bytes = decrypt(encryption_key, iv, encrypted_bytes);
            byte[] decrypted_msg_bytes = new byte[decrypted_bytes.length - 4];
            for (int i = 0; i < decrypted_bytes.length - 4; i++) {
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
            int time_received = new_enc_msg.getTime_received();
            boolean is_sensor_alert = payload_obj.getBoolean("is_sa"); // is_sensor_alert
            int state;
            if (is_sensor_alert) {
                state = payload_obj.getInt("st"); // state
            } else {
                state = -1;
            }

            // Overwrite fields that are never used in a global notification message
            // (i.e., a global notification message is never a sensor alert).
            if (new_enc_msg.is_global_notification()) {
                is_sensor_alert = false;
                state = -1;
            }

            new_message = new ReceivedMessage(id,
                    new_enc_msg.getChannel(),
                    subject,
                    message,
                    time_triggered,
                    time_sent,
                    time_received,
                    is_sensor_alert,
                    state,
                    new_enc_msg.is_global_notification(),
                    false);
        }

        // Error thrown when decryption options are wrong.
        catch(Throwable e) {
            throw e;
        }

        return new_message;
    }


    public static void issueErrorNotification(Context context, int id, String title, String text) {

        // Build notification.
        NotificationCompat.Builder notification_builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text);

        // Set default notification sound.
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification_builder.setSound(alarmSound);

        // Set vibration.
        notification_builder.setVibrate(new long[]{0, 500, 500, 500});

        // Set LED notification.
        notification_builder.setLights(Color.WHITE, 1000, 2000);

        // Gets an instance of the NotificationManager service
        NotificationManager notification_manager =
                (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        notification_manager.notify(id, notification_builder.build());

    }


    public static void issueMsgNotification(Context context, int id, String title, String text) {

        // Build notification.
        NotificationCompat.Builder notification_builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text);


        // Set click action of notification.
        Intent result_intent = new Intent(context, MainActivity.class);
        PendingIntent result_pending_intent =
                PendingIntent.getActivity(
                        context,
                        0,
                        result_intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notification_builder.setContentIntent(result_pending_intent);

        // Set default notification sound.
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification_builder.setSound(alarmSound);

        // Set vibration.
        notification_builder.setVibrate(new long[] { 0, 500, 500, 500 });

        // Set LED notification.
        notification_builder.setLights(Color.WHITE, 1000, 2000);

        // Gets an instance of the NotificationManager service
        NotificationManager notification_manager =
                (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        notification_manager.notify(id, notification_builder.build());

    }


    // Constructor is executed each time a message is received.
    public AlertrFirebaseMessaging() {
        alertr_firebase_messaging = this;
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

                byte[] encryption_key = Config.getInstance().getEncryption_key();

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

                // Create received encrypted message object.
                ReceivedEncryptedMessage new_enc_msg = new ReceivedEncryptedMessage(channel,
                        is_global_notification,
                        (int)(System.currentTimeMillis() / 1000),
                        data.get("payload"));

                // If a application is running, add it by using the GUI and updating it.
                if(MainActivity.main_activity != null) {

                    // Decrypt received
                    try {
                        ReceivedMessage new_message = decrypt_encrypted_msg(new_enc_msg, encryption_key);

                        // Issue a display notification.
                        long time_in_ms = ((long) new_message.getTimeTriggered()) * 1000;
                        Date date = new Date(time_in_ms);

                        DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                DateFormat.SHORT,
                                Locale.getDefault());
                        String time_string = date_format.format(date);

                        // Distinguish between global notification and sensor alert.
                        if(is_global_notification) {
                            issueMsgNotification(this,
                                    0,
                                    "Notification: " + new_message.getSubject(),
                                    "Issued on " + time_string);
                        }
                        else {
                            issueMsgNotification(this,
                                    0,
                                    new_message.getSubject(),
                                    "Triggered on " + time_string);
                        }

                        // Only the main thread can change the UI => execute code as main thread.
                        MainActivity.main_activity.runOnUiThread(
                                new AddReceivedMessage(new_message)
                        );

                    }

                    // Either received message was illegal or decryption failed.
                    catch(org.json.JSONException e) {
                        Log.e(LOGTAG, "Decrypted message illegal.");
                        e.printStackTrace();
                        issueErrorNotification(this,
                                0,
                                "Message Error",
                                "Decryption failed or received message illegal.");
                        return;
                    }

                    // Error thrown when decryption options are wrong.
                    catch(Throwable e) {
                        Log.e(LOGTAG, "Received payload illegal.");
                        e.printStackTrace();
                        issueErrorNotification(this,
                                0,
                                "Message Error",
                                "Received illegal message.");
                        return;
                    }
                }

                // Application is not running => store message encrypted.
                else {
                    ArrayList<ReceivedEncryptedMessage> received_enc_messages = null;
                    File file_obj = getBaseContext().getFileStreamPath("encrypted_msgs.dat");
                    // Load existing received encrypted messages.
                    try {
                        if(file_obj.exists()) {
                            // Read data from file.
                            FileInputStream fis = this.openFileInput(file_obj.getName());
                            StringBuilder file_content = new StringBuilder("");
                            byte[] buffer = new byte[1024];
                            int n;
                            while ((n = fis.read(buffer)) != -1) {
                                file_content.append(new String(buffer, 0, n));
                            }
                            fis.close();

                            // Deserialize data to received messages objects.
                            Gson gson = new Gson();
                            Type collection_type = new TypeToken<ArrayList<ReceivedEncryptedMessage>>() {
                            }.getType();
                            received_enc_messages = gson.fromJson(file_content.toString(), collection_type);
                        }
                    }
                    catch(Throwable e) {
                    }
                    // Create object, if no received encrypted message exists.
                    if(received_enc_messages == null) {
                        received_enc_messages = new ArrayList<ReceivedEncryptedMessage>();
                    }

                    received_enc_messages.add(new_enc_msg);

                    // Store encrypted received messages.
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(received_enc_messages);
                        FileOutputStream fos = this.openFileOutput(file_obj.getName(), Context.MODE_PRIVATE);
                        fos.write(json.getBytes());
                        fos.close();
                    }
                    catch(Throwable e) {
                        Log.e(LOGTAG, "Storing encrypted messages failed.");
                        e.printStackTrace();
                        return;
                    }

                    long time_in_ms = ((long)new_enc_msg.getTime_received()) * 1000;
                    Date date = new Date(time_in_ms);

                    DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                            DateFormat.SHORT,
                            Locale.getDefault());
                    String time_string = date_format.format(date);

                    int number_recv_messages = received_enc_messages.size();
                    issueMsgNotification(this,
                            0,
                            "Received " + number_recv_messages + " new message(s).",
                            "Last received on " + time_string);
                }
            }
            else {
                Log.e(LOGTAG, "Received message does not contain payload.");
                issueErrorNotification(this,
                        0,
                        "Message Error",
                        "Received message does not contain payload.");
                return;
            }
        }
        else {
            Log.e(LOGTAG, "Received message does not contain data.");
            issueErrorNotification(this,
                    0,
                    "Message Error",
                    "Received message does not contain data.");
            return;
        }
    }
}
