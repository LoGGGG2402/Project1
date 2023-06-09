package AirTable;

import Log.Logs;
import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Table {
    private int numChanges;
    private final String id;
    private final String name;
    private final List<Field> fields = new ArrayList<>();
    private final List<Record> records = new ArrayList<>();

    // Constructors
    protected Table(JsonObject table, String baseId, String token) {
        this.id = table.get("id").getAsString();
        this.name = table.get("name").getAsString();
        table.get("fields").getAsJsonArray().forEach(field -> this.fields.add(new Field(field.getAsJsonObject())));

        // Get Records
        String records = Record.listRecords(id, baseId, token);
        if (records == null) {
            Logs.writeLog("Error: Could not get records for table: " + name);
        } else {
            JsonObject recordsJson = new Gson().fromJson(records, JsonObject.class);
            JsonArray listRecords = recordsJson.get("records").getAsJsonArray();
            listRecords.forEach(record -> this.records.add(new Record(record.getAsJsonObject())));
        }
    }

    // Getters
    protected String getName() {
        return this.name;
    }
    protected String getId() {
        return this.id;
    }
    protected int getNumChanges() {
        return numChanges;
    }
    protected int getNumRecords() {
        return records.size();
    }

    // Handle Fields
    protected Field getField(String name) {
        for (Field field : this.fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }
    protected boolean updateField(JsonObject newField, Field field, String baseId, String token) {
        String fieldUpdate = Field.updateField(newField, field.getId(), id, baseId, token);
        if (fieldUpdate == null) {
            Logs.writeLog("Error: Could not update field: " + field.getName() + " in table: " + name);
            return false;
        }
        JsonObject fieldJson = JsonParser.parseString(fieldUpdate).getAsJsonObject();
        fields.remove(field);
        fields.add(new Field(fieldJson));
        Logs.writeLog("Updated field: " + field.getName() + " in table: " + name);
        return true;
    }
    protected boolean addField(JsonObject field, String baseId, String token) {
        String fieldCreate = Field.createField(field, id, baseId, token);
        if (fieldCreate == null) {
            Logs.writeLog("Error: Could not create field: " + field.get("name").getAsString() + " in table: " + name);
            return false;
        }
        JsonObject fieldJson = JsonParser.parseString(fieldCreate).getAsJsonObject();
        fields.add(new Field(fieldJson));
        Logs.writeLog("Created field: " + field.get("name").getAsString() + " in table: " + name);
        return true;
    }

    // Handle Records
    private boolean updateRecord(JsonObject fields, Record record, String baseId, String token) {
        String recordUpdate = Record.updateRecord(fields, record.getId(), id, baseId, token);
        if (recordUpdate == null) {
            Logs.writeLog("Error: Could not update record: " + record.getValOfId() + " in table: " + name);
            return false;
        }
        JsonObject recordJson = JsonParser.parseString(recordUpdate).getAsJsonObject();
        records.remove(record);
        records.add(new Record(recordJson));
        Logs.writeLog("Updated record: " + record.getValOfId() + " in table: " + name);
        return true;
    }
    private boolean addRecord(JsonObject fields, String baseId, String token) {
        String recordCreate = Record.createRecord(fields, id, baseId, token);
        if (recordCreate == null) {
            Logs.writeLog("Error: Could not create record: " + fields.get("Id").getAsString() + " in table: " + name + "has id: " + id + " baseId: " + baseId);
            return false;
        }
        JsonObject recordJson = new Gson().fromJson(recordCreate, JsonObject.class);
        JsonArray listRecords = recordJson.get("records").getAsJsonArray();

        records.clear();
        listRecords.forEach(record -> this.records.add(new Record(record.getAsJsonObject())));

        Logs.writeLog("Created record: " + fields.get("Id").getAsString() + " in table: " + name);
        return true;
    }
    protected Record getRecord(String idFieldVal) {
        for (Record record : this.records) {
            if (record.getValOfId().equals(idFieldVal)) {
                return record;
            }
        }
        return null;
    }
    private boolean pullRecord(JsonObject fields, String baseId, String token) {
        Record oldRecord = getRecord(fields.get("Id").getAsString());
        if (oldRecord == null) {
            if (addRecord(fields, baseId, token)){
                Logs.writeLog("Add record: " + fields.get("Id").getAsString() + " in table: " + name);
                numChanges++;
                return true;
            }
            return false;
        }
        if (oldRecord.equals(fields)) {
            return true;
        }
        if (updateRecord(fields, oldRecord, baseId, token)) {
            Logs.writeLog("Update record: " + fields.get("Id").getAsString() + " in table: " + name);
            numChanges++;
            return true;
        }
        return false;
    }
    protected boolean pullAllRecord(List<JsonObject> fields, String baseId, String token) {
        numChanges = 0;
        for (JsonObject field : fields) {
            if (!pullRecord(field, baseId, token)) {
                Logs.writeLog("Error: Could not pull record: " + field.get("Id").getAsString() + " in table: " + name);
                return false;
            }
        }
        return true;
    }


    // API Methods
    protected static String listTables(String baseId, String token) {
        String url = "https://api.airtable.com/v0/meta/bases/" + baseId + "/tables";
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + token);
            ClassicHttpResponse response = client.execute(get);
            if (response.getCode() != 200) {
                Logs.writeLog("Error: Could not list tables");
                return null;
            }
            Logs.writeLog("Listed tables");
            return EntityUtils.toString(response.getEntity());
        } catch (IOException | ParseException e) {
            Logs.writeLog("Error: Could not list tables due to exception: " + e.getMessage());
            return null;
        }
    }
    protected static String createTable(String name, JsonArray fields, String baseId, String token){
        String url = "https://api.airtable.com/v0/meta/bases/" + baseId + "/tables";
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + token);
            post.setHeader("Content-Type", "application/json");

            JsonObject body = new JsonObject();
            body.addProperty("name", name);
            body.add("fields", fields);

            post.setEntity(new StringEntity(body.toString()));

            ClassicHttpResponse response = client.execute(post);
            if (response.getCode() != 200) {
                Logs.writeLog("Error: Could not create table: " + name);
                return null;
            }
            Logs.writeLog("Created table: " + name);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException | ParseException e) {
            Logs.writeLog("Error: Could not create table: " + name + " with message: " + e.getMessage());
            return null;
        }
    }

    // write to xlsx file
    protected boolean writeTableToXlsx(String path) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(name);

            List<String> headers = new ArrayList<>();
            for (Field field : fields) {
                headers.add(field.getName());
            }

            // write headers
            Row headerRow = sheet.createRow(0);
            for (String header : headers) {
                Cell cell = headerRow.createCell(headers.indexOf(header));
                cell.setCellValue(header);
            }

            // write records
            for (Record record : records) {
                Row row = sheet.createRow(records.indexOf(record) + 1);
                JsonObject fields = record.getFields();
                for (String header : headers) {
                    Cell cell = row.createCell(headers.indexOf(header));
                    if (fields.has(header)) {
                        JsonElement field = fields.get(header);
                        cell.setCellValue(field.toString());
                    }
                }
            }

            // write to file
            FileOutputStream outputStream = new FileOutputStream(path);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
            Logs.writeLog("Wrote table: " + name + " to file: " + path);
            return true;
        } catch (IOException e) {
            Logs.writeLog("Error: Could not write table: " + name + " to file: " + path + " with message: " + e.getMessage());
            return false;
        }
    }

}
