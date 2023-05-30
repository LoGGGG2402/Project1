package Main;

import AirTable.Airtable;
import Slack.SlackAPI;
import com.slack.api.model.Conversation;
import com.slack.api.model.User;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Airtable airtable = new Airtable();
        SlackAPI slackAPI = new SlackAPI();
        List<Conversation> channels = slackAPI.listChannel();
        List<User> users = slackAPI.listUser();
        airtable.syncUserRecord(users);
        airtable.syncChannelRecord(channels);
    }
}