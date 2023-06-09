package AirTable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

public class Record {
    private final String id;
    private final JsonObject fields;
    private final String IdFieldVal;

    Record(JsonObject record) {
        this.id = record.get("id").getAsString();
        this.fields = record.get("fields").getAsJsonObject();
        this.IdFieldVal = this.fields.get("Id").getAsString();
    }

    protected String getId() {
        return this.id;
    }

    protected boolean equals(JsonObject record) {
        return this.fields.equals(record.get("fields").getAsJsonObject());
    }

    protected String getIdFieldVal() {
        return this.IdFieldVal;
    }
    // API Methods
    protected static String listRecords(String tableId, String baseId, String token) {
        //curl "https://api.airtable.com/v0/{baseId}/{tableIdOrName}" \
        //-H "Authorization: Bearer YOUR_TOKEN"
        String url = "https://api.airtable.com/v0/" + baseId + "/" + tableId;

        try(CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + token);
            get.setHeader("Content-Type", "application/json");

            ClassicHttpResponse response = client.execute(get);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    protected static String updateRecord(JsonObject fields, String recordId, String tableId, String baseId, String Token){
        String url = "https://api.airtable.com/v0/" + baseId + "/" + tableId + "/" + recordId;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPatch patch = new HttpPatch(url);
            patch.setHeader("Authorization", "Bearer " + Token);
            patch.setHeader("Content-Type", "application/json");

            JsonObject fullBody = new JsonObject();
            fullBody.add("fields", fields);
            patch.setEntity(new StringEntity(fullBody.toString()));

            ClassicHttpResponse response = client.execute(patch);

            if (response.getCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            } else {
                return null;
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    protected static String createRecord(JsonObject fields, String tableId, String baseId, String Token){
        String url = "https://api.airtable.com/v0/" + baseId + "/" + tableId;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPatch patch = new HttpPatch(url);
            patch.setHeader("Authorization", "Bearer " + Token);
            patch.setHeader("Content-Type", "application/json");

            JsonObject body = new JsonObject();
            body.add("fields", fields);

            JsonArray records = new JsonArray();
            records.add(body);

            JsonObject fullBody = new JsonObject();
            fullBody.add("records", records);

            patch.setEntity(new StringEntity(fullBody.toString()));

            ClassicHttpResponse response = client.execute(patch);

            if (response.getCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            } else {
                return null;
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
