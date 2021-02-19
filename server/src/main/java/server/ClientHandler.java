package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private final int SOCKET_TIMEOUT = 120_000;

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

            new Thread(() -> {
                try {
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                    authentication();
                    process();

                } catch (SocketTimeoutException e) {
                    try {
                        System.out.println("Socket timeout for client " + socket.getRemoteSocketAddress());
                        out.writeUTF(Command.END);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (RuntimeException | IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Client disconnected");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void authentication() throws IOException {
        while (true) {
            String str = in.readUTF();

            if (str.equals(Command.END)) {
                endConnection();

            } else if (str.startsWith(Command.AUTH)) {
                if (authUser(str)) {
                }

            } else if (str.startsWith(Command.REG)) {
                regUser(str);
            }
        }
    }

    private void process() throws IOException {

        while (true) {
            String message = in.readUTF();

            if (message.startsWith("/")) {
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
        if (msg==null) return;
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endConnection() {
        System.out.println("client want to disconnected ");
        sendMsg(Command.END);
        throw new RuntimeException("client want to disconnected");
    }

    private boolean authUser(String msg) throws SocketException {
        if (msg ==null) return false;
        String[] token = msg.split("\\s");
        String newNick = server.getAuthService()
                .getNicknameByLoginAndPassword(token[1], token[2]);
        login = token[1];
        if (newNick != null) {
            if (!server.isLoginAuthenticated(login)) {
                socket.setSoTimeout(0);
                nickname = newNick;
                sendMsg(Command.AUTH_OK + " " + nickname);
                server.subscribe(this);
                return true;
            } else {
                sendMsg("С этим логинов уже вошли");
            }
        } else {
            sendMsg("Неверный логин / пароль");
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
