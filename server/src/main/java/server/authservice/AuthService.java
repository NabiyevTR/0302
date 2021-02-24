package server.authservice;

import java.io.Closeable;

public interface AuthService extends Closeable {
    String getNicknameByLoginAndPassword(String login, String password);
    boolean registration(String login, String password, String nickname);
    boolean changeNickName(String login, String password, String newNickname);
    boolean changeNickName(String login, String newNickname);
}
