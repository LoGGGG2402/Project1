package Main;

import AirTable.AirTable;
import Slack.*;
import SyncTask.DataSyncTask;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        AirTable airTable = new AirTable();
        if (!airTable.isValid()){
            System.out.println("Error: Could not validate AirTable.");
            return;
        }
        Slack slack = new Slack();

        DataSyncTask dataSyncTask = new DataSyncTask(airTable, slack);

        Thread syncThread = new Thread(() -> dataSyncTask.setTimeSync(0, 0, 0));

        syncThread.start();

        Scanner scanner = new Scanner(System.in);

        while (true){
            System.out.println("Choose one of the following options:");
            System.out.println("1. List all users");
            System.out.println("2. List all channels");
            System.out.println("3. Create a new channel");
            System.out.println("4. Add a new user to a channel");
            System.out.println("5. Remove a user from a channel");
            System.out.println("6. Sync data");
            System.out.println("7. Set sync time");
            System.out.println("0. Exit");

            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> {
                    for (var user : slack.getUsers())
                        System.out.println(user.toJson());
                }
                case 2 -> {
                    for (var channel : slack.getChannels())
                        System.out.println(channel.toJson());
                }
                case 3 -> {
                    System.out.println("Enter channel name:");
                    String channelName = scanner.nextLine();
                    System.out.println(channelName);
                    if (slack.createChannel(channelName))
                        System.out.println("Channel created");
                    else
                        System.out.println("Channel not created");
                }
                case 4 -> {
                    System.out.print("Enter channel id: ");
                    String channelId = scanner.nextLine();
                    System.out.print("Enter user id: ");
                    String userId = scanner.nextLine();
                    Channel channel = slack.getChannel(channelId);
                    User user = slack.getUser(userId);
                    if (channel == null) {
                        System.out.println("Channel not found");
                        break;
                    }
                    if (user == null) {
                        System.out.println("User not found");
                        break;
                    }
                    JsonElement jsonElement = new Gson().fromJson(channelId, JsonElement.class);

                    if (channel.getMembersId().contains(jsonElement)){
                        System.out.println("User already in channel");
                        break;
                    }
                    if (slack.addUserToChannel(userId, channelId))
                        System.out.println("User added to channel");
                    else
                        System.out.println("User not added to channel");
                }
                case 5 -> {
                    System.out.print("Enter channel id: ");
                    String channelId = scanner.nextLine();
                    System.out.print("Enter user id: ");
                    String userId = scanner.nextLine();
                    Channel channel = slack.getChannel(channelId);
                    User user = slack.getUser(userId);
                    if (channel == null) {
                        System.out.println("Channel not found");
                        break;
                    }
                    if (user == null) {
                        System.out.println("User not found");
                        break;
                    }
                    JsonElement jsonElement = new Gson().fromJson(channelId, JsonElement.class);
                    if(!channel.getMembersId().contains(jsonElement)){
                        System.out.println("User not in channel");
                        break;
                    }
                    if (slack.removeUserFromChannel(userId, channelId))
                        System.out.println("User removed from channel");
                    else
                        System.out.println("User not removed from channel");
                }
                case 6 -> {
                    slack.syncLocal();
                    List<Channel> channels = slack.getChannels();
                    List<User> users = slack.getUsers();
                    if (channels == null || users == null) {
                        System.out.println("Error getting data");
                        break;
                    }
                    if (airTable.pushData(channels, users, true))
                        System.out.println("Data synced");
                    else
                        System.out.println("Data not synced");
                }
                case 7 -> {
                    System.out.print("Enter hour: ");
                    int hour = Integer.parseInt(scanner.nextLine());
                    System.out.print("Enter minute: ");
                    int minute = Integer.parseInt(scanner.nextLine());
                    System.out.print("Enter second: ");
                    int second = Integer.parseInt(scanner.nextLine());
                    dataSyncTask.setTimeSync(hour, minute, second);
                }
                case 0 -> System.exit(0);
                default -> System.out.println("Invalid choice");
            }

        }
    }
}
