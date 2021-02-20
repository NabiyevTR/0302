package server;

import java.io.Closeable;
import java.io.File;

public interface MessageLogger extends Closeable {

    void setFile(String path);

    String read();

    void write(String message);

    void setNumberOfMessagesToRead(int numberOfMessagesToRead);

    default boolean isActive () {
        return true;
    }
}
