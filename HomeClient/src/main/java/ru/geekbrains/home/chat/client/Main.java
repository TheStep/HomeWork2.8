package ru.geekbrains.home.chat.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("/chat.fxml").openStream());
        Controller controller = fxmlLoader.getController();

        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setTitle("Chat");
        primaryStage.setOnCloseRequest(event -> controller.sendCloseRequest());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
