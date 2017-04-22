package de.alertr.alertralarmnotification;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.content.Intent;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class MainActivity extends AppCompatActivity {

    public static MainActivity main_activity = null;

    private static final String LOGTAG = "MainActivity";
    private MainActivityFragment main_activity_fragment = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().findFragmentById(R.id.fragment_main_activity);
        main_activity_fragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main_activity);

        main_activity = this;

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

        // Load existing received encrypted messages (if exists).
        ArrayList<ReceivedEncryptedMessage> received_enc_messages = null;
        try {
            File file_obj = getBaseContext().getFileStreamPath("encrypted_msgs.dat");
            if(file_obj.exists()) {

                // Read data from file.
                FileInputStream fis = this.openFileInput(file_obj.getName());
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
                            this,
                            0,
                            "Message Error",
                            "Decryption failed or received message illegal.");
                }

                // Error thrown when decryption options are wrong.
                catch(Throwable e) {
                    Log.e(LOGTAG, "Received message payload illegal.");
                    e.printStackTrace();
                    AlertrFirebaseMessaging.issueErrorNotification(
                            this,
                            0,
                            "Message Error",
                            "Received illegal message.");
                }
            }
        }

        main_activity_fragment.refreshListView();

    }

    // Since we use "singleTask" launchMode, this function is called if we go back from
    // the detailed view.
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

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

