package Slack;

import Log.Logs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    protected void removeChannelId(String channelId){
        JsonElement element = new Gson().fromJson(channelId, JsonElement.class);
        if (channelsId.contains(element)){
            channelsId.remove(element);
            numChannels--;
        }
    }
    public JsonObject toJson(){
        JsonObject json = new JsonObject();
        json.addProperty("Id", id);
        json.addProperty("Display Name", name);
        json.addProperty("Real Name", realName);
        json.addProperty("Email", email);
        json.addProperty("Updated", updated);
        json.addProperty("Num Channels", numChannels);
        json.addProperty("Is Active", isActive);
        json.addProperty("Updated", updated);
        json.add("Role", roles);
        json.add("Channels Id", channelsId);


        return json;
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
