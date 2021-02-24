package server.messagecorrector;


import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Map;


public class SimpleWordCorrector implements WordCorrector {

    private static final String url = "jdbc:mysql://localhost:3306/chat";
    private static final String user = "root";
    private static final String password = "123456";

    private static final String sqlGetCorrectedWord = "SELECT `correctedWord` FROM `chat`.`dictionary` WHERE `word`=?";
    private static final String sqlAddWord = "INSERT INTO `chat`.`dictionary` (`word`, `correctedWord`) VALUES  (?, ?)";
    private static final String sqlUpdateWord = "UPDATE `chat`.`dictionary` SET `correctedWord`=?  WHERE `word` = ?";
    private static final String sqlRemoveWord = "DELETE FROM `chat`.`dictionary` WHERE `word` = ?";

    private Connection connection;
    private boolean isActive;

    public SimpleWordCorrector() {
        try {
            init();
            connection = DriverManager.getConnection(url, user, password);
            isActive = true;
            System.out.println("Word corrector service started.");
        } catch (Exception e) {
            System.out.println("Word corrector service failed to start.");
            System.out.println(e);
            isActive = false;
            close();
        }
    }

    private void init() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCorrectedText(String text) {

        if (!isActive) return null;

        if (text == null) return null;
        if (text.isEmpty()) return text;

        String[] words = text.trim().split("(?U)\\W+");

        for (int i = 0; i < words.length; i++) {
            String correctedWord = checkWord(words[i]);
            if (correctedWord != null) {
                text = text.replaceAll("\\b" + words[i] + "\\b", correctedWord);
            }
        }
        return text;
    }

    private String checkWord(String word) {
        if (word == null) return null;
        return executeQueryAndGetCorrectedWord(sqlGetCorrectedWord, word);
    }

    @Override
    public boolean addAll(Map<String, String> wordMap) {
        if (!isActive) return false;

        if (wordMap == null) return false;

        for (Map.Entry<String, String> entry : wordMap.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
        return true;
    }

    @Override
    public boolean forceAddAll(Map<String, String> wordMap) {
        if (!isActive) return false;

        if (wordMap == null) return false;

        for (Map.Entry<String, String> entry : wordMap.entrySet()) {
            forceAdd(entry.getKey(), entry.getValue());
        }
        return true;
    }

    @Override
    public boolean add(String word, String correctedWord) {
        if (!isActive) return false;

        if (word == null || correctedWord == null) return false;

        if (containsWord(word)) return false;

        return executeRequest(sqlAddWord, word, correctedWord);
    }

    @Override
    public boolean forceAdd(String word, String correctedWord) {
        if (!isActive) return false;

        if (word == null || correctedWord == null) return false;

        if (containsWord(word)) {
            return executeRequest(sqlUpdateWord, correctedWord, word);
        } else {
            return executeRequest(sqlAddWord, word, correctedWord);
        }
    }

    private boolean executeRequest(String sqlRequest, String... params) {
        if (sqlRequest == null || params == null) return false;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlRequest)) {
            for (int i = 0; i < params.length; i++) {
                preparedStatement.setString(i + 1, params[i]);
            }
            preparedStatement.addBatch();
            preparedStatement.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String executeQueryAndGetCorrectedWord(String sqlRequest, String... params) {
        if (sqlRequest == null || params == null) return null;
        ResultSet resultSet = null;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlRequest)) {
            for (int i = 0; i < params.length; i++) {
                preparedStatement.setString(i + 1, params[i]);
            }
            preparedStatement.addBatch();
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                return resultSet.getString("correctedWord");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                resultSet.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return null;
    }


    @Override
    public boolean remove(String word) {
        if (!isActive) return false;

        if (word == null) return false;

        return executeRequest(sqlRemoveWord, word);
    }

    @Override
    public boolean update(String word, String correctedWord) {
        if (!isActive) return false;
        if (word == null || correctedWord == null) return false;
        return executeRequest(sqlUpdateWord, correctedWord, word);
    }

    private boolean containsWord(String word) {
        if (word == null) return false;
        return executeQueryAndGetCorrectedWord(sqlGetCorrectedWord, word) != null;
    }
}
