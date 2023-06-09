package AirTable;

import Log.Logs;
import Slack.Channel;
import Slack.User;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AirTable {
    private boolean valid = true;
    private final String token = "pateiU5ObhT6DgGZC.b05a2bd7f9e4cabb3585f6d1950c6607db060aa6cbb4a62973e12b64efdb6dda";
    private final String base = "app0JoYGd35HXtP3S";

    private Table channelTable = null;
    private Table userTable = null;
    private Table taskTable = null;

    public AirTable() {
       String listTables = Table.listTables(base, token);
       if (listTables == null) {
           Logs.writeLog("Error: Could not list tables.");
           return;
       }
       JsonArray tables = JsonParser.parseString(listTables).getAsJsonObject().getAsJsonArray("tables");
       for (JsonElement table : tables) {
            JsonObject tableJson = table.getAsJsonObject();
            String tableName = tableJson.get("name").getAsString();
            switch (tableName) {
               case "Channels" -> channelTable = new Table(tableJson, base, token);
               case "Users" -> userTable = new Table(tableJson, base, token);
               case "Tasks" -> taskTable = new Table(tableJson, base, token);
            }
       }
       channelTable = allFieldsValid(channelTable, "Channels");
       userTable = allFieldsValid(userTable, "Users");
       taskTable = allFieldsValid(taskTable, "Tasks");

       if (channelTable == null || userTable == null || taskTable == null) {
          Logs.writeLog("Error: Could not validate AirTable.");
          valid = false;
       }
    }
    public boolean isValid() {
       return valid;
    }
    private Table allFieldsValid(Table table, String name) {
        try (FileReader fileReader = new FileReader("src/main/java/Log/fields.json")) {

            JsonObject jsonObject = new Gson().fromJson(new JsonReader(fileReader), JsonObject.class);

            JsonArray fields = jsonObject.getAsJsonArray(name);


            if (table == null) {
                String createTable = Table.createTable(name, fields, base, token);
                if (createTable == null) {
                    Logs.writeLog("Error: Could not create table " + name + ".");
                    return null;
                }
                return new Table(JsonParser.parseString(createTable).getAsJsonObject(), base, token);
            }

            for (JsonElement field : fields) {
                JsonObject fieldJson = field.getAsJsonObject();
                String fieldName = fieldJson.get("name").getAsString();
                Field tableField = table.getField(fieldName);

                if (tableField == null){
                    if(!table.addField(fieldJson, base, token)) return null;
                } else
                    if(!tableField.equals(fieldJson))
                        if(!table.updateField(fieldJson, tableField, base, token))
                            return null;
            }
            return table;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean pushChannels(List<Channel> channels) {
        List<JsonObject> fields = new ArrayList<>();
        for (Channel channel: channels){
            JsonObject field = new JsonObject();
            field.addProperty("Id", channel.getId());
            field.addProperty("Name", channel.getName());
            field.addProperty("Topic", channel.getTopic());
            field.addProperty("Purpose", channel.getPurpose());
            field.addProperty("Is Private", channel.isPrivate());
            field.addProperty("Is Archived", channel.isArchive());
            field.addProperty("Creator Id", channel.getCreatorId());
            field.addProperty("Created", channel.getCreated());
            field.addProperty("Num Members", channel.getNumMembers());

            fields.add(field);
        }

        if (!channelTable.pullAllRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push channels to AirTable.");
            return false;
        }
        return true;
    }
    private boolean pushUsers(List<User> users){
        List<JsonObject> fields = new ArrayList<>();
        for (User user: users){
            JsonObject field = new JsonObject();
            field.addProperty("Id", user.getId());
            field.addProperty("Display Name", user.getName());
            field.addProperty("Real Name", user.getRealName());
            field.addProperty("Email", user.getEmail());
            field.addProperty("Num Channels", user.getNumChannels());
            field.addProperty("Is Active", user.isActive());
            field.add("Roles", user.getRoles());
            field.addProperty("Updated", user.getUpdated());

            fields.add(field);
        }
        if (!userTable.pullAllRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push users to AirTable.");
            return false;
        }
        return true;
    }
    public boolean pushData(List<Channel> channels, List<User> users, boolean isManual) {
        if (!pushChannels(channels)) return false;
        if (!pushUsers(users)) return false;

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Is Manual", isManual);
        jsonObject.addProperty("Num of changes", channels.size() + users.size());

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        jsonObject.addProperty("Time", now.format(formatter));

        List<JsonObject> fields = new ArrayList<>();
        fields.add(jsonObject);

        if (!taskTable.pullAllRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push task to AirTable.");
            return false;
        }
        return true;
    }
}
