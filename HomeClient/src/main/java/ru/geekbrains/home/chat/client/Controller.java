package ru.geekbrains.home.chat.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {
    @FXML
    TextArea mainTextArea;
    @FXML
    TextField mainTextField, userNameField;
    @FXML
    HBox authPanel, msgPanel;
    @FXML
    ListView<String> clientsListView;
    @FXML
    TextField mainName;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String tempName;

    public void setAuthorized(boolean authorized) { // видимость и невидимость панелей
        msgPanel.setVisible(authorized);
        msgPanel.setManaged(authorized);
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        clientsListView.setVisible(authorized);
        clientsListView.setManaged(authorized);
        mainName.setText(tempName);
    }

    public void clickMeBtnAction() { //отправка сообщения
        if (!mainTextField.getText().trim().isEmpty()) {
            try {
                out.writeUTF(mainTextField.getText());
                mainTextField.clear();
                mainTextField.requestFocus();
            } catch (Exception e) {
                showError("Невозможно отправить сообщение на сервер");
            }
        }
    }

    public void sendCloseRequest() { // запрос на закрытие через строку
        try {
            if (out != null) {
                out.writeUTF("/exit");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth() { // попытка авторизации
        connect();
        try {
            out.writeUTF("/auth " + userNameField.getText());
            tempName = userNameField.getText();
            userNameField.clear();
        } catch (IOException e) {
            showError("Невозможно установить соединение с сервером");
        }
    }

    public void connect() { // подключение
        if (socket != null && !socket.isClosed()) {
            return;
        }
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> mainClientLogic()).start();
        } catch (IOException e) {
            showError("Сервер не отвечает....");
        }
    }

    private void mainClientLogic() {
        try {
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.equals("/exit")) {
                    closeConnection();
                }
                if (inputMessage.startsWith("/authok")) {
                    setAuthorized(true);
                    break;
                }
                mainTextArea.appendText(inputMessage + "\n");
            }
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.startsWith("/")) {
                    if (inputMessage.equals("/exit")) {
                        break;
                    }
                    if (inputMessage.startsWith("/clients_list ")) {
                        Platform.runLater(() -> {
                            String[] tokens = inputMessage.split("\\s+");
                            clientsListView.getItems().clear();
                            for (int i = 1; i < tokens.length; i++) {
                                clientsListView.getItems().add(tokens[i]);
                            }
                        });
                    }
                    continue;
                }
                mainTextArea.appendText(inputMessage + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {  //безопасное отключение
        setAuthorized(false);
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

    public void showError(String msg) { // показать ошибку юзеру
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public void clientsListDoubleClick(MouseEvent mouseEvent) { // вызов личного сообщение двойным кликом
        if (mouseEvent.getClickCount() == 2) {
            String selectedUser = clientsListView.getSelectionModel().getSelectedItem();
            mainTextField.setText("/w " + selectedUser + " ");
            mainTextField.requestFocus();
            mainTextField.selectEnd();
        }
    }
}
