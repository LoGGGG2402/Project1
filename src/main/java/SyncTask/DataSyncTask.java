package SyncTask;

import AirTable.AirTable;
import Slack.Channel;
import Slack.Slack;
import Slack.SlackUser;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;

public class DataSyncTask {
    private final AirTable airtable;
    private final Slack slack;
    private final Timer timer;
    private TimeTask timeTask;

    public DataSyncTask(AirTable airtable, Slack slack) {
        this.airtable = airtable;
        this.slack = slack;
        timer = new Timer();
        timeTask = new TimeTask(airtable, slack);
    }

    public void setTimeSync(int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        timeTask.cancel();

        timeTask = new TimeTask(airtable, slack);

        if (calendar.getTime().compareTo(Calendar.getInstance().getTime()) < 0)
            calendar.add(Calendar.DAY_OF_MONTH, 1);

        timer.scheduleAtFixedRate(timeTask, calendar.getTime(), 1000 * 60 * 60 * 24);
    }
}

class TimeTask extends java.util.TimerTask{

    private final AirTable airtable;
    private final Slack slack;

    public TimeTask (AirTable airtable, Slack slack){
        this.airtable = airtable;
        this.slack = slack;
    }

    @Override
    public void run() {
        slack.syncLocal();
        List<SlackUser> userList = slack.getUsers();
        List<Channel> channelList = slack.getChannels();
        airtable.pushData(channelList, userList, false);
    }

}
