package AirTable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.slack.api.model.Conversation;
import com.slack.api.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static AirTable.Table.listTable;

public class Airtable {
    private static final String token = "pateiU5ObhT6DgGZC.b05a2bd7f9e4cabb3585f6d1950c6607db060aa6cbb4a62973e12b64efdb6dda";
    private static final String base = "app0JoYGd35HXtP3S";

    public static final Map<String, String> channelsField = Map.of(
            "Name", "singleLineText",
            "Creator", "singleLineText",
            "Created", "number",
            "Num of Members", "number",
            "Enterprise Id", "singleLineText"
    );

    public static final Map<String, String> usersField = Map.of(
            "Name", "singleLineText",
            "Real Name", "singleLineText",
            "Email", "email"
    );
    public Table channelS = null;
    public Table userS = null;
    public Airtable() {
        String listTable = listTable(base, token);
        JsonObject jsonObject = new Gson().fromJson(listTable, JsonObject.class);
        JsonArray tables = jsonObject.getAsJsonArray("tables");
        for (AtomicInteger i = new AtomicInteger(); i.get() < tables.size(); i.getAndIncrement()) {
            JsonObject table = tables.get(i.get()).getAsJsonObject();
            String id = table.get("id").getAsString();
            String name = table.get("name").getAsString();
            String primaryFieldId = table.get("primaryFieldId").getAsString();

            Table t = new Table(id, name, primaryFieldId);

            JsonArray fields = table.getAsJsonArray("fields");

            for (AtomicInteger j = new AtomicInteger(); j.get() < fields.size(); j.getAndIncrement()) {
                JsonObject field = fields.get(j.get()).getAsJsonObject();
                String fieldId = field.get("id").getAsString();
                String fieldName = field.get("name").getAsString();
                String fieldType = field.get("type").getAsString();
                Field f;
                if (field.get("choices") == null) {
                    f = new Field(fieldId, fieldName, fieldType);
                }else {
                    f = new Field(fieldId, fieldName, fieldType, field.get("choices").getAsString());
                }
                t.addField(f);
            }

            if (name.equals("Channels"))
                channelS = t;

            if (name.equals("Users"))
                userS = t;
        }

        if (channelS == null){
            String response = Table.addTable("Channels", base, token);
            JsonObject jsonObject1 = new Gson().fromJson(response, JsonObject.class);
            String id = jsonObject1.get("id").getAsString();
            String name = jsonObject1.get("name").getAsString();
            String primaryFieldId = jsonObject1.get("primaryFieldId").getAsString();

            JsonObject field = jsonObject1.getAsJsonArray("fields").get(0).getAsJsonObject();

            Field f = new Field(field.get("id").getAsString(), field.get("name").getAsString(), field.get("type").getAsString());
            channelS = new Table(id, name, primaryFieldId);
            channelS.addField(f);
        }

        if (userS == null) {
            String response = Table.addTable("Users", base, token);
            JsonObject jsonObject1 = new Gson().fromJson(response, JsonObject.class);
            String id = jsonObject1.get("id").getAsString();
            String name = jsonObject1.get("name").getAsString();
            String primaryFieldId = jsonObject1.get("primaryFieldId").getAsString();

            JsonObject field = jsonObject1.getAsJsonArray("fields").get(0).getAsJsonObject();

            Field f = new Field(field.get("id").getAsString(), field.get("name").getAsString(), field.get("type").getAsString());
            userS = new Table(id, name, primaryFieldId);
            userS.addField(f);
        }

        for (Map.Entry<String, String> entry : channelsField.entrySet()) {
            String name = entry.getKey();
            String type = entry.getValue();
            String options = null;
            if (type.equals("number")) options = """
                    {
                          "precision": 0
                        }
                    """;
            List<Field> fields = channelS.getFields();
            AtomicBoolean found = new AtomicBoolean(false);
            for (Field field : fields) {
                if (field.getName().equals(name)) {
                    found.set(true);
                    break;
                }
            }
            if (!found.get()) Field.addField(channelS.getId(), name, type, options, base, token);
        }

        for (Map.Entry<String, String> entry : usersField.entrySet()) {
            String name = entry.getKey();
            String type = entry.getValue();
            List<Field> fields = userS.getFields();
            AtomicBoolean found = new AtomicBoolean(false);
            for (Field field : fields) {
                if (field.getName().equals(name)) {
                    found.set(true);
                    break;
                }
            }
            if (!found.get()) Field.addField(userS.getId(), name, type, null, base, token);
        }


    }
    public void syncChannelRecord(List<Conversation> channels) {
        for (Conversation channel : channels) {
            JsonObject record = new JsonObject();
            record.addProperty("Id", channel.getId());
            record.addProperty("Name", channel.getName());
            record.addProperty("Creator", channel.getCreator());
            record.addProperty("Created", channel.getCreated());
            record.addProperty("Num of Members", channel.getNumOfMembers());
            record.addProperty("Enterprise Id", channel.getEnterpriseId());

            JsonObject field = new JsonObject();
            field.add("fields", record);

            JsonArray j = new JsonArray();
            j.add(field);

            JsonObject jsonObject = new JsonObject();
            jsonObject.add("records", j);

            System.out.println(jsonObject);
            Record.addRecord(channelS, jsonObject.toString(), token, base);

        }
    }
    public void syncUserRecord(List<User> users) {
        for (User user : users) {
            JsonObject record = new JsonObject();
            record.addProperty("Id", user.getId());
            record.addProperty("Name", user.getName());
            record.addProperty("Real Name", user.getRealName());
            record.addProperty("Email", user.getProfile().getEmail());

            JsonObject field = new JsonObject();
            field.add("fields", record);

            JsonArray j = new JsonArray();
            j.add(field);

            JsonObject jsonObject = new JsonObject();
            jsonObject.add("records", j);

            System.out.println(jsonObject);
            Record.addRecord(userS, jsonObject.toString(), token, base);
        }
    }
    public static String readResponse(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = in.lines().collect(Collectors.joining());
        in.close();
        return response;
    }


}
