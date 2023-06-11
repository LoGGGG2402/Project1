package Main;

import AirTable.AirTable;
import Slack.Channel;
import Slack.Slack;
import Slack.User;
import SyncTask.DataSyncTask;
import com.google.gson.JsonElement;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

public class MainUI {
    private final AirTable airTable;
    private final Slack slack;
    private JPanel mainPanel;
    private JButton createChannelsButton;
    private JButton addUserToChannelButton;
    private JButton removeUserFromChannelButton;
    private JButton airTableToXlsxButton;
    private JButton listAllUsersButton;
    private JButton listAllChannelsButton;
    private JButton syncButton;
    private JButton setSyncTimeButton;
    private JTextField status;
    private JList list;

    public MainUI() {
        airTable = new AirTable();
        slack = new Slack();
        if (!airTable.isValid()){
            status.setText("Error: Could not validate AirTable.");
            return;
        }

        DataSyncTask dataSyncTask = new DataSyncTask(airTable, slack);

        Thread syncThread = new Thread(() -> dataSyncTask.setTimeSync(0, 0, 0));

        syncThread.start();

        listAllChannelsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Vector<String> channels = new Vector<>();
                for (var channel : slack.getChannels())
                    channels.add(channel.toJson().toString());
                list.setListData(channels);
                status.setText("Channels listed");
            }
        });
        listAllUsersButton.addActionListener(e -> {
            Vector<String> users = new Vector<>();
            for (var user : slack.getUsers())
                users.add(user.toJson().toString());
            list.setListData(users);
            status.setText("Users listed");
        });
        createChannelsButton.addActionListener(e -> {
            String channelName = JOptionPane.showInputDialog("Enter channel name:");
            if (slack.createChannel(channelName))
                status.setText("Channel created");
            else
                status.setText("Channel not created");
        });
        addUserToChannelButton.addActionListener(e -> {
            String channelId = JOptionPane.showInputDialog("Enter channel Id:");
            Channel channel = slack.getChannel(channelId);
            if (channel == null){
                status.setText("Channel not found");
                return;
            }

            String userId = JOptionPane.showInputDialog("Enter user Id:");
            User user = slack.getUser(userId);
            if (user == null){
                status.setText("User not found");
                return;
            }

            for (JsonElement r : channel.getMembersId()) {
                if (r.getAsString().equals(userId)) {
                    status.setText("User already in channel");
                    return;
                }
            }

            if (slack.addUserToChannel(channelId, userId))
                status.setText("User added to channel");
            else
                status.setText("User not added to channel");
        });
        removeUserFromChannelButton.addActionListener(e -> {
            String channelId = JOptionPane.showInputDialog("Enter channel Id:");
            Channel channel = slack.getChannel(channelId);
            if (channel == null){
                status.setText("Channel not found");
                return;
            }

            String userId = JOptionPane.showInputDialog("Enter user Id:");
            User user = slack.getUser(userId);
            if (user == null){
                status.setText("User not found");
                return;
            }

            for (JsonElement r : channel.getMembersId()) {
                if (r.getAsString().equals(userId)) {
                    if (slack.removeUserFromChannel(channelId, userId))
                        status.setText("User removed from channel");
                    else
                        status.setText("User not removed from channel");
                    return;
                }
            }
            status.setText("User not in channel");
        });
        syncButton.addActionListener(e -> {
            slack.syncLocal();
            List<Channel> channels = slack.getChannels();
            List<User> users = slack.getUsers();
            if (channels == null || users == null) {
                status.setText("Error: Could not sync Slack.");
                return;
            }
            if (airTable.pushData(channels, users, true))
                status.setText("Data synced");
            else
                status.setText("Data not synced");
        });
        setSyncTimeButton.addActionListener(e -> {
            int hour;
            int minute;
            int second;
            try {
                hour = Integer.parseInt(JOptionPane.showInputDialog("Enter hour:"));
                minute = Integer.parseInt(JOptionPane.showInputDialog("Enter minute:"));
                second = Integer.parseInt(JOptionPane.showInputDialog("Enter second:"));
            } catch (NumberFormatException exception) {
                status.setText("Error: Could not parse time.");
                return;
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                status.setText("Error: Invalid time.");
                return;
            }
            dataSyncTask.setTimeSync(hour, minute, second);
        });
        airTableToXlsxButton.addActionListener(e -> {
            String path = JOptionPane.showInputDialog("Enter path:");
            if (path == null)
                return;
            airTable.exportToXlsx(path);
            status.setText("Data exported to xlsx in " + path);
        });

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainUI");
        frame.setContentPane(new MainUI().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
