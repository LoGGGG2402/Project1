package Slack;

import Log.Logs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.slack.api.methods.MethodsClient;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class User {
    private final String id;
    private final String name;
    private final String realName;
    private final String email;
    private final String updated;
    private int numChannels;
    private final boolean isActive;
    private final JsonArray channelsId = new JsonArray();
    private final JsonArray roles = new JsonArray();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRealName() {
        return realName;
    }

    public String getEmail() {
        return email;
    }

    public String getUpdated() {
        return updated;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public boolean isActive() {
        return isActive;
    }

    public JsonArray getChannelsId() {
        return channelsId;
    }

    public JsonArray getRoles() {
        return roles;
    }

    protected User(com.slack.api.model.User user, MethodsClient client){
        this.id = user.getId();
        this.name = user.getName();
        this.realName = user.getRealName();
        this.email = user.getProfile().getEmail();
        this.isActive = !user.isDeleted();

        Instant instant = Instant.ofEpochSecond(user.getUpdated());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        this.updated = formatter.format(instant);

        if(user.isBot())
            roles.add("Bot");
        if(user.isOwner())
            roles.add("Owner");
        if(user.isPrimaryOwner())
            roles.add("Primary Owner");
        if(user.isRestricted())
            roles.add("Restricted");
        if(user.isUltraRestricted())
            roles.add("Ultra Restricted");
        if(user.isAdmin())
            roles.add("Admin");

        try {
            var result = client.usersConversations(r -> r
                    .user(this.id)
            );
            if (result.isOk()){
                result.getChannels().forEach(r -> channelsId.add(r.getId()));
                numChannels = channelsId.size();
                Logs.writeLog("Get list channel of user: " + this.name + " successfully");
            }else
                Logs.writeLog("Get list channel of user: " + this.name + " failed");
        } catch (Exception e) {
            Logs.writeLog("Get list channel of user: " + this.name + " failed");
            e.printStackTrace();
        }


    }

    protected void addChannelId(String channelId){
        channelsId.add(channelId);
        numChannels++;
    }
    protected boolean removeChannelId(String channelId){
        JsonElement element = new Gson().fromJson(channelId, JsonElement.class);
        if (!channelsId.contains(element))
            return false;
        channelsId.remove(element);
        numChannels--;
        return true;
    }

    // API Method
    protected static List<com.slack.api.model.User> listUsers(MethodsClient client){
        try {
            var result = client.usersList(r -> r
                    .limit(1000)
            );
            if (result.isOk()){
                Logs.writeLog("Get list user successfully");
                return result.getMembers();
            }else {
                Logs.writeLog("Get list user failed");
                return null;
            }
        } catch (Exception e) {
            Logs.writeLog("Get list user failed");
            e.printStackTrace();
            return null;
        }
    }
}
