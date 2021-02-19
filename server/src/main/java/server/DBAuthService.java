package server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;

public class DBAuthService implements AuthService {

    private static final String url = "jdbc:mysql://localhost:3306/chat";
    private static final String user = "root";
    private static final String password = "123456";

    private static Connection connection;
    private static ResultSet resultSet;


    public DBAuthService() {
        try {
            init();
            connection = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            System.out.println("Connection failed...");
            System.out.println(e);
        } finally {
            try {
                close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void init() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        System.out.println("Connection successful!");
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT `nickname`, `password` FROM `users` WHERE `login`= ? AND `password`= ?")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString("nickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        return addUser(login, password, nickname);
    }

    @Override
    public boolean changeNickName(String login, String password, String nickname) {
        if (!containsUser(login, password)) return false;

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `users` SET `nickname`=?  WHERE `login`=? AND `password` = ?")) {
            preparedStatement.setString(1, nickname);
            preparedStatement.setString(2, login);
            preparedStatement.setString(3, password);
            preparedStatement.addBatch();
            preparedStatement.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean changeNickName(String login, String nickname) {

        if (!containsUser(login)) return false;

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `users` SET `nickname`=?  WHERE `password` = ?")) {
            preparedStatement.setString(1, nickname);
            preparedStatement.setString(2, login);
            preparedStatement.addBatch();
            preparedStatement.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }


    private boolean addUser(String login, String password, String nickname) {

        if (containsUser(login)) return false;

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO `users` (`login`, `password`, `nickname`) VALUES  (?, ?, ?)")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, nickname);
            preparedStatement.addBatch();
            preparedStatement.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean containsUser(String login, String password) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT `login` FROM `users` WHERE `login`=? AND `password`=? ")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            resultSet = preparedStatement.executeQuery();
            if (resultSet == null) return false;
            while (resultSet.next()) {
                if (login.equals(resultSet.getString("login"))) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private boolean containsUser(String login) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT `login` FROM `users` WHERE `login`=?")) {
            preparedStatement.setString(1, login);
            resultSet = preparedStatement.executeQuery();
            if (resultSet == null) return false;
            while (resultSet.next()) {
                if (login.equals(resultSet.getString("login"))) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }


}


