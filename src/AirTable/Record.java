package AirTable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

public class Record {
    public static String addRecord(Table table, String record, String token, String base) {
        String url = "https://api.airtable.com/v0/" + base + "/" + table.getId();
        try {
            var connection = (HttpURLConnection) new URI(url).toURL().openConnection();

            connection.setRequestMethod("POST");

            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(record);
            wr.flush();
            wr.close();

            System.out.println("wrote record to table " + table.getName() + " with response code " + connection.getResponseCode());

            String response = Airtable.readResponse(connection);
            connection.disconnect();

            return response;

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
