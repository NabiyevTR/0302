package server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleMessageLogger implements MessageLogger {

    private static final int NUMBER_OF_MESSAGES_TO_READ = 100;

    private File logFile;
    private BufferedWriter writer;
    private BufferedReader reader;
    private volatile int numberOfMessagesToRead = NUMBER_OF_MESSAGES_TO_READ;

    public SimpleMessageLogger(String path) throws NullPointerException {
        setFile(path);

        try {
            writer = new BufferedWriter(new FileWriter(logFile, true));
            reader = new BufferedReader(new FileReader(logFile));
        } catch (IOException e) {
            System.out.println("IO error in SimpleMessageLogger.");
        } finally {
            try {
                close();
            } catch (IOException e) {
                System.out.println("Error occurred during closing reader/writer.");
            }
        }
    }

    @Override
    public void setNumberOfMessagesToRead(int numberOfMessagesToRead) {
        this.numberOfMessagesToRead = numberOfMessagesToRead;
    }

    @Override
    public void close() throws IOException {
        if (writer != null) writer.close();
        if (reader != null) reader.close();

    }

    @Override
    public synchronized void setFile(String path) {
        if (path == null) throw new NullPointerException("File name is null.");
        logFile = new File(path);
    }

    @Override
    public synchronized String read() {

        List<String> messageList = new ArrayList<>();

        //read all file
        try {
            while (reader.ready()) {
                messageList.add(reader.readLine());
            }
        } catch (IOException e) {
            System.out.println("Cannot read from file: " + logFile.getName());
        }

        //get last lines
        StringBuilder messages = new StringBuilder();
        int startLine = messageList.size() - numberOfMessagesToRead < 0 ? 0 : messageList.size() - numberOfMessagesToRead;
        for (int i = startLine; i < messageList.size(); i++) {
            messages.append(messageList.get(i));
            if (i != messageList.size() - 1) {
                messages.append("/n");
            }
        }
        return messages.toString();
    }

    @Override
    public synchronized void write(String message) {

        try {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Cannot write to file: " + logFile.getName());
        }

    }
}
