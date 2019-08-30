package de.alertr.alertralarmnotification;


import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NotificationData {

    class CompareTimestampsDesc implements Comparator<ReceivedMessage> {
        @Override
        public int compare(ReceivedMessage o1, ReceivedMessage o2) {
            int temp1 = o1.getTimeTriggered();
            int temp2 = o2.getTimeTriggered();

            if(temp1 < temp2) {
                return 1;
            }
            else if (temp1 > temp2) {
                return -1;
            }
            return 0;
        }
    }

    private static final String LOGTAG = "NotificationData";
    private static NotificationData notification_data = null;

    private String file_name = "received_msgs.json";
    private ArrayList<ReceivedMessage> received_messages = null;
    private int max_number_received_msgs = 1000;
    private long next_id = 0;

    public static NotificationData getInstance() {
        if(notification_data == null) {
            new NotificationData();
        }
        return notification_data;
    }


    private NotificationData() {
        received_messages = new ArrayList<ReceivedMessage>();

        // Set max number of received msgs.
        max_number_received_msgs = Config.getInstance().getMax_number_received_msgs();

        loadStoredData();

        notification_data = this;
    }


    public void loadStoredData() {

        // DEBUG
        Log.d(LOGTAG, "loadStoredData()");

        if(MainActivity.main_activity != null) {
            Context context = MainActivity.main_activity;

            try {
                // Read data from file.
                FileInputStream fis = context.openFileInput(file_name);
                StringBuilder file_content = new StringBuilder("");
                byte[] buffer = new byte[1024];
                int n;
                while((n = fis.read(buffer)) != -1) {
                    file_content.append(new String(buffer, 0, n));
                }
                fis.close();

                // Deserialize data to received messages objects.
                Gson gson = new Gson();
                Type collection_type = new TypeToken<ArrayList<ReceivedMessage>>(){}.getType();
                received_messages = gson.fromJson(file_content.toString(), collection_type);
                Collections.sort(received_messages, new CompareTimestampsDesc());

                // NOTE: a modified file can result in duplicated ids for received message objects.
                // But since this is not used in a security critical way, at the worst a
                // wrong notification will be shown when a duplicated id occures.
            }
            catch(Throwable e) {
                Log.e(LOGTAG, "Loading stored data failed.");
                e.printStackTrace();
            }

            // Search for the largest id => set next expected id.
            for(ReceivedMessage item : received_messages) {
                if(item.getId() > next_id) {
                    next_id = item.getId();
                }
            }
            next_id++;
        }
    }


    public void storeData() {

        // DEBUG
        Log.d(LOGTAG, "storeData()");

        if(MainActivity.main_activity != null) {
            Context context = MainActivity.main_activity;

            try {
                Gson gson = new Gson();
                String json = gson.toJson(received_messages);
                FileOutputStream fos = context.openFileOutput(file_name, Context.MODE_PRIVATE);
                fos.write(json.getBytes());
                fos.close();
            }
            catch(Throwable e) {
                Log.e(LOGTAG, "Storing data failed.");
                e.printStackTrace();
            }
        }
    }


    public boolean updateMaxNumberReceivedMsgs() {

        // DEBUG
        Log.d(LOGTAG, "updateMaxNumberReceivedMsgs()");
        max_number_received_msgs = Config.getInstance().getMax_number_received_msgs();
        boolean msgs_removed = false;

        // Remove the last elements in the array list (the oldest ones).
        while(received_messages.size() > max_number_received_msgs) {
            received_messages.remove(received_messages.size() - 1);
            msgs_removed = true;
        }

        storeData();

        return msgs_removed;
    }


    public boolean addReceivedMessage(ReceivedMessage recv_msg) {

        if(recv_msg.getId() != next_id) {
            throw new IllegalArgumentException("Received message's id illegal.");
        }

        boolean msgs_removed = false;

        // DEBUG
        Log.d(LOGTAG, "addReceivedMessage()");

        received_messages.add(recv_msg);
        next_id++;
        Collections.sort(received_messages, new CompareTimestampsDesc());

        if(received_messages.size() > max_number_received_msgs) {
            // Remove the last elements in the array list (the oldest ones).
            while (received_messages.size() > max_number_received_msgs) {
                received_messages.remove(received_messages.size() - 1);
                msgs_removed = true;
            }
        }

        storeData();

        return msgs_removed;
    }


    public void clearReceivedMessages() {

        // DEBUG
        Log.d(LOGTAG, "clearReceivedMessages()");

        received_messages.clear();

        storeData();

    }


    public ArrayList<ReceivedMessage> getReceivedMsgs() {
        return new ArrayList<ReceivedMessage>(received_messages);
    }


    public long getNextId() {
        return next_id;
    }


    public ReceivedMessage getReceivedMsg(long id) {
        for(ReceivedMessage msg : received_messages) {
            if(msg.getId() == id) {
                return msg;
            }
        }
        return null;
    }
}


class ReceivedEncryptedMessage {

    private String channel;
    private boolean is_global_notification;
    private String payload;
    private int time_received;

    ReceivedEncryptedMessage(String channel,
                             boolean is_global_notification,
                             int time_received,
                             String payload) {
        this.channel = channel;
        this.is_global_notification = is_global_notification;
        this.time_received = time_received;
        this.payload = payload;
    }

    public String getChannel() {
        return channel;
    }

    public String getPayload() {
        return payload;
    }

    public boolean is_global_notification() {
        return is_global_notification;
    }

    public int getTime_received() {
        return time_received;
    }
}


class ReceivedMessage {

    private long id;
    private String channel;
    private String subject;
    private String message;
    private int time_triggered;
    private int time_sent;
    private int time_received;
    private boolean is_sensor_alert;
    private int state;
    private boolean is_global_notification;
    private boolean msg_read;

    ReceivedMessage(long id,
                    String channel,
                    String subject,
                    String message,
                    int time_triggered,
                    int time_sent,
                    int time_received,
                    boolean is_sensor_alert,
                    int state,
                    boolean is_global_notification,
                    boolean msg_read) {
        this.id = id;
        this.channel = channel;
        this.subject = subject;
        this.message = message;
        this.time_triggered = time_triggered;
        this.time_sent = time_sent;
        this.time_received = time_received;
        this.is_sensor_alert = is_sensor_alert;
        this.state = state;
        this.is_global_notification = is_global_notification;
        this.msg_read = msg_read;
    }

    public long getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public int getTimeTriggered() {
        return time_triggered;
    }

    public int getTimeSent() {
        return time_sent;
    }

    public int getTimeReceived() {
        return time_received;
    }

    public boolean getIsSensorAlert() {
        return is_sensor_alert;
    }

    public int getState() {
        if(is_sensor_alert) {
            return state;
        }
        return -1;
    }

    public boolean getIsGlobalNotification() {
        return is_global_notification;
    }

    public boolean getMsg_read() {
        return msg_read;
    }

    public void setMsg_read(boolean msg_read) {
        this.msg_read = msg_read;
    }
}