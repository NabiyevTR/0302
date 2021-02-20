package server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SimpleMessageLogger implements MessageLogger {

    private static final int NUMBER_OF_MESSAGES_TO_READ = 100;

    private File logFile;
    private BufferedWriter writer;
    private volatile int numberOfMessagesToRead = NUMBER_OF_MESSAGES_TO_READ;
    boolean isActive = false;

    public SimpleMessageLogger(String path) throws NullPointerException {

        setFile(path);

        try {
            writer = new BufferedWriter(new FileWriter(logFile, true));
            System.out.println("Message logger started.");
            isActive =true;
        } catch (IOException e) {
            System.out.println("IO error in SimpleMessageLogger.");
            try {
                close();
            } catch (IOException ex) {
                System.out.println("Error occurred during closing reader/writer.");
            }
        }
    }
    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setNumberOfMessagesToRead(int numberOfMessagesToRead) {
        this.numberOfMessagesToRead = numberOfMessagesToRead;
    }

    @Override
    public void close() throws IOException {
        if (writer != null) writer.close();
    }

    @Override
    public synchronized void setFile(String path) {
        if (path == null) throw new NullPointerException("File name is null.");
        logFile = new File(path);
        if (!Files.isWritable(logFile.toPath())) {
            System.out.printf("Cannot write to %s. File is already opened by another application", logFile.getName());
            isActive= false;
        }
    }

    @Override
    public synchronized String read() {
        if (!isActive) return null;

        List<String> messageList = new ArrayList<>();

        //read all file
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            while (reader.ready()) {
                messageList.add(reader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("Cannot read from file: %s. An IOexception has occurred.\n", logFile.getName());
        }

        //get last lines
        StringBuilder messages = new StringBuilder();
        int startLine = messageList.size() - numberOfMessagesToRead < 0 ? 0 : messageList.size() - numberOfMessagesToRead;
        for (int i = startLine; i < messageList.size(); i++) {
            messages.append(messageList.get(i));
            messages.append("\n");

        }
        return messages.toString();
    }

    @Override
    public synchronized void write(String message) {
        if (message == null) {
            System.out.printf("Cannot write to file %s. Message is null.\n", logFile.getName());
            return;
        }
        if (!isActive) return;
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
            System.out.printf("Log to file %s: %s.\n", logFile.getName(), message);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("Cannot write to file: %s. An IOexception has occurred.\n", logFile.getName());
        }
    }
}
