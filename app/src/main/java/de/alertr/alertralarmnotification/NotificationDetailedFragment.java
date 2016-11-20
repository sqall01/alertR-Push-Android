package de.alertr.alertralarmnotification;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class NotificationDetailedFragment extends Fragment {

    private ReceivedMessage msg = null;
    private TextView subject_content_view;
    private TextView message_content_view;
    private TextView state_content_view;
    private TextView triggered_on_content_view;
    private TextView sent_on_content_view;
    private TextView received_on_content_view;
    private TextView channel_content_view;

    public NotificationDetailedFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notification_detailed, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        msg = NotificationDetailed.notification_detailed.getMsg();

        // Get instances of the all views
        // (NOTE: getView() can not be called in onCreateView() ).
        subject_content_view = (TextView)getView().findViewById(R.id.subject_content);
        message_content_view = (TextView)getView().findViewById(R.id.message_content);
        state_content_view = (TextView)getView().findViewById(R.id.state_content);
        triggered_on_content_view = (TextView)getView().findViewById(R.id.triggered_on_content);
        sent_on_content_view = (TextView)getView().findViewById(R.id.sent_on_content);
        received_on_content_view = (TextView)getView().findViewById(R.id.received_on_content);
        channel_content_view = (TextView)getView().findViewById(R.id.channel_content);

        if(msg != null) {

            subject_content_view.setText(msg.getSubject());
            message_content_view.setText(msg.getMessage());

            // Set state view.
            String state_string = "";
            if(msg.getIsSensorAlert()) {
                int state = msg.getState();
                if(state == 1) {
                    state_string = "Triggered";
                }
                else if(state == 0) {
                    state_string = "Normal";
                }
                else {
                    state_string = "Unknown";
                }
            }
            state_content_view.setText(state_string);

            // Set triggered on view.
            long time_in_ms = ((long)msg.getTimeTriggered()) * 1000;
            Date date = new Date(time_in_ms);
            DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                                    DateFormat.SHORT);
            String triggered_string = date_format.format(date);
            triggered_on_content_view.setText(triggered_string);

            // Set sent on view.
            time_in_ms = ((long)msg.getTimeSent()) * 1000;
            date = new Date(time_in_ms);
            String sent_string = date_format.format(date);
            sent_on_content_view.setText(sent_string);

            // Set received on view.
            time_in_ms = ((long)msg.getTimeReceived()) * 1000;
            date = new Date(time_in_ms);
            String received_string = date_format.format(date);
            received_on_content_view.setText(received_string);

            channel_content_view.setText(msg.getChannel());
        }
    }

}
