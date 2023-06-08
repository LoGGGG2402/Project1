package Main;

import AirTable.Airtable;
import Schedule.DataSyncTask;
import Slack.SlackAPI;
import com.slack.api.model.Conversation;
import com.slack.api.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainUI {
    private final Airtable airtable;
    private final SlackAPI slackAPI;
    private final DataSyncTask dataSyncTask;
    private Thread syncThread;
    private JFrame frame;
    private JTextArea outputTextArea;

    public MainUI() {
        airtable = new Airtable();
        slackAPI = new SlackAPI();
        dataSyncTask = new DataSyncTask(airtable, slackAPI);
        syncThread = new Thread(() -> dataSyncTask.setTimeSync(0, 0, 0));
        syncThread.start();
    }

    public void createAndShowUI() {
        frame = new JFrame("Slack App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(7, 1));

        JButton listUsersButton = new JButton("List all users");
        listUsersButton.addActionListener(e -> {
            List<User> userList = slackAPI.listUser();
            outputTextArea.append("Users:\n");
            for (var user : userList) {
                outputTextArea.append(user.getName() + " " + user.getId() + "\n");
            }
            outputTextArea.append("\n");
        });
        buttonPanel.add(listUsersButton);

        JButton listChannelsButton = new JButton("List all channels");
        listChannelsButton.addActionListener(e -> {
            List<Conversation> channelList = slackAPI.listChannel();
            outputTextArea.append("Channels:\n");
            for (var channel : channelList) {
                outputTextArea.append(channel.getName() + " " + channel.getId() + "\n");
            }
            outputTextArea.append("\n");
        });
        buttonPanel.add(listChannelsButton);

        JButton createChannelButton = new JButton("Create a new channel");
        createChannelButton.addActionListener(e -> {
            String channelName = JOptionPane.showInputDialog(frame, "Enter channel name:");
            outputTextArea.append("Creating new channel: " + channelName + "\n");
            slackAPI.createChannel(channelName);
            outputTextArea.append("New channel created\n\n");
        });
        buttonPanel.add(createChannelButton);

        JButton addUserToChannelButton = new JButton("Add a new user to a channel");
        addUserToChannelButton.addActionListener(e -> {
            String channelId = JOptionPane.showInputDialog(frame, "Enter channel id:");
            int numberOfUsers = Integer.parseInt(JOptionPane.showInputDialog(frame, "Enter number of users:"));
            List<String> userIdList = new java.util.ArrayList<>();
            for (int i = 0; i < numberOfUsers; i++) {
                String userId = JOptionPane.showInputDialog(frame, "Enter user id:");
                userIdList.add(userId);
            }
            slackAPI.addUserToChannel(userIdList, channelId);
            outputTextArea.append("Added " + numberOfUsers + " user(s) to channel " + channelId + "\n\n");
        });
        buttonPanel.add(addUserToChannelButton);

        JButton syncDataButton = new JButton("Sync data");
        syncDataButton.addActionListener(e -> {
            airtable.syncUserRecord(slackAPI.listUser());
            airtable.syncChannelRecord(slackAPI.listChannel());
            outputTextArea.append("Data sync complete\n\n");
        });
        buttonPanel.add(syncDataButton);

        JButton setSyncTimeButton = new JButton("Set sync time");
        setSyncTimeButton.addActionListener(e -> {
            int hour = Integer.parseInt(JOptionPane.showInputDialog(frame, "Enter hour:"));
            int minute = Integer.parseInt(JOptionPane.showInputDialog(frame, "Enter minute:"));
            int second = Integer.parseInt(JOptionPane.showInputDialog(frame, "Enter second:"));

            syncThread.interrupt();
            try {
                syncThread.join();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            syncThread = new Thread(() -> dataSyncTask.setTimeSync(hour, minute, second));
            syncThread.start();
            outputTextArea.append("Sync time set to: " + hour + ":" + minute + ":" + second + "\n\n");
        });
        buttonPanel.add(setSyncTimeButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        buttonPanel.add(exitButton);

        panel.add(buttonPanel, BorderLayout.WEST);

        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        MainUI mainUI = new MainUI();
        mainUI.createAndShowUI();
    }
}
