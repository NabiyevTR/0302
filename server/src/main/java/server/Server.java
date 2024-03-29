package server;

import commands.Command;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import server.authservice.AuthService;
import server.authservice.DBAuthService;
import server.messagecorrector.SimpleWordCorrector;
import server.messagecorrector.WordCorrector;
import server.messagelogger.MessageLogger;
import server.messagelogger.SimpleMessageLogger;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;
    private final List<ClientHandler> clients;
    private final AuthService authService;

    // Custom logger for logging messages from users
    private final MessageLogger messageLogger;

    // Log4j logger
    private final Logger logger = LogManager.getLogger(Server.class);
    private final WordCorrector wordCorrector;
    //private final String LOG_FILE_PATH = "C:\\Users\\HP 8570W\\Google Диск\\Программирование\\java\\03\\0302\\server\\src\\main\\resources";
    private final String LOG_FILE_PATH = "C:\\test\\chatLog.txt";
    private static final int MAX_CONNECTIONS = 2;
    ExecutorService serverExecutor;

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        authService = new DBAuthService();
        messageLogger = new SimpleMessageLogger(LOG_FILE_PATH);
        messageLogger.setNumberOfMessagesToRead(3);
        wordCorrector = new SimpleWordCorrector();


        try {
            serverExecutor = Executors.newFixedThreadPool(MAX_CONNECTIONS);
            server = new ServerSocket(PORT);
            logger.info("Server started");

            while (true) {
                socket = server.accept();
                logger.info("Client connected " + socket.getRemoteSocketAddress());
                serverExecutor.execute(new ClientHandler(this, socket));
            }

        } catch (IOException e) {
            logger.fatal("Server failed", e);
        } finally {
            try {
                wordCorrector.close();
                messageLogger.close();
                authService.close();
                server.close();
                serverExecutor.shutdown();
            } catch (IOException e) {
                logger.error("Error has occurred during closing server", e);
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        if (wordCorrector.isActive()) {
            msg = wordCorrector.getCorrectedText(msg);
        }

        String message = String.format("[ %s ] : %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
        logMessage(message);
        logger.debug("Client send message: " + message);
    }

    public void logMessage(String msg) {
        if (messageLogger.isActive()) {
            messageLogger.write(msg);
        }
    }

    public String getLoggedMessages() {
        return messageLogger.read();
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg) {
        if (wordCorrector.isActive()) {
            msg = wordCorrector.getCorrectedText(msg);
        }

        String message = String.format("[ %s ] to [ %s ]: %s", sender.getNickname(), receiver, msg);
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(receiver)) {
                c.sendMsg(message);
                if (!c.equals(sender)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg("Don't find user: " + receiver);
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST);
        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }
        String message = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }
}
