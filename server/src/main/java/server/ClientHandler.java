package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                System.out.println("client want to disconnected ");
                                out.writeUTF(Command.END);
                                throw new RuntimeException("client want to disconnected");
                            }
                            if (str.startsWith(Command.AUTH)) {
                                String[] token = str.split("\\s");
                                String newNick = server.getAuthService()
                                        .getNicknameByLoginAndPassword(token[1], token[2]);
                                login = token[1];
                                if (newNick != null) {
                                    if (!server.isLoginAuthenticated(login)) {
                                        socket.setSoTimeout(0);
                                        nickname = newNick;
                                        sendMsg(Command.AUTH_OK + " " + nickname);
                                        server.subscribe(this);

                                        break;
                                    } else {
                                        sendMsg("С этим логинов уже вошли");
                                    }
                                } else {
                                    sendMsg("Неверный логин / пароль");
                                }
                            }

                            if (str.startsWith(Command.REG)) {
                                String[] token = str.split("\\s");
                                if (token.length < 4) {
                                    continue;
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
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                out.writeUTF(Command.END);
                                break;
                            }

                            if (str.startsWith(Command.PRIVATE_MSG)) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }

                            if (str.startsWith(Command.CHANGE_NICK)) {
                                String[] token = str.trim().split("\\s+");

                                if (token.length != 2) {
                                    sendMsg(Command.CHANGE_NICK_NO);
                                    continue;
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
                                continue;
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }


                } catch (SocketTimeoutException e) {
                    try {
                        System.out.println("Socket timeout for client " + socket.getRemoteSocketAddress());
                        out.writeUTF(Command.END);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
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

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
