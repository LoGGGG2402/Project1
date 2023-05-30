package AirTable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

public class Field {
    String id;
    String name;
    String type;
    String choices;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getChoices() {
        return choices;
    }

    public Field(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Field(String id, String name, String type, String choices) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.choices = choices;
    }

    public static String addField(String tableId, String name, String type, String options, String base, String token){
        String url = "https://api.airtable.com/v0/meta/bases/" + base + "/tables/" + tableId + "/fields";

        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();

            connection.setRequestMethod("POST");

            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            if (options == null) {
                wr.writeBytes("""
                        {
                            "name": "%s",
                            "type": "%s"
                        }""".formatted(name, type));

                System.out.printf("""
                        {
                            "name": "%s",
                            "type": "%s"
                        }%n""", name, type);
            } else {
                wr.writeBytes("""
                        {
                            "name": "%s",
                            "type": "%s",
                            "options": %s
                        }""".formatted(name, type, options));
                System.out.printf("""
                        {
                            "name": "%s",
                            "type": "%s",
                            "options": %s
                        }%n""", name, type, options);
            }

            wr.flush();
            wr.close();

            System.out.println("Add field " + name + " to table " + tableId + " with type " + type + " has response code " + connection.getResponseCode());

            String response = Airtable.readResponse(connection);
            connection.disconnect();

            return response;
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

    }
}
