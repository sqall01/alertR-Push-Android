package de.alertr.alertralarmnotification;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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

}
