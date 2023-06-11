package Main;

import AirTable.AirTable;
import Slack.Channel;
import Slack.Slack;
import Slack.User;
import SyncTask.DataSyncTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import javax.swing.*;
import java.util.List;
import java.util.Vector;

public class MainUI {
    private  AirTable airTable;
    private  Slack slack;
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
    private JList<String> list;
    private DataSyncTask dataSyncTask;
    Thread syncThread;

    public MainUI() {
        list.setCellRenderer(new ColorfulCellRenderer());

        listAllChannelsButton.addActionListener(e -> {
            DefaultListModel<String> model = new DefaultListModel<>();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            for (var channel : slack.getChannels()){
                String json = gson.toJson(channel.toJson());
                String html = "<html>" + json.replaceAll("\n", "<br>").replaceAll(" ", "&nbsp;") + "</html>";
                model.addElement(html);
            }
            list.setModel(model);
            status.setText("Channels listed");
        });
        listAllUsersButton.addActionListener(e -> {
            Vector<String> users = new Vector<>();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            for (var user : slack.getUsers()){
                String json = gson.toJson(user.toJson());
                String html = "<html>" + json.replaceAll("\n", "<br>  ").replaceAll(" ", "&nbsp;") + "</html>";
                users.add(html);
            }

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

            if (user.isBot()){
                status.setText("User is a bot");
                return;
            }

            for (JsonElement r : channel.getMembersId()) {
                if (r.getAsString().equals(userId)) {
                    status.setText("User already in channel");
                    return;
                }
            }

            if (slack.addUserToChannel(userId, channelId))
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
                    if (slack.removeUserFromChannel(userId, channelId))
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

    public void setProperties(AirTable airTable, Slack slack) {
        this.airTable = airTable;
        this.slack = slack;
        dataSyncTask = new DataSyncTask(airTable, slack);
        dataSyncTask.setTimeSync(0, 0, 0);


        syncThread = new Thread(() -> dataSyncTask.setTimeSync(0, 0, 0));
        syncThread.start();
    }

    public static void main(String[] args) {
        JDialog loadingDialog = new JDialog();
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setModal(true);
        loadingDialog.setSize(200, 100);
        loadingDialog.setLocationRelativeTo(null);

        JLabel loadingLabel = new JLabel("Loading...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingDialog.add(loadingLabel);

        JFrame frame = new JFrame("MainUI");
        MainUI mainUI = new MainUI();
        frame.setContentPane(mainUI.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                AirTable airTable = new AirTable();
                Slack slack = new Slack();
                mainUI.setProperties(airTable, slack);
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                frame.setVisible(true);
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }
}
