package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Server extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    public void start(Stage stage) throws IOException {
        Parent load = FXMLLoader.load(getClass().getResource("/view/server.fxml"));
        Scene scene = new Scene(load);
        stage.setTitle("Server");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}