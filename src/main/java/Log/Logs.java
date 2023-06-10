package Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logs {
    private static final String path = "src/main/java/Log/Logs.txt";
    private static final File file = new File(path);

    public static void writeLog(String message) {
        try {
            if (!file.exists())
                if (file.createNewFile())
                    System.out.println("Created new log file.");
                else
                    System.out.println("Could not create new log file.");

            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write(message + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        writeLog("Test");
    }
}
