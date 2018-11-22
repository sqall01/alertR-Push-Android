package de.alertr.alertralarmnotification;

import android.net.Uri;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class MainActivity extends AppCompatActivity {

    public static MainActivity main_activity = null;

    private static final String LOGTAG = "MainActivity";
    private MainActivityFragment main_activity_fragment = null;
    private SplashFragment main_splash_fragment = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main_activity_fragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main_activity);
        main_splash_fragment = (SplashFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main_splash);
        main_activity = this;
        makeSplashVisible();

        // Check if Google Play Services are available.
        int is_available = GoogleApiAvailability. getInstance().isGooglePlayServicesAvailable(this);
        if(is_available != ConnectionResult.SUCCESS) {

            CharSequence text = getString(R.string.pref_play_unavail);
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();

            GoogleApiAvailability. getInstance().makeGooglePlayServicesAvailable(this);
        }

        // Initiate configuration (needs object of this main activity).
        Config config = Config.getInstance();

        // Check if the app is already configured, otherwise show the preference screen.
        if(!config.isConfigured()) {
            Intent temp_intent = new Intent(this, SettingsActivity.class);
            startActivity(temp_intent);

            // Display a short message to configure the application.
            CharSequence text = getString(R.string.pref_not_complete);
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        }

        // Do startup in separated thread.
        StartupThread startup_thread = new StartupThread();
        startup_thread.start();
    }

    // Since we use "singleTask" launchMode, this function is called if we go back from
    // the detailed view.
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Refresh list to show if a task is read or not.
        main_activity_fragment.refreshListView();
    }

    public void refreshListView() {
        // Refresh list to show if a task is read or not.
        main_activity_fragment.refreshListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings) {

            // DEBUG
            Log.d(LOGTAG, "Settings Menu");

            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        else if(id == R.id.action_privacy) {

            // DEBUG
            Log.d(LOGTAG, "Privacy Policy Menu");

            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                              Uri.parse("https://alertr.de/legal.php"));
            startActivity(browserIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addReceivedMessage(ReceivedMessage recv_msg) {

        // DEBUG
        Log.d(LOGTAG, "addReceivedMessage()");

        // If the last elements in the array list (the oldest ones)
        // were removed, refresh the whole list view.
        NotificationData notification_data = NotificationData.getInstance();
        if(notification_data.addReceivedMessage(recv_msg)) {
            main_activity_fragment.refreshListView();
        }
        else {
            main_activity_fragment.addItemToListView(recv_msg);
        }
    }

    public void updateMaxNumberReceivedMsgs() {

        // DEBUG
        Log.d(LOGTAG, "updateMaxNumberReceivedMsgs()");

        // Remove the last elements in the array list (the oldest ones)
        // and refresh view.
        NotificationData notification_data = NotificationData.getInstance();
        if(notification_data.updateMaxNumberReceivedMsgs()) {
            main_activity_fragment.refreshListView();
        }
    }


    public void showNotificationDetailedFragment(ReceivedMessage msg) {

        Intent intent = new Intent(this, NotificationDetailed.class);
        intent.putExtra("id", msg.getId());
        startActivity(intent);
    }

    public void makeMainVisible() {
        // Switch visibility to main activity fragment.
        main_splash_fragment.getView().setVisibility(View.GONE);
        main_activity_fragment.getView().setVisibility(View.VISIBLE);
    }

    public void makeSplashVisible() {
        // Switch visibility to splash fragment.
        main_activity_fragment.getView().setVisibility(View.GONE);
        main_splash_fragment.getView().setVisibility(View.VISIBLE);
    }
}


// Class for adding a received message to the main activity
// (used to execute the code in the main thread started from the service thread).
class AddReceivedMessage implements Runnable {

        private ReceivedMessage recv_msg;

        AddReceivedMessage(ReceivedMessage recv_msg) {
            this.recv_msg = recv_msg;
        }

        public void run() {
            MainActivity.main_activity.addReceivedMessage(recv_msg);
        }
}


// Class that handles startup of app in separated thread.
class StartupThread extends Thread {

    private static final String LOGTAG = "StartupThread";

    @Override
    public void run() {
        // Load existing received encrypted messages (if exists).
        ArrayList<ReceivedEncryptedMessage> received_enc_messages = null;
        try {
            File file_obj = MainActivity.main_activity.getBaseContext().getFileStreamPath("encrypted_msgs.dat");
            if(file_obj.exists()) {

                // Read data from file.
                FileInputStream fis = MainActivity.main_activity.openFileInput(file_obj.getName());
                StringBuilder file_content = new StringBuilder("");
                byte[] buffer = new byte[1024];
                int n;
                while((n = fis.read(buffer)) != -1) {
                    file_content.append(new String(buffer, 0, n));
                }
                fis.close();

                // Deserialize data to received messages objects.
                Gson gson = new Gson();
                Type collection_type = new TypeToken<ArrayList<ReceivedEncryptedMessage>>(){}.getType();
                received_enc_messages = gson.fromJson(file_content.toString(), collection_type);

                file_obj.delete();
            }
        }
        catch(Throwable e) {
            Log.e(LOGTAG, "Reading received encrypted messages failed.");
            e.printStackTrace();
        }

        // If encrypted received messages exist, decrypt and add them to our internal data.
        if(received_enc_messages != null) {

            NotificationData notification_data_obj = NotificationData.getInstance();

            for(ReceivedEncryptedMessage recv_enc_msg : received_enc_messages) {
                byte[] encryption_key = Config.getInstance().getEncryption_key(
                        recv_enc_msg.getChannel());

                try {
                    ReceivedMessage msg = AlertrFirebaseMessaging.decrypt_encrypted_msg(
                            recv_enc_msg,
                            encryption_key);

                    notification_data_obj.addReceivedMessage(msg);
                }
                // Either received message was illegal or decryption failed.
                catch(org.json.JSONException e) {
                    Log.e(LOGTAG, "Decrypted message illegal.");
                    e.printStackTrace();
                    AlertrFirebaseMessaging.issueErrorNotification(
                            MainActivity.main_activity,
                            0,
                            "Message Error",
                            "Decryption failed or received message illegal.");
                }

                // Error thrown when decryption options are wrong.
                catch(Throwable e) {
                    Log.e(LOGTAG, "Received message payload illegal.");
                    e.printStackTrace();
                    AlertrFirebaseMessaging.issueErrorNotification(
                            MainActivity.main_activity,
                            0,
                            "Message Error",
                            "Received illegal message.");
                }
            }
        }

        // After loading all messages, refresh the list view of the main activity.
        MainActivity.main_activity.refreshListView();

        // For now until transitions are used so the splash screen can still be
        // seen when loading is too fast.
        SystemClock.sleep(500);

        // Switch from splash to main activity fragment.
        MainActivity.main_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.main_activity.makeMainVisible();
            }
        });
    }
}