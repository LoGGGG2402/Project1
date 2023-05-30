package Schedule;

import AirTable.Airtable;
import Slack.SlackAPI;

import javax.swing.*;
import java.util.Calendar;

public class DataSyncTask{
    TimerTask timerTask;
    public DataSyncTask(Airtable airtable, SlackAPI slackAPI){
        timerTask = new TimerTask(airtable, slackAPI);
    }
}
