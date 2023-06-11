package Main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;

public class test {
    public static void main(String[] args) {
        try (FileReader fileReader = new FileReader("src/main/java/Log/fields.json")) {

            JsonObject jsonObject = new Gson().fromJson(new JsonReader(fileReader), JsonObject.class);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(jsonObject);
            System.out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
