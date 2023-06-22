package AirTable;

import Logs.Logs;
import Slack.Channel;
import Slack.SlackUser;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.File;
import java.io.FileNotFoundException;
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
    private String token;
    private String base;
    static final HttpClientResponseHandler<String> responseHandler = httpResponse -> {
        int status = httpResponse.getCode();
        if (status != 200) {
            return null;
        }

        try {
            return EntityUtils.toString(httpResponse.getEntity());
        } catch (ParseException e) {
            return null;
        }
    };


    private Table channelTable = null;
    private Table userTable = null;
    private Table taskTable = null;

    public AirTable() {
        try {
            FileReader fileReader = new FileReader("src/main/resources/data/Info.json");
            JsonObject jsonObject = new Gson().fromJson(new JsonReader(fileReader), JsonObject.class);
            this.token = jsonObject.get("airtable").getAsString();
            this.base = jsonObject.get("base").getAsString();
        } catch (FileNotFoundException e) {
            Logs.writeLog("Error: Could not find Info.json");
            this.isActive = false;
            return;
        }

        long time = System.currentTimeMillis();
        String listTables = Table.listTables(base, token);
        if (listTables == null) {
            this.isActive = false;
            return;
        }else {
            Logs.writeLog("List tables: " + (System.currentTimeMillis() - time));
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
           if (channelTable.addField(newField, base, token)) {
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
        try (FileReader fileReader = new FileReader("src/main/resources/data/fields.json")) {

            JsonObject jsonObject = new Gson().fromJson(new JsonReader(fileReader), JsonObject.class);

            JsonArray fields = jsonObject.getAsJsonArray(name);


            if (table == null) {
                String createTable = Table.createTable(name, fields, base, token);
                if (createTable == null) {
                    return null;
                }
                Logs.writeLog("Created table " + name + ".");
                return new Table(JsonParser.parseString(createTable).getAsJsonObject());
            }

            for (JsonElement field : fields) {
                JsonObject fieldJson = field.getAsJsonObject();
                String fieldName = fieldJson.get("name").getAsString();
                Field tableField = table.getField(fieldName);

                if (tableField == null){
                    System.out.println("Adding field " + fieldName + " to table " + name + ".");
                    if(table.addField(fieldJson, base, token)) return null;
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
        if (channelTable.pullMultipleRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push channels to AirTable.");
            return false;
        }
        return true;
    }
    private boolean pushUsers(List<SlackUser> users){
        List<JsonObject> fields = new ArrayList<>();
        for (SlackUser user: users){
            JsonObject field = user.toJson();
            field.remove("Channels Id");

            fields.add(field);
        }
        if (userTable.pullMultipleRecord(fields, base, token)) {
            Logs.writeLog("Error: Could not push users to AirTable.");
            return false;
        }
        return true;
    }
    public boolean pushData(List<Channel> channels, List<SlackUser> users, boolean isManual) {
        if (!pushUsers(users)) return false;
        if (!pushChannels(channels)) return false;


        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Id", taskTable.getNumRecords()+1);
        jsonObject.addProperty("Is Manual", isManual);
        jsonObject.addProperty("Num of changes", channelTable.getNumChanges()+userTable.getNumChanges());

        long time = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(time);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;


        jsonObject.addProperty("Update Time", formatter.format(instant));


        if (!taskTable.addMultipleRecords(List.of(jsonObject), base, token)) {
            Logs.writeLog("Error: Could not push task to AirTable.");
            return false;
        }
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
