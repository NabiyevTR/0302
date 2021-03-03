package server.messagelogger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import server.Server;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SimpleMessageLogger implements MessageLogger {

    private final Logger logger = LogManager.getLogger(Server.class);

    private static final int NUMBER_OF_MESSAGES_TO_READ = 100;

    private File logFile;
    private BufferedWriter writer;
    private volatile int numberOfMessagesToRead = NUMBER_OF_MESSAGES_TO_READ;
    boolean isActive = false;

    public SimpleMessageLogger(String path) throws NullPointerException {
        init(path);
    }

    public void init(String path) {

        try {
            setFile(path);
        } catch (NullPointerException e) {
            logger.error("File name is null.", e);
            isActive = false;
        }

        try {
            writer = new BufferedWriter(new FileWriter(logFile, true));
            logger.info("Message logger started.");
            isActive = true;
        } catch (IOException e) {
            logger.error("IO error in SimpleMessageLogger.",e);
            try {
                close();
            } catch (IOException ex) {
                logger.error("Error occurred during closing reader/writer.",e);
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
            isActive = false;
            logger.error(
                    String.format("Cannot write to %s. File is already opened by another application", logFile.getName())
            );
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
            logger.error(
                    String.format("Cannot read from file: %s. An IOexception has occurred.", logFile.getName())
            );
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
            logger.error(
                    String.format("Cannot write to file %s. Message is null.", logFile.getName())
            );
            return;
        }
        if (!isActive) return;
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
            logger.info(
                    String.format("Log to file %s: %s.", logFile.getName(), message)
            );
        } catch (IOException e) {
            logger.error(
                    String.format("Cannot write to file: %s. An IOexception has occurred.", logFile.getName())
            );
        }
    }
}
