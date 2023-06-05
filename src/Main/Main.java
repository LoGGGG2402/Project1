package Main;

import AirTable.Airtable;
import Schedule.DataSyncTask;
import Slack.SlackAPI;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Airtable airtable = new Airtable();
        SlackAPI slackAPI = new SlackAPI();

        DataSyncTask dataSyncTask = new DataSyncTask(airtable, slackAPI);

        Thread syncThread = new Thread(() -> dataSyncTask.setTimeSync(0, 0, 0));

        syncThread.start();

        Scanner scanner = new Scanner(System.in);

        while (true){
            System.out.println("Choose one of the following options:");
            System.out.println("1. List all users");
            System.out.println("2. List all channels");
            System.out.println("3. Create a new channel");
            System.out.println("4. Add a new user to a channel");
            System.out.println("5. Sync data");
            System.out.println("6. Set sync time");
            System.out.println("7. Exit");

            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> System.out.println(slackAPI.listUser());
                case 2 -> System.out.println(slackAPI.listChannel());
                case 3 -> {
                    System.out.println("Enter channel name:");
                    String channelName = scanner.nextLine();
                    System.out.println(channelName);
                    slackAPI.createChannel(channelName);
                }
                case 4 -> {
                    System.out.print("Enter channel id: ");
                    String channelId = scanner.nextLine();
                    System.out.print("Enter number of users: ");
                    int numberOfUsers = Integer.parseInt(scanner.nextLine());
                    List<String> userIds = new java.util.ArrayList<>();
                    for (int i = 0; i < numberOfUsers; i++) {
                        System.out.print("Enter user id: ");
                        userIds.add(scanner.nextLine());
                    }
                    slackAPI.addUserToChannel(userIds, channelId);
                }
                case 5 -> {
                    airtable.syncUserRecord(slackAPI.listUser());
                    airtable.syncChannelRecord(slackAPI.listChannel());
                }
                case 6 -> {
                    System.out.println("Enter hour:");
                    int hour = Integer.parseInt(scanner.nextLine());
                    System.out.println("Enter minute:");
                    int minute = Integer.parseInt(scanner.nextLine());
                    System.out.println("Enter second:");
                    int second = Integer.parseInt(scanner.nextLine());
                    syncThread.interrupt();
                    try {
                        syncThread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    syncThread = new Thread(() -> dataSyncTask.setTimeSync(hour, minute, second));
                    syncThread.start();
                }
                case 7 -> System.exit(0);
                default -> System.out.println("Invalid choice");
            }
        }




    }
}