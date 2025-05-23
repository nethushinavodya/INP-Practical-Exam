package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client2 extends Application {
    public static void main(String[]args){
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent load = FXMLLoader.load(getClass().getResource("/view/client.fxml"));
        Scene scene = new Scene(load);
        stage.setTitle("Client 02");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
