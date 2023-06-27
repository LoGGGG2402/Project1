package SyncTask;

import AirTable.AirTable;
import Slack.Slack;

import Slack.Channel;
import Slack.SlackUser;

import java.util.List;

class TimeTask extends java.util.TimerTask {

    private final AirTable airtable;
    private final Slack slack;

    public TimeTask(AirTable airtable, Slack slack) {
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
