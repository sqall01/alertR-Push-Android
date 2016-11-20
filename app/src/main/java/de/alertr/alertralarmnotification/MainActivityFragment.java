package de.alertr.alertralarmnotification;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;



public class MainActivityFragment extends Fragment {

    private ListView notification_view;
    private NotificationListAdapter notification_adapter;

    public MainActivityFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_activity, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // Get instance of the list view
        // (NOTE: getView() can not be called in onCreateView() ).
        notification_view = (ListView)getView().findViewById(R.id.notification_list);

        notification_adapter = new NotificationListAdapter(getContext());

        notification_view.setAdapter(notification_adapter);

        // Register an item click listener to show a detailed view of each notification if clicked.
        notification_view.setOnItemClickListener(new NotificationViewClickListener());
    }

    public void addItemToListView(ReceivedMessage item) {
        notification_adapter.addTop(item);
    }

    public void refreshListView() {
        if(notification_adapter != null) {
            notification_adapter.clear();
            notification_adapter.addAll(NotificationData.getInstance().getReceivedMsgs());
        }
    }
}


class NotificationViewClickListener implements AdapterView.OnItemClickListener {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        NotificationListAdapter adapter = (NotificationListAdapter)parent.getAdapter();
        ReceivedMessage item = (ReceivedMessage)adapter.getItem(position);
        MainActivity.main_activity.showNotificationDetailedFragment(item);
    }
}