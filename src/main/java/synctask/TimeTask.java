package synctask;

import airtable.AirTable;
import slack.Slack;

import slack.Channel;
import slack.SlackUser;

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
        airtable.reSync();
        airtable.pushData(channelList, userList, false);
    }

}
