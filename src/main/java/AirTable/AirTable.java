package AirTable;

import Logs.Logs;
import Slack.Channel;
import Slack.SlackUser;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AirTable {
    private boolean isActive = true;
    private final String token = "patogTEA7FvHGIl6u.349a94a6d90f7677d2a982c8d1d1b1f23559a96400fd5e378af806afa6c6f4a2";
    private final String base = "app0JoYGd35HXtP3S";

    private Table channelTable = null;
    private Table userTable = null;
    private Table taskTable = null;

    public AirTable() {
        long time = System.currentTimeMillis();
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
               case "Channels" -> channelTable = new Table(tableJson);
               case "Users" -> userTable = new Table(tableJson);
               case "Tasks" -> taskTable = new Table(tableJson);
            }
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            executor.submit(() -> {
               channelTable = validTable(channelTable, "Channels");
               if (channelTable == null) {
                   isActive = false;
                   return;
               }
               channelTable.syncRecord(base, token);
            });
            executor.submit(() -> {
            userTable = validTable(userTable, "Users");
            if (userTable == null) {
                 isActive = false;
                 return;
            }
            userTable.syncRecord(base, token);
            });
            executor.submit(() -> {
                taskTable = validTable(taskTable, "Tasks");
                if (taskTable == null) {
                     isActive = false;
                     return;
                }
                taskTable.syncRecord(base, token);
            });

            executor.shutdown();
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
               isActive = false;
           }
        }

        long time2 = System.currentTimeMillis();
        Logs.writeLog("AirTable initialized in " + (time2 - time) + "ms.");
    }
    public boolean isActive() {
       return isActive;
    }
    private Table validTable(Table table, String name) {
        try (FileReader fileReader = new FileReader("src/main/java/data/fields.json")) {

            JsonObject jsonObject = new Gson().fromJson(new JsonReader(fileReader), JsonObject.class);

            JsonArray fields = jsonObject.getAsJsonArray(name);


            if (table == null) {
                String createTable = Table.createTable(name, fields, base, token);
                if (createTable == null) {
                    Logs.writeLog("Error: Could not create table " + name + ".");
                    return null;
                }
                return new Table(JsonParser.parseString(createTable).getAsJsonObject());
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
                MembersRecordId.add(record.getRecordId());
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
    private boolean pushUsers(List<SlackUser> users){
        List<JsonObject> fields = new ArrayList<>();
        for (SlackUser user: users){
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
    public boolean pushData(List<Channel> channels, List<SlackUser> users, boolean isManual) {
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
    public void exportToXlsx(String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Logs.writeLog("Error: Could not create directory " + path + ".");
                return;
            }
        }
        channelTable.writeTableToXlsx(path + "/channels.xlsx");
        userTable.writeTableToXlsx(path + "/users.xlsx");
        taskTable.writeTableToXlsx(path + "/tasks.xlsx");
    }
}
