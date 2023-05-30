package AirTable;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Table {
    private final List<Field> fields = new ArrayList<>();
    private final String id;
    private final String name;
    private final String primaryFieldId;
    public void addField(Field field){
        fields.add(field);
    }
    public List<Field> getFields() {
        return fields;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrimaryFieldId() {
        return primaryFieldId;
    }

    public Table(String id, String name, String primaryFieldId) {
        this.id = id;
        this.name = name;
        this.primaryFieldId = primaryFieldId;
    }

    public static String addTable(String name, String base, String token){
        String url = "https://api.airtable.com/v0/meta/bases/" + base + "/tables";

        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes("""
                    {
                        "fields": [
                          {
                            "name": "Id",
                            "type": "singleLineText"
                          }
                        ],
                        "name": "%s"
                      }""".formatted(name));
            wr.flush();
            wr.close();

            System.out.println("Create Table: " + name + " Request Code: " + connection.getResponseCode());

            String response = Airtable.readResponse(connection);
            connection.disconnect();

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String listTable(String base, String token){
        String url = "https://api.airtable.com/v0/meta/bases/" + base + "/tables";

        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();

            connection.setRequestMethod("GET");

            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setDoOutput(true);

            System.out.println("Request Code: " + connection.getResponseCode());

            var response = connection.getInputStream().readAllBytes();
            connection.disconnect();

            return new String(response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
