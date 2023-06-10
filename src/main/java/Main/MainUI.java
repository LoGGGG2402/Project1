package Main;

import AirTable.AirTable;
import Slack.Channel;
import Slack.Slack;
import Slack.User;
import SyncTask.DataSyncTask;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainUI {
    private final AirTable airTable;
    private final Slack slack;
    private DataSyncTask dataSyncTask;
    private JFrame frame;

    public MainUI() {
        airTable = new AirTable();
        slack = new Slack();
        if (!airTable.isValid()) {
            System.out.println("Error: Could not validate AirTable.");
            return;
        }
        dataSyncTask = new DataSyncTask(airTable, slack);
    }

    public void createAndShowGUI() {
        frame = new JFrame("Slack Data Sync");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(4, 2));

        JLabel label = new JLabel("Choose one of the following options:");
        frame.add(label);

        JButton listUsersButton = new JButton("List all users");
        listUsersButton.addActionListener(e -> listAllUsers());
        frame.add(listUsersButton);

        JButton listChannelsButton = new JButton("List all channels");
        listChannelsButton.addActionListener(e -> listAllChannels());
        frame.add(listChannelsButton);

        JButton createChannelButton = new JButton("Create a new channel");
        createChannelButton.addActionListener(e -> createNewChannel());
        frame.add(createChannelButton);

        JButton addUserToChannelButton = new JButton("Add a new user to a channel");
        addUserToChannelButton.addActionListener(e -> addUserToChannel());
        frame.add(addUserToChannelButton);

        JButton removeUserFromChannelButton = new JButton("Remove a user from a channel");
        removeUserFromChannelButton.addActionListener(e -> removeUserFromChannel());
        frame.add(removeUserFromChannelButton);

        JButton syncDataButton = new JButton("Sync data");
        syncDataButton.addActionListener(e -> syncData());
        frame.add(syncDataButton);

        JButton setSyncTimeButton = new JButton("Set sync time");
        setSyncTimeButton.addActionListener(e -> setSyncTime());
        frame.add(setSyncTimeButton);

        JButton airTableToXlsxButton = new JButton("AirTable to Xlsx");
        airTableToXlsxButton.addActionListener(e -> airTableToXlsx());
        frame.add(airTableToXlsxButton);

        frame.pack();
        frame.setVisible(true);
    }

    private void listAllUsers() {
        for (var user : slack.getUsers())
            System.out.println(user.toJson());
    }

    private void listAllChannels() {
        for (var channel : slack.getChannels())
            System.out.println(channel.toJson());
    }

    private void createNewChannel() {
        String channelName = JOptionPane.showInputDialog(frame, "Enter channel name:");
        if (channelName != null) {
            if (slack.createChannel(channelName))
                JOptionPane.showMessageDialog(frame, "Channel created");
            else
                JOptionPane.showMessageDialog(frame, "Channel not created");
        }
    }

    private void addUserToChannel() {
        String channelId = JOptionPane.showInputDialog(frame, "Enter channel id:");
        String userId = JOptionPane.showInputDialog(frame, "Enter user id:");
        Channel channel = slack.getChannel(channelId);
        User user = slack.getUser(userId);
        if (channel == null) {
            JOptionPane.showMessageDialog(frame, "Channel not found");
            return;
        }
        if (user == null) {
            JOptionPane.showMessageDialog(frame, "User not found");
            return;
        }
        JsonElement jsonElement = new Gson().fromJson(channelId, JsonElement.class);
        if (channel.getMembersId().contains(jsonElement)) {
            JOptionPane.showMessageDialog(frame, "User already in channel");
            return;
        }
        if (slack.addUserToChannel(userId, channelId))
            JOptionPane.showMessageDialog(frame, "User added to channel");
        else
            JOptionPane.showMessageDialog(frame, "User not added to channel");
    }

    private void removeUserFromChannel() {
        String channelId = JOptionPane.showInputDialog(frame, "Enter channel id:");
        String userId = JOptionPane.showInputDialog(frame, "Enter user id:");
        Channel channel = slack.getChannel(channelId);
        User user = slack.getUser(userId);
        if (channel == null) {
            JOptionPane.showMessageDialog(frame, "Channel not found");
            return;
        }
        if (user == null) {
            JOptionPane.showMessageDialog(frame, "User not found");
            return;
        }
        JsonElement jsonElement = new Gson().fromJson(channelId, JsonElement.class);
        if (!channel.getMembersId().contains(jsonElement)) {
            JOptionPane.showMessageDialog(frame, "User not in channel");
            return;
        }
        if (slack.removeUserFromChannel(userId, channelId))
            JOptionPane.showMessageDialog(frame, "User removed from channel");
        else
            JOptionPane.showMessageDialog(frame, "User not removed from channel");
    }

    private void syncData() {
        slack.syncLocal();
        List<Channel> channels = slack.getChannels();
        List<User> users = slack.getUsers();
        if (channels == null || users == null) {
            JOptionPane.showMessageDialog(frame, "Error getting data");
            return;
        }
        if (airTable.pushData(channels, users, true))
            JOptionPane.showMessageDialog(frame, "Data synced");
        else
            JOptionPane.showMessageDialog(frame, "Data not synced");
    }

    private void setSyncTime() {
        String hourString = JOptionPane.showInputDialog(frame, "Enter hour:");
        String minuteString = JOptionPane.showInputDialog(frame, "Enter minute:");
        String secondString = JOptionPane.showInputDialog(frame, "Enter second:");
        try {
            int hour = Integer.parseInt(hourString);
            int minute = Integer.parseInt(minuteString);
            int second = Integer.parseInt(secondString);
            dataSyncTask.setTimeSync(hour, minute, second);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid time format");
        }
    }

    private void airTableToXlsx() {
        airTable.tableToXlsx();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI mainUI = new MainUI();
            mainUI.createAndShowGUI();
        });
    }
}
