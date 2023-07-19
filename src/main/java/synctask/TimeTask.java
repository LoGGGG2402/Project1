package synctask;

import airtable.AirTable;
import logs.Logs;
import slack.Slack;

import slack.Channel;
import slack.SlackUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TimeTask extends java.util.TimerTask {

    private final AirTable airTable;
    private final Slack slack;

    public TimeTask() {
        this.airTable = new AirTable();
        this.slack = new Slack();
    }

    @Override
    public void run() {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)){
            List<Future<Boolean>> futures = new ArrayList<>();
            futures.add(executor.submit(slack::syncLocal));
            futures.add(executor.submit(airTable::reSync));

            for (Future<Boolean> future : futures) {
                if (Boolean.FALSE.equals(future.get())) {
                    Logs.writeLog("Failed to push data to Airtable when scheduled");
                    return;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            Logs.writeLog(e.getMessage());
        }


        List<SlackUser> userList = slack.getUsers();
        List<Channel> channelList = slack.getChannels();


        if (!airTable.pushData(channelList, userList, false)) {
            Logs.writeLog("Failed to push data to Airtable when scheduled");
            airTable.reSync();
        }


        Logs.writeLog("Scheduled sync completed");
    }
}
