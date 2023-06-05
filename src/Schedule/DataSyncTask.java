package Schedule;

import AirTable.Airtable;
import Slack.SlackAPI;

import java.util.Calendar;
import java.util.Timer;

public class DataSyncTask {
    private final Airtable airtable;
    private final SlackAPI slackAPI;
    private final Timer timer;
    private TimerTask timerTask;

    public DataSyncTask(Airtable airtable, SlackAPI slackAPI) {
        this.airtable = airtable;
        this.slackAPI = slackAPI;
        timer = new Timer();
        timerTask = new TimerTask(airtable, slackAPI);
    }

    public void setTimeSync(int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        timerTask.cancel();

        timerTask = new TimerTask(airtable, slackAPI);

        if (calendar.getTime().compareTo(Calendar.getInstance().getTime()) < 0)
            calendar.add(Calendar.DAY_OF_MONTH, 1);

        timer.scheduleAtFixedRate(timerTask, calendar.getTime(), 1000 * 60 * 60 * 24);
    }
}
