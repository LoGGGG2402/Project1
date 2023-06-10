package AirTable;

import Log.Logs;
import Slack.Channel;
import Slack.User;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
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
          Logs.writeLog("Error: Could not validate AirTable. because of missing fields.");
          System.out.println(channelTable == null);
          System.out.println(userTable == null);
          System.out.println(taskTable == null);
          valid = false;
       }

       Field linkField = channelTable.getField("Users");
       if (linkField == null) {
           JsonObject newField = new JsonObject();
           newField.addProperty("name", "Users");
           newField.addProperty("type", "multipleRecordLinks");

           JsonObject options = new JsonObject();
           options.addProperty("linkedTableId", userTable.getId());
           newField.add("options", options);
           if (!channelTable.addField(newField, base, token)) {
               Logs.writeLog("Error: Could not add field Users to Channels table.");
               valid = false;
           }
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
                }
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
            JsonObject field = channel.toJson();

            JsonArray MembersId = field.getAsJsonArray("Members Id");
            JsonArray MembersRecordId = new JsonArray();
            for (JsonElement member : MembersId) {
                String memberId = member.getAsString();
                Record record = userTable.getRecord(memberId);
                if (record == null) {
                    Logs.writeLog("Error: Could not find user with id " + memberId + ".");
                    return false;
                }
                MembersRecordId.add(record.getId());
            }
            field.add("Users", MembersRecordId);
            field.remove("Members Id");

            fields.add(field);
        }

        if (!channelTable.pullAllRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push channels to AirTable.");
            return false;
        }
        channelTable.dropRecord(fields, base, token);
        return true;
    }
    private boolean pushUsers(List<User> users){
        List<JsonObject> fields = new ArrayList<>();
        for (User user: users){
            JsonObject field = user.toJson();
            field.remove("Channels Id");

            fields.add(field);
        }
        if (!userTable.pullAllRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push users to AirTable.");
            return false;
        }
        userTable.dropRecord(fields, base, token);
        return true;
    }
    public boolean pushData(List<Channel> channels, List<User> users, boolean isManual) {
        if (!pushUsers(users)) return false;
        userTable.syncRecord(base, token);
        if (!pushChannels(channels)) return false;
        channelTable.syncRecord(base, token);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Id", taskTable.getNumRecords()+1);
        jsonObject.addProperty("Is Manual", isManual);
        jsonObject.addProperty("Num of changes", channelTable.getNumChanges()+userTable.getNumChanges());

        long time = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(time);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;


        jsonObject.addProperty("Update Time", formatter.format(instant));

        List<JsonObject> fields = new ArrayList<>();
        fields.add(jsonObject);

        if (!taskTable.pullAllRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push task to AirTable.");
            return false;
        }
        channelTable.syncRecord(base, token);
        return true;
    }
    public void tableToXlsx() {
        channelTable.writeTableToXlsx("src/main/java/Log/channels.xlsx");
        userTable.writeTableToXlsx("src/main/java/Log/users.xlsx");
        taskTable.writeTableToXlsx("src/main/java/Log/tasks.xlsx");
    }
}
