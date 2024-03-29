package server;

import commands.Command;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class ClientHandler implements Runnable {

    private final Logger logger = LogManager.getLogger(ClientHandler.class);

    private Server server;
    private Socket socket;

    private static final int SOCKET_TIMEOUT = 120_000;

    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            logger.error("Client handler failed.", e);
        }
    }

    @Override
    public void run() {

        try {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            authentication();
            process();

        } catch (SocketTimeoutException e) {
            try {
                out.writeUTF(Command.END);
                logger.info("Socket timeout for client " + socket.getRemoteSocketAddress());
            } catch (IOException ioException) {
                logger.error("Failed to send command.", ioException);
            }
        } catch (IOException e) {
            logger.error("IOException exception has occurred.", e);
        } catch (RuntimeException e) {
            logger.error("Runtime exception has occurred.", e);
        } finally {
            server.unsubscribe(this);
            logger.info(String.format("Client %s disconnected.", socket.getRemoteSocketAddress()));
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Failed to close socket.", e);
            }
        }
    }

    private void authentication() throws IOException {
        while (true) {
            String msg = in.readUTF();

            if (msg.equals(Command.END)) {
                endConnection();

            } else if (msg.startsWith(Command.AUTH)) {
                if (authUser(msg)) break;

            } else if (msg.startsWith(Command.REG)) {
                regUser(msg);
            }
        }
    }

    private void process() throws IOException {

        while (true) {
            String message = in.readUTF();

            if (message.startsWith("/")) {
                logger.debug(
                        String.format("Request from %s: %s.", socket.getRemoteSocketAddress(), message)
                );

                if (message.equals(Command.END)) {
                    endConnection();
                    break;
                }

                if (message.startsWith(Command.PRIVATE_MSG)) {
                    privateMsg(message);
                    continue;
                }

                if (message.startsWith(Command.CHANGE_NICK)) {
                    changeNick(message);
                    continue;
                }

                if (message.startsWith(Command.GET_MESSAGES)) {
                    sendLogMsg();
                    continue;
                }

            } else {
                server.broadcastMsg(this, message);
            }
        }

    }

    void sendLogMsg() {
        sendMsg(Command.MESSAGES + " " + server.getLoggedMessages());
    }

    public void sendMsg(String msg) {
        if (msg == null) return;
        try {
            out.writeUTF(msg);
            logger.debug("Server sent the message to client: " + msg);
        } catch (IOException e) {
            logger.error("Failed to send message.", e);
        }
    }

    private void endConnection() throws RuntimeException {
        sendMsg(Command.END);
        throw new RuntimeException(
                String.format("Client %s wants to disconnect.", socket.getRemoteSocketAddress()));
    }

    private boolean authUser(String msg) throws SocketException {
        if (msg == null) return false;
        String[] token = msg.split("\\s");
        if (token.length != 3) {
            sendMsg("Wrong login or password.");
            return false;
        }
        String newNick = server.getAuthService().getNicknameByLoginAndPassword(token[1], token[2]);
        login = token[1];
        if (newNick != null) {
            if (!server.isLoginAuthenticated(login)) {
                socket.setSoTimeout(0);
                nickname = newNick;
                sendMsg(Command.AUTH_OK + " " + nickname);
                server.subscribe(this);
                return true;
            } else {
                sendMsg("Already signed in with this login");
            }
        } else {
            sendMsg("Wrong login or password.");
        }
        return false;
    }

    private void regUser(String msg) throws SocketException {
        if (msg == null) return;
        String[] token = msg.split("\\s");
        if (token.length < 4) {
            return;
        }
        boolean regSuccessful = server.getAuthService()
                .registration(token[1], token[2], token[3]);
        if (regSuccessful) {
            socket.setSoTimeout(0);
            sendMsg(Command.REG_OK);
        } else {
            sendMsg(Command.REG_NO);
        }
    }


    private void privateMsg(String msg) {
        if (msg == null) return;
        String[] token = msg.split("\\s+", 3);
        if (token.length < 3) {
            return;
        }
        server.privateMsg(this, token[1], token[2]);
    }

    private void changeNick(String msg) {
        if (msg == null) return;
        String[] token = msg.trim().split("\\s+");
        if (token.length != 2) {
            sendMsg(Command.CHANGE_NICK_NO);
            return;
        }
        if (server.getAuthService().changeNickName(login, token[1])) {
            sendMsg(Command.CHANGE_NICK_OK + " " + token[1]);
            server.broadcastMsg(this,
                    String.format("[%s] now is [%s]", nickname, token[1])
            );
            nickname = token[1];
            server.broadcastClientList();
        } else {
            sendMsg(Command.CHANGE_NICK_NO);
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
