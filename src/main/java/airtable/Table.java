package airtable;

import Logs.Logs;
import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Table {
    private int numChanges;
    private final String id;
    private final String name;
    private final List<Field> fields = new ArrayList<>();
    private final List<Record> records = new ArrayList<>();

    // Constructors
    protected Table(JsonObject table) {
        this.id = table.get("id").getAsString();
        this.name = table.get("name").getAsString();
        table.get("fields").getAsJsonArray().forEach(field -> this.fields.add(new Field(field.getAsJsonObject())));
    }

    protected void syncRecord(String baseId, String token) {
        records.clear();
        String offset = null;
        while (true){
            String response = Record.listRecords(id, offset, baseId, token);
            if (response == null) {
                Logs.writeLog("Error: Could not get records for table: " + name);
            } else {
                JsonObject recordsJson = new Gson().fromJson(response, JsonObject.class);
                JsonArray listRecords = recordsJson.get("records").getAsJsonArray();
                listRecords.forEach(record -> this.records.add(new Record(record.getAsJsonObject())));
                if (recordsJson.has("offset"))
                    offset = recordsJson.get("offset").getAsString();
                else
                    break;
            }
        }
        Logs.writeLog("Info: Synced " + records.size() + " records for table: " + name);
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
    protected Record getRecord(String Id) {
        for (Record record : this.records) {
            if (record.getId().equals(Id)) {
                return record;
            }
        }
        return null;
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
    @Deprecated // api not allow to update field type
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
            return true;
        }
        JsonObject fieldJson = JsonParser.parseString(fieldCreate).getAsJsonObject();
        fields.add(new Field(fieldJson));
        Logs.writeLog("Created field: " + field.get("name").getAsString() + " in table: " + name);
        return false;
    }


    // Handle Records
    private boolean updateMultipleRecords(Map<Record, JsonObject> listUpdate, String baseId, String token) {
        if (listUpdate.isEmpty()) {
            return true;
        }
        JsonArray records = new JsonArray();
        List<Record> recordsUpdate = new ArrayList<>();
        for (Record record : listUpdate.keySet()) {
            JsonObject fields = listUpdate.get(record);
            JsonObject recordJson = new JsonObject();
            recordJson.addProperty("id", record.getRecordId());
            recordJson.add("fields", fields);
            records.add(recordJson);
            recordsUpdate.add(record);

            if (records.size() >= 10 || records.size() == listUpdate.size()) {
                JsonObject body = new JsonObject();
                body.add("records", records);

                String response = Record.updateMultipleRecords(body, id, baseId, token);
                if (response == null) {
                    Logs.writeLog("Error: Could not update multiple records in table: " + name);
                    return false;
                }
                Logs.writeLog("Updated " + records.size() + " records in table: " + name);
                for (Record recordUpdate : recordsUpdate) {
                    this.records.remove(recordUpdate);
                }
                JsonArray recordsResponse = JsonParser.parseString(response).getAsJsonObject().get("records").getAsJsonArray();
                for (JsonElement recordResponse : recordsResponse) {
                    this.records.add(new Record(recordResponse.getAsJsonObject()));
                }
                records = new JsonArray();
                recordsUpdate = new ArrayList<>();
            }
        }
        return true;
    }
    protected boolean addMultipleRecords(List<JsonObject> listAdd, String baseId, String token) {
        if (listAdd.isEmpty()) {
            return true;
        }

        JsonArray records = new JsonArray();
        for (JsonObject fields : listAdd) {
            JsonObject recordJson = new JsonObject();
            recordJson.add("fields", fields);
            records.add(recordJson);

            if (records.size() >= 10 || records.size() == listAdd.size()) {
                JsonObject body = new JsonObject();
                body.add("records", records);

                String response = Record.addMultipleRecords(body, id, baseId, token);
                if (response == null) {
                    Logs.writeLog("Error: Could not add multiple records in table: " + name);
                    return false;
                }
                Logs.writeLog("Added " + records.size() + " records in table: " + name);
                JsonArray recordsResponse = JsonParser.parseString(response).getAsJsonObject().get("records").getAsJsonArray();
                for (JsonElement record : recordsResponse) {
                    this.records.add(new Record(record.getAsJsonObject()));
                }
                records = new JsonArray();
            }
        }
        return true;
    }
    private boolean deleteMultipleRecords(List<Record> listDelete, String baseId, String token) {
        if (listDelete.isEmpty()) {
            return true;
        }
        try (ExecutorService executorService = Executors.newFixedThreadPool(listDelete.size())) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Record record : listDelete) {
                futures.add(executorService.submit(() -> {
                    if (!Record.dropRecord(record.getRecordId(), id, baseId, token)) {
                        records.remove(record);
                        Logs.writeLog("Error: Could not delete record: " + record.getRecordId() + " in table: " + name);
                        return false;
                    }
                    Logs.writeLog("Deleted record: " + record.getRecordId() + " in table: " + name);
                    return true;
                }));
            }
            for (Future<Boolean> future : futures) {
                if (!future.get()) {
                    return false;
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    protected boolean pullMultipleRecord(List<JsonObject> fields, String baseId, String token) {
        List<Record> listDelete = new ArrayList<>(records);

        List<JsonObject> listAdd = new ArrayList<>(fields);
        Map<Record, JsonObject> listUpdate = new HashMap<>();

        for (JsonObject field : fields) {
            String id = field.get("Id").getAsString();
            Record record = getRecord(id);
            if (record != null) {
                listDelete.remove(record);
                listAdd.remove(field);
                if(!record.equals(field, this.fields)){
                    listUpdate.put(record, field);
                }
            }
        }

        numChanges = listDelete.size() + listAdd.size() + listUpdate.size();
        try(ExecutorService executorService = Executors.newFixedThreadPool(3)){
            List<Future<Boolean>> futures = executorService.invokeAll(Arrays.asList(
                    () -> deleteMultipleRecords(listDelete, baseId, token),
                    () -> addMultipleRecords(listAdd, baseId, token),
                    () -> updateMultipleRecords(listUpdate, baseId, token)
            ));
            for (Future<Boolean> future : futures) {
                if (!future.get()) {
                    return true;
                }
            }
            return false;
        } catch (InterruptedException | ExecutionException e) {
            Logs.writeLog("Error: Could not pull multiple records in table: " + name + " with message: " + e.getMessage());
            return true;
        }
    }

    // API Methods
    protected static String listTables(String baseId, String token) {
        String url = "https://api.airtable.com/v0/meta/bases/" + baseId + "/tables";
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + token);
            return client.execute(get, AirTable.responseHandler);

        } catch (IOException e) {
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

            return client.execute(post, AirTable.responseHandler);
        } catch (IOException e) {
            Logs.writeLog("Error: Could not create table: " + name + " with message: " + e.getMessage());
            return null;
        }
    }

    // write to xlsx file
    protected void writeTableToXlsx(String path) {
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
        } catch (IOException e) {
            Logs.writeLog("Error: Could not write table: " + name + " to file: " + path + " with message: " + e.getMessage());
        }
    }
}
