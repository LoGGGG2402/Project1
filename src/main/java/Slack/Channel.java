package Slack;

import Log.Logs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.Conversation;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Channel {
    private final String id;
    private final String name;
    private final String creatorId;
    private final String created;
    private final String topic;
    private final String purpose;
    private int numMembers;
    private final boolean isPrivate;
    private final boolean isArchive;

    JsonArray membersId = new JsonArray();
    public Channel(Conversation conversation, MethodsClient client){
        this.id = conversation.getId();
        this.name = conversation.getName();
        this.topic = conversation.getTopic().getValue();
        this.purpose = conversation.getPurpose().getValue();
        this.creatorId = conversation.getCreator();
        this.isArchive = conversation.isArchived();
        this.isPrivate = conversation.isPrivate();

        Instant instant = Instant.ofEpochSecond(conversation.getCreated());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        this.created = formatter.format(instant);


        try {
            var result = client.conversationsMembers(r -> r
                    .channel(this.id)
            );
            if (result.isOk()){
                result.getMembers().forEach(r -> membersId.add(r));
                numMembers = membersId.size();
                Logs.writeLog("Get list user of channel: " + this.name);
            }else {
                Logs.writeLog("Get list user of channel: " + this.name + " failed");
            }
        } catch (SlackApiException | IOException e) {
            Logs.writeLog("Get list user of channel: " + this.name + " failed");
            e.printStackTrace();
        }
    }

    public JsonArray getMembersId() {
        return membersId;
    }
    public JsonObject toJson(){
        JsonObject json = new JsonObject();
        json.addProperty("Id", this.id);
        json.addProperty("Name", this.name);
        json.addProperty("Topic", this.topic);
        json.addProperty("Purpose", this.purpose);
        json.addProperty("Is Private", this.isPrivate);
        json.addProperty("Is Archived", this.isArchive);
        json.addProperty("Creator Id", this.creatorId);
        json.addProperty("Created", this.created);
        json.addProperty("Num Members", this.numMembers);
        json.add("Members Id", this.membersId);
        return json;
    }

    public String getId() {
        return id;
    }


    // API method
    protected boolean removeUser(String userId, MethodsClient client){
        try {
            var result = client.conversationsKick(r -> r
                    .channel(this.id)
                    .user(userId)
            );
            if (result.isOk()){
                Logs.writeLog("Remove user: " + userId + " from channel: " + this.name);
                membersId.remove(new Gson().fromJson(userId, JsonElement.class));
                return true;
            }else {
                Logs.writeLog("Remove user: " + userId + " from channel: " + this.name + " failed");
                return false;
            }
        } catch (SlackApiException | IOException e) {
            Logs.writeLog("Remove user: " + userId + " from channel: " + this.name + " failed");
            e.printStackTrace();
            return false;
        }
    }
    protected boolean addUser(String userId, MethodsClient client){
        try {
            var result = client.conversationsInvite(r -> r
                    .channel(this.id)
                    .users(List.of(userId))
            );
            if (result.isOk()){
                Logs.writeLog("Add user: " + userId + " to channel: " + this.name);
                membersId.add(new Gson().fromJson(userId, JsonElement.class));
                return true;
            }else {
                Logs.writeLog("Add user: " + userId + " to channel: " + this.name + " failed");
                return false;
            }
        } catch (SlackApiException | IOException e) {
            Logs.writeLog("Add user: " + userId + " to channel: " + this.name + " failed");
            e.printStackTrace();
            return false;
        }
    }
    protected static List<Conversation> listChannels(MethodsClient client){
        try {
            var result = client.conversationsList(r -> r
                    .limit(1000)
            );
            if (result.isOk()){
                Logs.writeLog("Get list channel success");
                return result.getChannels();
            }else {
                Logs.writeLog("Get list channel failed");
                return null;
            }
        } catch (SlackApiException | IOException e) {
            Logs.writeLog("Get list channel failed");
            e.printStackTrace();
            return null;
        }
    }
    protected static Conversation createChannel(String name, MethodsClient client){
        try {
            var result = client.conversationsCreate(r -> r
                    .name(name)
            );
            if (result.isOk()){
                Logs.writeLog("Create channel: " + name + " success");
                return result.getChannel();
            }else {
                Logs.writeLog("Create channel: " + name + " failed");
                return null;
            }
        } catch (SlackApiException | IOException e) {
            Logs.writeLog("Create channel: " + name + " failed");
            e.printStackTrace();
            return null;
        }
    }


}
