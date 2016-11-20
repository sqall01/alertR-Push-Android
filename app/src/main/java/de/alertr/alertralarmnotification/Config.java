package de.alertr.alertralarmnotification;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

public class Config {

    private static final String LOGTAG = "Config";
    private static Config config = null;

    private ArrayList<String> channels = new ArrayList<String>();
    private ArrayList<String> channels_subscribed = new ArrayList<String>();
    private byte[] encryption_key = new byte[32];
    private int max_number_received_msgs = 1000;
    private boolean notification_enabled = true;


    public boolean isNotification_enabled() {
        return notification_enabled;
    }


    public void setNotification_enabled(boolean notification_enabled) {
        this.notification_enabled = notification_enabled;
    }


    public byte[] getEncryption_key() {
        return encryption_key;
    }


    public void updateEncryption_key(String secret) {

        // DEBUG
        Log.d(LOGTAG, "updateEncryption_key()");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            encryption_key = digest.digest(secret.getBytes());
        }
        catch(Throwable e) {
            Log.e(LOGTAG, "Could not calculate encryption key.");
            e.printStackTrace();
        }
    }


    public ArrayList<String> getChannels_subscribed() {
        return channels_subscribed;
    }


    public int getMax_number_received_msgs() {
        return max_number_received_msgs;
    }


    public void setMax_number_received_msgs(int value) {
        max_number_received_msgs = value;
    }


    public static Config getInstance() {
        if(config == null) {
            new Config();
        }
        return config;
    }


    private Config() {

        // Parse shared preferences.
        if(MainActivity.main_activity != null) {
            SharedPreferences shared_prefs =
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.main_activity);
            parseConfig(shared_prefs);
        }

        config = this;
    }


    public void parseConfig(SharedPreferences shared_prefs) {

        // DEBUG
        Log.d(LOGTAG, "parseConfig");

        // Parse channels from config.
        String pref_general_channel_key = MainActivity.main_activity.getString(R.string.pref_push_channel_key); // TODO what happens if main activity does not exist?
        String channels_string = shared_prefs.getString(pref_general_channel_key, "");
        ArrayList<String> channels_array =
                new ArrayList<String>(Arrays.asList(channels_string.replace(" ", "").split(",")));
        removeIllegalChannels(channels_array);
        channels_array.add("alertR_notification"); // Add manually the notification channel.
        updateChannels(channels_array);

        // Parse encryption key.
        String pref_general_encryption_key = MainActivity.main_activity.getString(R.string.pref_push_encryption_key); // TODO what happens if main activity does not exist?
        updateEncryption_key(shared_prefs.getString(pref_general_encryption_key, ""));

        // Parse number received messages.
        String pref_general_number_notifications_key = MainActivity.main_activity.getString(R.string.pref_push_number_notifications_key); // TODO what happens if main activity does not exist?
        String pref_general_number_notifications_default = MainActivity.main_activity.getString(R.string.pref_push_number_notifications_default); // TODO what happens if main activity does not exist?
        String number_str = shared_prefs.getString(pref_general_number_notifications_key, pref_general_number_notifications_default);
        max_number_received_msgs = Integer.parseInt(number_str);

        // Parse notification enabled.
        String pref_push_notification_key = MainActivity.main_activity.getString(R.string.pref_push_notification_key); // TODO what happens if main activity does not exist?
        boolean pref_push_notification_default = Boolean.parseBoolean(MainActivity.main_activity.getString(R.string.pref_push_notification_default)); // TODO what happens if main activity does not exist?
        this.notification_enabled = shared_prefs.getBoolean(pref_push_notification_key, pref_push_notification_default);
    }


    private void removeIllegalChannels(ArrayList<String> channels_array) {
        for(String channel : channels_array) {
            if(channel.length() == 0 || channel.length() > 900) {
                channels_array.remove(channel);
            }
            else if(channel.matches("[a-bA-Z0-9-_.~%]")) {
                channels_array.remove(channel);
            }
        }
    }


    public void updateChannels(ArrayList<String> channels_array) {

        channels.clear();
        for(String channel : channels_array) {
            channels.add(channel);
        }

        // Subscribe to new channels.
        for(String channel : channels_array) {

            // Check if the channel is already subscribed to => subscribe if not.
            if(!channels_subscribed.contains(channel)) {

                channels_subscribed.add(channel);
                FirebaseMessaging.getInstance().subscribeToTopic(channel);

                // DEBUG
                Log.d(LOGTAG, "Subscribed to channel: " + channel);
            }
        }

        // Unsubscribe old channels.
        for(String channel : new ArrayList<String>(channels_subscribed)) {

            // Check if the channel is no longer subscribed to => unsubscribe if not.
            if(!channels_array.contains(channel)) {

                channels_subscribed.remove(channel);
                FirebaseMessaging.getInstance().unsubscribeFromTopic(channel);

                // DEBUG
                Log.d(LOGTAG, "Unsubscribed from channel: " + channel);
            }
        }
    }

}
