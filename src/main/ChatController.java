package main;


import com.sun.istack.internal.NotNull;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public class ChatController {

    private static final Logger log = Logger.getLogger(ChatController.class);

    @FXML
    public Button sendButton;

    @FXML
    public TextField message;

    @FXML
    public ListView<String> messageList;

    private Client client = null;

    public ChatController() {
        UserPreferences user = chatInfromationDailog();

        while (user.userPort != ChatConstants.getDefaultPort()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Внимание!");
            alert.setHeaderText("Внимание!");
            alert.setContentText("Нет сервера на текущем порте!");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                user = chatInfromationDailog();
            } else {
                System.exit(0);
            }

        }

        client = new Client(user.userName, this, user.userPort);
        log.info("User - " + client.clientName + " connected to chat");
        try {
            client.makeConnection();
        } catch (IOException ignored) {

        }
    }


    @NotNull
    private UserPreferences chatInfromationDailog() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("ChatNIO");
        dialog.setHeaderText("Чтобы присоединиться к чату \nвведите имя и порт!");

        dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        TextField port = new TextField();
        port.setPromptText(String.valueOf(ChatConstants.getDefaultPort()));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(port, 1, 1);


        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        username.textProperty().addListener((observable, oldValue, newValue) ->
                loginButton.setDisable(newValue.trim().isEmpty() || port.getText().isEmpty()));

        port.textProperty().addListener((observable, oldValue, newValue) ->
                loginButton.setDisable(!newValue.trim().matches("([0-9])*") || username.getText().isEmpty()));
        dialog.getDialogPane().setContent(grid);

        Platform.runLater(username::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), port.getText());
            }

            if (dialogButton == ButtonType.CANCEL || dialogButton == ButtonType.CLOSE)
                System.exit(0);

            return null;
        });


        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> log.info("User " + usernamePassword.getKey() + " in PORT = " + ChatConstants.getDefaultPort() + result.get().getValue() + " created. "));

        if (result.get().getValue().isEmpty())
            return new UserPreferences(result.get().getKey(), ChatConstants.getDefaultPort());

        return new UserPreferences(result.get().getKey(), Integer.parseInt(result.get().getValue()));
    }

    @FXML
    public void onEnter() {
        messageSender();
    }

    @FXML
    public void sendMessage() {
        messageSender();
    }

    private void messageSender() {
        if (!message.getText().isEmpty()) {
            String sendTime = " " + LocalDateTime.now().getHour() + ":" + LocalDateTime.now().getMinute() + ":";
            String seconds = String.valueOf(LocalDateTime.now().getSecond());
            if (seconds.length() < 2)
                sendTime += 0 + seconds + "  ";
            else sendTime += seconds + "  ";
            client.sendMessage(sendTime + "[" + client.clientName + "]" + ": " + message.getText());
            message.clear();
        }
    }

    void messageReceiver(String message) {
        Platform.runLater(() -> messageList.getItems().add(message));
    }


    private class UserPreferences {
        final String userName;
        final int userPort;

        UserPreferences(String userName, int userPort) {
            this.userName = userName;
            this.userPort = userPort;
        }

        @Override
        public String toString() {
            return "User name - " + userName + "\nUser port - " + userPort;
        }
    }

}

