package de.alertr.alertralarmnotification;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.content.Intent;


/*
TODO
- if app is not started we can not store notifications
*/

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


        // TODO check if MainActivity instances exists (What happens if we do not have an active GUI?)
        public void run() {
            MainActivity.main_activity.addReceivedMessage(recv_msg);
        }
}

