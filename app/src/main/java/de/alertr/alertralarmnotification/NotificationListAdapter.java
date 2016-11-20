package de.alertr.alertralarmnotification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NotificationListAdapter extends BaseAdapter {
    private ArrayList<ReceivedMessage> list_data;
    private LayoutInflater layoutInflater;

    public NotificationListAdapter(Context context) {
        this.list_data = new ArrayList<ReceivedMessage>();
        layoutInflater = LayoutInflater.from(context);
    }

    public NotificationListAdapter(Context context, ArrayList<ReceivedMessage> list_data) {
        this.list_data = new ArrayList<ReceivedMessage>();
        this.list_data.addAll(list_data);
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return list_data.size();
    }

    @Override
    public Object getItem(int position) {
        return list_data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        list_data.clear();
    }


    public View getView(int position, View convert_view, ViewGroup parent) {
        ViewHolder holder;
        if (convert_view == null) {
            convert_view = layoutInflater.inflate(R.layout.notification_list_view_layout, null);
            holder = new ViewHolder();
            holder.subject_view = (TextView) convert_view.findViewById(R.id.subject);
            holder.state_view = (TextView) convert_view.findViewById(R.id.state);
            holder.timestamp_view = (TextView) convert_view.findViewById(R.id.timestamp);
            convert_view.setTag(holder);
        } else {
            holder = (ViewHolder) convert_view.getTag();
        }

        boolean is_global_notification = list_data.get(position).getIsGlobalNotification();

        // Convert timestamp to date string.
        long time_in_ms = ((long)list_data.get(position).getTimeTriggered()) * 1000;
        Date date = new Date(time_in_ms);
        DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                                DateFormat.SHORT);
        String time_string = date_format.format(date);

        // Distinguish between sensor alert and global notification.
        String state_string = "";
        if(is_global_notification) {
            state_string = "Notification";
        }
        else {
            if (list_data.get(position).getIsSensorAlert()) {
                int state = list_data.get(position).getState();
                if (state == 1) {
                    state_string = "State: Triggered";
                } else if (state == 0) {
                    state_string = "State: Normal";
                } else {
                    state_string = "State: Unknown";
                }
            }
        }

        holder.subject_view.setText(list_data.get(position).getSubject());
        holder.state_view.setText(state_string);
        // Distinguish between sensor alert and global notification.
        if(is_global_notification) {
            holder.timestamp_view.setText("Issued on " + time_string);
        }
        else {
            holder.timestamp_view.setText("Triggered on " + time_string);
        }


        return convert_view;
    }


    public void addTop(ReceivedMessage item) {

        // Add item in the beginning of the list.
        list_data.add(0, item);
        notifyDataSetChanged();
    }


    public void addBottom(ReceivedMessage item) {
        list_data.add(item);
        notifyDataSetChanged();
    }


    public void addAll(ArrayList<ReceivedMessage> items) {
        list_data.addAll(items);
        notifyDataSetChanged();
    }


    static class ViewHolder {
        TextView subject_view;
        TextView state_view;
        TextView timestamp_view;
    }

}
