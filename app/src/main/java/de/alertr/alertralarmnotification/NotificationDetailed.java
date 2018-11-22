package de.alertr.alertralarmnotification;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class NotificationDetailed extends AppCompatActivity {

    public static NotificationDetailed notification_detailed = null;

    private ReceivedMessage msg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        long id = intent.getLongExtra("id", -1);
        msg = NotificationData.getInstance().getReceivedMsg(id);

        notification_detailed = this;

        setContentView(R.layout.activity_notification_detailed);
    }


    public ReceivedMessage getMsg() {
        return msg;
    }


    @Override
    public void onBackPressed() {

        // Refresh list to show if a task is read or not when someone presses the back button.
        if(MainActivity.main_activity != null) {
            MainActivity.main_activity.refreshListView();
        }
        finish();
    }
}
