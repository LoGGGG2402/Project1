package airtable;

import Logs.Logs;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.net.URI;

public class Field {
    private final String id;
    private final String name;
    private final String type;
//    private final JsonObject options;

    protected Field(JsonObject field) {
        this.id = field.get("id").getAsString();
        this.name = field.get("name").getAsString();
        this.type = field.get("type").getAsString();
//        if (field.has("options")) {
//            this.options = field.get("options").getAsJsonObject();
//        } else {
//            this.options = null;
//        }
    }

    protected String getId() {
        return this.id;
    }

    protected String getName() {
        return this.name;
    }
    protected String getType() {
        return this.type;
    }


    // API Methods
    protected static String createField(JsonObject field, String tableId, String baseId, String token) {

        URI uri = URI.create("https://api.airtable.com/v0/meta/bases/" + baseId + "/tables/" + tableId + "/fields");

        try(CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(uri);
            post.setHeader("Authorization", "Bearer " + token);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(field.toString()));

            return client.execute(post, AirTable.responseHandler);

        } catch (IOException e) {
            Logs.writeLog("Error creating field: " + field);
            return null;
        }


    }
    protected static String updateField(JsonObject field, String fieldId, String tableId, String baseId, String token) {

        URI uri = URI.create("https://api.airtable.com/v0/meta/bases/" + baseId + "/tables/" + tableId + "/fields/" + fieldId);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPatch patch = new HttpPatch(uri);
            patch.setHeader("Authorization", "Bearer " + token);
            patch.setHeader("Content-Type", "application/json");
            patch.setEntity(new StringEntity(field.toString()));
            return client.execute(patch, AirTable.responseHandler );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
