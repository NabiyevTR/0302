package server;

import java.sql.*;

public class TestDBConnection {


    // JDBC URL, username and password of MySQL server
    private static final String url = "jdbc:mysql://localhost:3306/chat";
    private static final String user = "root";
    private static final String password = "123456";

    // JDBC variables for opening and managing connection
    private static Connection connection;
    private static Statement statement;
    private static ResultSet resultSet;


    public static void main(String[] args) {
        //java -classpath c:\Java\mysql-connector-java-8.0.11.jar;c:\Java Program
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
            System.out.println("Connection successful!");
        } catch (Exception ex) {
            System.out.println("Connection failed...");

            System.out.println(ex);
        }


        String query = "select count(*) from users";

        try {
            // opening database connection to MySQL server
            connection = DriverManager.getConnection(url, user, password);

            // getting Statement object to execute query
            statement = connection.createStatement();

            // executing SELECT query
            resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int count = resultSet.getInt(1);
                System.out.println("Total number of books in the table : " + count);
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT `nickname` FROM `users` WHERE `login`=?;"
                            + " SELECT `nickname` FROM `users` WHERE `password`=?;")// +
                 //  "SELECT `nickname` FROM `chat.users` WHERE `password`=?")
//    SQL = "Update developers SET salary=? WHERE specialty=?";

            ) {
                preparedStatement.setString(1, "1");
                 preparedStatement.setString(2, "2");
                resultSet = preparedStatement.executeQuery();

            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Cannot add new user in DB");

            }

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close connection ,stmt and resultset here
            try {
                connection.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                statement.close();
            } catch (SQLException se) { /*can't do anything  */ }
            try {
                resultSet.close();
            } catch (SQLException se) {/* can't do anything */ }
        }
    }

}




