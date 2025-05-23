package org.example.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerController {
    public Button sendMsg;
    public TextField serverInputMsg;
    public Button sendEmoji;
    public Button sendImage;
    public VBox ServerTextArea;
    public ScrollPane scrollPane;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private int clientCounter = 0; // To assign client numbers (01, 02, etc.)

    public void initialize() {
        ServerTextArea.heightProperty().addListener((observable, oldValue, newValue) -> scrollPane.setVvalue(1.0));

        new Thread(() -> {
            try {
                appendMsg("Server started on port 3000");
                serverSocket = new ServerSocket(3000);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    clientCounter++;
                    String clientId = String.format("Client %02d", clientCounter);
                    String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    appendMsg(clientId + " connected at " + dateTime);
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this, clientId);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();

                    try {
                        DataOutputStream initialDos = new DataOutputStream(clientSocket.getOutputStream());
                        initialDos.writeUTF("CLIENTID:" + clientId);
                        initialDos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> appendMsg("Error: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

   //display message on server side
    public void appendMsg(String text) {
        Platform.runLater(() -> {
            Label label = new Label(text);
            ServerTextArea.getChildren().add(label);
        });
    }
    //display image on server side
    private void displayImage(String imagePath) {
        try {
            File file = new File(imagePath);
            Image image = new Image(file.toURI().toURL().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(150);
            imageView.setFitHeight(150);
            ServerTextArea.getChildren().add(imageView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void btnSend(ActionEvent actionEvent) {
        String message = serverInputMsg.getText();
        if (!message.isEmpty()) {
            // Broadcast message to all clients with Server prefix
            broadcastMessage("Server: " + message);
            appendMsg("Server: " + message);
            serverInputMsg.clear();
        }
    }

    public void btnSendImage(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.txt", "*.doc", "*.docx")
        );
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            String fileName = file.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            if (extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpeg") ||
                    extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("gif")) {
                displayImage(file.getPath());
                broadcastImage(file.getPath(), "Server");
                appendMsg("Server sent an image");
            } else if (extension.equalsIgnoreCase("txt") || extension.equalsIgnoreCase("doc") ||
                    extension.equalsIgnoreCase("docx")) {
                broadcastFile(file.getPath(), "Server");
                appendMsg("Server sent a file: " + fileName);
            }
        }
    }

    public void btnSendEmoji(ActionEvent actionEvent) {
        String emojis = "😀";
        broadcastMessage("Server: " + emojis);
        appendMsg("Server: " + emojis);
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastImage(String imagePath, String sender) {
        for (ClientHandler client : clients) {
            client.sendImage(imagePath, sender);
        }
    }

    public void broadcastFile(String filePath, String sender) {
        for (ClientHandler client : clients) {
            client.sendFile(filePath, sender);
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        appendMsg(clientHandler.getClientId() + " disconnected. Total clients: " + clients.size());
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;
        private ServerController server;
        private String clientId;

        public ClientHandler(Socket socket, ServerController server, String clientId) {
            this.socket = socket;
            this.server = server;
            this.clientId = clientId;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getClientId() {
            return clientId;
        }

        @Override
        public void run() {
            try {
                String message;
                while (true) {
                    message = dataInputStream.readUTF();

                    if (message.startsWith("image")) {
                        String imagePath = message.substring(5);
                        Platform.runLater(() -> {
                            displayImage(imagePath);
                            appendMsg(clientId + " sent an image");
                        });

                        for (ClientHandler client : clients) {
                            if (client != this) {
                                client.sendImage(imagePath, clientId);
                            }
                        }
                    } else if (message.startsWith("file")) {
                        String filePath = message.substring(4);
                        Platform.runLater(() -> {
                            appendMsg(clientId + " sent a file: " + new File(filePath).getName());
                        });

                        for (ClientHandler client : clients) {
                            if (client != this) {
                                client.sendFile(filePath, clientId);
                            }
                        }
                    } else {
                        String formattedMessage = clientId + ": " + message;
                        Platform.runLater(() -> appendMsg(formattedMessage));

                        for (ClientHandler client : clients) {
                            if (client != this) {
                                client.sendMessage(formattedMessage);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                server.removeClient(this);
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            try {
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendImage(String imagePath, String sender) {
            try {
                dataOutputStream.writeUTF("image" + imagePath);
                dataOutputStream.flush();
                dataOutputStream.writeUTF(sender + " sent an image");
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendFile(String filePath, String sender) {
            try {
                dataOutputStream.writeUTF("file" + filePath);
                dataOutputStream.flush();
                dataOutputStream.writeUTF(sender + " sent a file: " + new File(filePath).getName());
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
