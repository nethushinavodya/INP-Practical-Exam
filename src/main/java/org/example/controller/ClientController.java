package org.example.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientController {
    public VBox ClientTextArea;
    public TextField clientInputMsg;
    public Button sendMsg;
    public Button sendEmoji;
    public Button sendImage;
    public ScrollPane ScrollPane;

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String message = "";
    private String clientId = "";//client id will be set by the server

    public void initialize() {
        ClientTextArea.heightProperty().addListener((observable, oldValue, newValue) -> ScrollPane.setVvalue(1.0));

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 3000);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                appendMsg("Connected to server");

                // First message from server should be client ID
                message = dataInputStream.readUTF();
                if (message.startsWith("CLIENTID:")) {
                    clientId = message.substring(9);
                    Platform.runLater(() -> appendMsg("You are " + clientId));
                }

                while (!message .equals("bye")) {
                    message = dataInputStream.readUTF();

                    if (message.startsWith("image")) {
                        String imagePath = message.substring(5);
                        Platform.runLater(() -> {
                            displayImage(imagePath);
                        });
                        // The next message will contain who sent the image
                        continue;
                    } else if (message.startsWith("file")) {
                        String filePath = message.substring(4);
                        // The next message will contain who sent the file
                        continue;
                    } else {
                        // This handles messages from the server and other clients
                        Platform.runLater(() -> appendMsg(message));
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> appendMsg("Error: Server not found or disconnected"));
                e.printStackTrace();
            }
        }).start();
    }

    private void displayImage(String imagePath) {
        try {
            File file = new File(imagePath);
            Image image = new Image(file.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(150);
            imageView.setFitHeight(150);
            ClientTextArea.getChildren().add(imageView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendMsg(String text) {
        Label label = new Label(text);
        ClientTextArea.getChildren().add(label);
    }

    public void btnSend(ActionEvent actionEvent) {
        String message = clientInputMsg.getText();
        if (!message.isEmpty()) {
            try {

                // Display locally with your client ID and current date
                String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                appendMsg(clientId + ": " + dateTime + " - " + message);

                // Send just the message - server will add the client ID
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
                clientInputMsg.clear();
            } catch (IOException e) {
                Platform.runLater(() -> appendMsg("Error: Could not send message"));
                e.printStackTrace();
            }
        }
    }

    public void btnSendEmoji(ActionEvent actionEvent) {
        String emojis = "😊";
        try {
            appendMsg(clientId + ": " + emojis);
            dataOutputStream.writeUTF(emojis);
            dataOutputStream.flush();
        } catch (IOException e) {
            Platform.runLater(() -> appendMsg("Error: Could not send emoji"));
            e.printStackTrace();
        }
    }

    public void btnSendImage(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.txt", "*.doc", "*.docx")
        );
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            String fileName = file.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            if (extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpeg") ||
                    extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("gif")) {
                try {
                    displayImage(file.getPath());
                    dataOutputStream.writeUTF("image" + file.getPath());
                    dataOutputStream.flush();
                    appendMsg(clientId + " sent an image");
                } catch (IOException e) {
                    Platform.runLater(() -> appendMsg("Error: Could not send image"));
                    e.printStackTrace();
                }
            } else if (extension.equalsIgnoreCase("txt") || extension.equalsIgnoreCase("doc") ||
                    extension.equalsIgnoreCase("docx")) {
                try {
                    dataOutputStream.writeUTF("file" + file.getPath());
                    dataOutputStream.flush();
                    appendMsg(clientId + " sent a file: " + fileName);
                } catch (IOException e) {
                    Platform.runLater(() -> appendMsg("Error: Could not send file"));
                    e.printStackTrace();
                }
            }
        }
    }
}
