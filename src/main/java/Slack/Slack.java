package Slack;

import Log.Logs;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.Conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Slack {
    private final String token = "xoxb-5244767721591-5326693653700-351dqpn8DUCgFiYFwT5gN9k4";
    private final MethodsClient client = com.slack.api.Slack.getInstance().methods(token);
    private final List<User> users = new ArrayList<>();
    private final List<Channel> channels = new ArrayList<>();
    public Slack() {
        Objects.requireNonNull(User.listUsers(client)).forEach(user -> users.add(new User(user, client)));
        Objects.requireNonNull(Channel.listChannels(client)).forEach(channel -> channels.add(new Channel(channel, client)));
    }

    public void syncLocal() {
        users.clear();
        channels.clear();
        Objects.requireNonNull(User.listUsers(client)).forEach(user -> users.add(new User(user, client)));
        Objects.requireNonNull(Channel.listChannels(client)).forEach(channel -> channels.add(new Channel(channel, client)));
    }

    // Getters
    public List<User> getUsers() {
        return users;
    }
    public List<Channel> getChannels() {
        return channels;
    }

    public User getUser(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
    }

    public Channel getChannel(String id) {
        return channels.stream().filter(channel -> channel.getId().equals(id)).findFirst().orElse(null);
    }

    public boolean addUserToChannel(String userId, String channelId) {
        if (channels.get(channels.indexOf(getChannel(channelId))).addUser(userId, client)){
            users.get(users.indexOf(getUser(userId))).addChannelId(channelId);
            Logs.writeLog("User " + userId + " added to channel " + channelId);
            return true;
        }
        Logs.writeLog("User " + userId + " added to channel " + channelId + " failed");
        return false;
    }
    public boolean removeUserFromChannel(String userId, String channelId) {
        if (channels.get(channels.indexOf(getChannel(channelId))).removeUser(userId, client)){
            users.get(users.indexOf(getUser(userId))).removeChannelId(channelId);
            Logs.writeLog("User " + userId + " removed from channel " + channelId);
            return true;
        }
        Logs.writeLog("User " + userId + " removed from channel " + channelId + " failed");
        return false;
    }
    public boolean createChannel(String name) {
        Conversation channel = Channel.createChannel(name, client);
        if (channel != null) {
            channels.add(new Channel(channel, client));
            Logs.writeLog("Channel " + name + " created");
            return true;
        }
        Logs.writeLog("Channel " + name + " created failed");
        return false;
    }
}
