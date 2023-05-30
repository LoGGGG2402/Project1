package Schedule;

import AirTable.Airtable;
import Slack.SlackAPI;

public class TimerTask extends java.util.TimerTask{
    private Airtable airtable;
    private SlackAPI slackAPI;

    public TimerTask (Airtable airtable, SlackAPI slackAPI){
        this.airtable = airtable;
        this.slackAPI = slackAPI;
    }
    @Override
    public void run() {
        airtable.syncUserRecord(slackAPI.listUser());
        airtable.syncChannelRecord(slackAPI.listChannel());
    }
}
