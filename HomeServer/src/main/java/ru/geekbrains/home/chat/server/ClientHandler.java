package ru.geekbrains.home.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private String userName;
    private DataInputStream in;
    private DataOutputStream out;


    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream()); //входящее
            this.out = new DataOutputStream(socket.getOutputStream()); //выходящее
            new Thread(() -> logic()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logic() {
        try {
            while (!consumeAuthorizeMessage(in.readUTF())) ;
            while (consumeRegularMessage(in.readUTF())) ;  //отключаем возможность вызова команд
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Юзер " + userName + " отключился.");
            server.unSubscribe(this);
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message); //отправка сообщений
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean consumeRegularMessage(String inputMessage) {
        if (inputMessage.startsWith("/")) {
            if (inputMessage.equals("/exit")) {
                sendMessage("/exit");
                return false;
            }
            if (inputMessage.startsWith("/w ")) {
                String[] tokens = inputMessage.split("\\s+", 3);
                server.sendPersonalMessage(this, tokens[1], tokens[2]);
            }
            return true;
        }
        server.broadCastMessage(userName + ": " + inputMessage); //отображение сообщения всем юзерам
        return true;

    }

    private boolean consumeAuthorizeMessage(String message) { // Цикл авторизации
        if (message.startsWith("/auth ")) {
            String[] tokens = message.split("\\s+");//определение никнейма при авторизации
            if (tokens.length == 1) {
                sendMessage("Server : Не указано имя пользователя.");
                return false;
            }
            if (tokens.length > 2) {
                sendMessage("Server : Имя пользователя не может состоять из нескольких слов");
                return false;
            }
            String selectedUsername = tokens[1];
            if (server.isUsernameUsed(selectedUsername)) {
                sendMessage("Server : Данное имя уже используется.");
                return false;
            }
            userName = selectedUsername;
            sendMessage("/authok "); //подверждение авторизации
            sendMessage("Вы зашли в чат по именем: " + userName);
            server.subscribe(this); //добавление в список подписчиков
            return true;
        } else {
            sendMessage("Server : Необходима авторизация");
            return false;

        }
    }

    public String getUserName() { //геттер для вызова имени в сервере
        return userName;
    }

    private void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
