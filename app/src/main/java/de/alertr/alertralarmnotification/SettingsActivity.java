package de.alertr.alertralarmnotification;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;


public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    private static final String LOGTAG = "SettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // DEBUG
        Log.d(LOGTAG, "Settings Activity onCreate()");

        // Register this activity object to settings fragment.
        SettingsActivityFragment settingsActivity = new SettingsActivityFragment();
        settingsActivity.setParentActivity(this);

        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsActivity).commit();
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {

        // DEBUG
        Log.d(LOGTAG, "Settings Activity onPreferenceChange()");

        // If channels have changed => subscribe and unsubscribe channels.
        if(preference.getKey() == getString(R.string.pref_push_channel_key)) {

            String channels_string = (String) value;
            ArrayList<String> channels_array =
                    new ArrayList<String>(
                            Arrays.asList(channels_string.replace(" ", "").split(",")));

            // NOTE: config object takes care of notification channel, unsubscribing to old channels
            // and so on.
            Config.getInstance().updateChannels(channels_array);
        }

        // If username has changed => set username, subscribe and unsubscribe channels.
        else if(preference.getKey() == getString(R.string.pref_push_username_key)) {
            String username = (String) value;
            Config.getInstance().setUsername(username);
        }

        // If encryption key has changed => transfer key to firebase messaging service.
        else if(preference.getKey() == getString(R.string.pref_push_encryption_key)) {

            String temp = (String) value;
            Config.getInstance().updateEncryption_key(temp);

        }

        // If number of stored notifications has changed => updated stored notifications and GUI.
        else if(preference.getKey() == getString(R.string.pref_push_number_notifications_key)) {

            String temp = (String) value;
            Config.getInstance().setMax_number_received_msgs(Integer.parseInt(temp));
            if(MainActivity.main_activity != null) {
                MainActivity.main_activity.updateMaxNumberReceivedMsgs();
            }

        }

        return true;
    }

    @Override
    public void onBackPressed() {

        // DEBUG
        Log.d(LOGTAG, "Settings Activity onBackPressed()");

        // Check if the app is already configured, otherwise remain in preference screen.
        Config config = Config.getInstance();
        if(!config.isConfigured()) {

            // Display a short message to configure the application.
            CharSequence text = getString(R.string.pref_not_complete);
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        }

        else {
            // Re-subscribe to all channels.
            config.updateChannels();

            finish();
        }
    }

    public static class SettingsActivityFragment extends PreferenceFragment {

        private static final String LOGTAG = "SettingsActivityFr";
        private SettingsActivity parent = null;

        public SettingsActivityFragment() {
            // Required empty public constructor
        }


        public void setParentActivity(SettingsActivity parent) {
            this.parent = parent;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Generate settings view from XML file.
            addPreferencesFromResource(R.xml.pref_general);

            // Add preference change listener to channel option.
            Preference topicPref = findPreference(getString(R.string.pref_push_channel_key));
            topicPref.setOnPreferenceChangeListener(this.parent);

            // Add preference change listener to username option.
            topicPref = findPreference(getString(R.string.pref_push_username_key));
            topicPref.setOnPreferenceChangeListener(this.parent);

            // Add preference change listener to encryption option.
            topicPref = findPreference(getString(R.string.pref_push_encryption_key));
            topicPref.setOnPreferenceChangeListener(this.parent);

            // Add preference change listener to number notifications option.
            topicPref = findPreference(getString(R.string.pref_push_number_notifications_key));
            topicPref.setOnPreferenceChangeListener(this.parent);

        }

    }
}

