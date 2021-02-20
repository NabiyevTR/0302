package server;

import commands.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;
    private final List<ClientHandler> clients;
    private final AuthService authService;
    private final MessageLogger messageLogger;
    //private final String LOG_FILE_PATH = "C:\\Users\\HP 8570W\\Google Диск\\Программирование\\java\\03\\0302\\server\\src\\main\\resources";
    private final String LOG_FILE_PATH = "C:\\test\\chatLog.txt";

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        authService = new DBAuthService();
        messageLogger = new SimpleMessageLogger(LOG_FILE_PATH);
        messageLogger.setNumberOfMessagesToRead(3);


        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started.");

            while (true) {
                socket = server.accept();
                System.out.println("Client connected " + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                messageLogger.close();
                authService.close();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ] : %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
        logMessage(message);
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
