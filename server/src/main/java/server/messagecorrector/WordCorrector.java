package server.messagecorrector;

import java.io.Closeable;
import java.util.Map;

public interface WordCorrector extends Closeable {
    String getCorrectedText(String text);

    boolean addAll(Map<String, String> wordMap);

    boolean forceAddAll(Map<String, String> wordMap);

    boolean add(String word, String correctedWord);

    boolean forceAdd(String word, String correctedWord);

    boolean remove(String word);

    boolean update(String word, String correctedWord);

    default boolean isActive() {return true;}
}

