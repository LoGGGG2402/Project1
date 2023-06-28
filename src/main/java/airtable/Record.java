package airtable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.List;

public class Record{
    private final String id;
    private final JsonObject fields;
    private final String IdFieldVal;

    // Constructor
    Record(JsonObject record) {
        this.id = record.get("id").getAsString();
        this.fields = record.get("fields").getAsJsonObject();
        if (this.fields.has("Id")){
            this.IdFieldVal = this.fields.get("Id").getAsString();
//            System.out.println("Record :" + this.id + " has Id: " + this.IdFieldVal);
        }

        else
            this.IdFieldVal = null;
    }

    protected boolean equals(JsonObject fields, List<Field> fieldsList) {
        for (Field field : fieldsList) {
            if (fields.has(field.getName())) {
                String newVal = fields.get(field.getName()).toString();
                if (newVal.equals("false") || newVal.equals("null") || newVal.equals("[]"))
                    continue;
                if (!this.fields.has(field.getName()))
                {
                    return false;
                }
                String oldVal = this.fields.get(field.getName()).toString();
                if (field.getType().contains("date"))
                    if(oldVal.replaceAll(".000Z", "Z").equals(newVal))
                        continue;
                if (!newVal.equals(oldVal))
                {
                    return false;
                }
            }
        }
        return true;
    }

    // Getters
    protected String getRecordId() {
        return this.id;
    }
    protected String getId() {
        return this.IdFieldVal;
    }
    protected JsonObject getFields() {
        return this.fields;
    }


    // API Methods
    protected static String listRecords(String tableId, String offset, String baseId, String token) {
        //curl "https://api.airtable.com/v0/{baseId}/{tableIdOrName}" \
        //-H "Authorization: Bearer YOUR_TOKEN"
        String url = "https://api.airtable.com/v0/" + baseId + "/" + tableId + "?maxRecords=100000";
        if (offset != null)
            url += "&offset=" + offset;

        try(CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + token);
            get.setHeader("Content-Type", "application/json");

            return client.execute(get, AirTable.responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    protected static boolean dropRecord(String recordId, String tableId, String baseId, String Token){
        String url = "https://api.airtable.com/v0/" + baseId + "/" + tableId + "/" + recordId;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpDelete delete = new HttpDelete(url);
            delete.setHeader("Authorization", "Bearer " + Token);

            String response = client.execute(delete, AirTable.responseHandler);

            JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);
            return responseJson.get("deleted").getAsBoolean();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    protected static String updateMultipleRecords(JsonObject records, String tableId, String baseId, String Token){
        String url = "https://api.airtable.com/v0/" + baseId + "/" + tableId;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPatch patch = new HttpPatch(url);
            patch.setHeader("Authorization", "Bearer " + Token);
            patch.setHeader("Content-Type", "application/json");

            patch.setEntity(new StringEntity(records.toString()));

            return client.execute(patch, AirTable.responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    protected static String addMultipleRecords(JsonObject records, String tableId, String baseId, String Token){
        String url = "https://api.airtable.com/v0/" + baseId + "/" + tableId;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + Token);
            post.setHeader("Content-Type", "application/json");

            post.setEntity(new StringEntity(records.toString()));

            return client.execute(post, AirTable.responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}