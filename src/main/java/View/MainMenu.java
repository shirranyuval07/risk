package View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.example.demo.RiskWebSocketClient;
import javafx.util.Pair;
import service.UserService;

public class MainMenu extends StackPane {

    public record PlayerSetup(String name, Color color, String type) {}

    private final List<PlayerRow> playerRows = new ArrayList<>();
    private final BiConsumer<List<PlayerSetup>, RiskWebSocketClient> onStartGame;

    // null when running in client mode (no Spring context)
    private final UserService userService;
    private Entity.User currentUser = null;

    public MainMenu(BiConsumer<List<PlayerSetup>, RiskWebSocketClient> onStartGame, UserService userService) {
        this.onStartGame = onStartGame;
        this.userService = userService;
        setBackground(new Background(new BackgroundFill(Color.rgb(8, 16, 35), CornerRadii.EMPTY, Insets.EMPTY)));

        VBox mainContent = new VBox(30);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(50));

        Label title = new Label("⚔ RISK: GLOBAL CONQUEST ⚔");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        title.setTextFill(Color.WHITE);

        VBox playersBox = new VBox(15);
        playersBox.setAlignment(Pos.CENTER);

        Color[] defaultColors = {
                Color.rgb(225, 60, 60),
                Color.rgb(0, 85, 225),
                Color.rgb(225, 143, 60),
                Color.rgb(46, 170, 80),
                Color.rgb(140, 80, 190),
                Color.rgb(210, 175, 45)
        };

        for (int i = 0; i < 6; i++) {
            PlayerRow row = new PlayerRow(i + 1, defaultColors[i]);
            playerRows.add(row);
            playersBox.getChildren().add(row);
        }

        // --- Local game button ---
        Button btnStart = new Button("START LOCAL CONQUEST");
        btnStart.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        btnStart.setStyle("-fx-background-color: #2eaa50; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 5;");
        btnStart.setCursor(javafx.scene.Cursor.HAND);

        btnStart.setOnAction(e -> {
            List<PlayerSetup> activePlayers = new ArrayList<>();
            for (PlayerRow row : playerRows) {
                if (!row.getType().equals("None")) {
                    activePlayers.add(new PlayerSetup(row.getName(), row.getColor(), row.getType()));
                }
            }
            if (activePlayers.size() >= 2) {
                onStartGame.accept(activePlayers, null);
            } else {
                new Alert(Alert.AlertType.WARNING, "You need at least 2 players to start!").show();
            }
        });

        // --- Multiplayer buttons ---
        RiskWebSocketClient networkClient = new RiskWebSocketClient("Guest");
        networkClient.connect();

        Button createRoomBtn = new Button("CREATE ROOM");
        createRoomBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        createRoomBtn.setStyle("-fx-background-color: #0055e1; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        createRoomBtn.setCursor(javafx.scene.Cursor.HAND);
        createRoomBtn.setOnAction(e -> networkClient.sendAction("CREATE_ROOM", "", "Create me a room"));

        Button joinRoomBtn = new Button("JOIN ROOM");
        joinRoomBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        joinRoomBtn.setStyle("-fx-background-color: #e18f3c; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        joinRoomBtn.setCursor(javafx.scene.Cursor.HAND);
        joinRoomBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Join Room");
            dialog.setHeaderText("Enter the 4-character Room Code:");
            dialog.showAndWait().ifPresent(code -> {
                if (!code.trim().isEmpty()) {
                    networkClient.sendAction("JOIN_ROOM", code.toUpperCase(), "Let me in");
                }
            });
        });

        networkClient.setOnMessageReceived(message -> {
            if (message.type().equals("ROOM_CREATED")) {
                showLobby(message.roomId(), true, networkClient, mainContent);
            } else if (message.type().equals("JOIN_ROOM_SUCCESS")) {
                showLobby(message.roomId(), false, networkClient, mainContent);
            } else if (message.type().equals("ERROR")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(message.content());
                alert.show();
            }
        });

        HBox multiplayerBox = new HBox(20);
        multiplayerBox.setAlignment(Pos.CENTER);
        multiplayerBox.getChildren().addAll(createRoomBtn, joinRoomBtn);

        mainContent.getChildren().addAll(title, playersBox, btnStart, multiplayerBox);

        // --- Rules button ---
        Button btnRules = new Button("📖 Rules");
        btnRules.setStyle("-fx-background-color: #4a6a92; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnRules.setCursor(javafx.scene.Cursor.HAND);
        btnRules.setOnAction(e -> RulesDialog.show());
        StackPane.setAlignment(btnRules, Pos.TOP_RIGHT);
        StackPane.setMargin(btnRules, new Insets(20));

        // --- Auth box (only shown in server mode where UserService is available) ---
        VBox authBox = new VBox(15);
        authBox.setAlignment(Pos.TOP_LEFT);
        authBox.setMaxWidth(150);
        StackPane.setAlignment(authBox, Pos.TOP_LEFT);
        StackPane.setMargin(authBox, new Insets(20));

        Label userLabel = new Label("Welcome, Guest");
        userLabel.setTextFill(Color.WHITE);
        userLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        if (userService != null) {
            // Full auth UI available (server mode)
            Button loginBtn  = new Button("Login");
            Button signupBtn = new Button("Sign Up");
            Button logoutBtn = new Button("Logout");
            logoutBtn.setVisible(false);
            logoutBtn.setManaged(false);

            authBox.getChildren().addAll(userLabel, loginBtn, signupBtn, logoutBtn);

            signupBtn.setOnAction(e -> {
                Optional<Pair<String, String>> result = LoginDialog.show("Sign Up");
                result.ifPresent(credentials -> {
                    boolean success = userService.signup(credentials.getKey(), credentials.getValue());
                    if (success) {
                        new Alert(Alert.AlertType.INFORMATION, "Registration successful! You can now login.").show();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Username already exists!").show();
                    }
                });
            });

            loginBtn.setOnAction(e -> {
                Optional<Pair<String, String>> result = LoginDialog.show("Login");
                result.ifPresent(credentials -> {
                    Entity.User loggedInUser = userService.login(credentials.getKey(), credentials.getValue());
                    if (loggedInUser != null) {
                        userLabel.setText("Commander: " + loggedInUser.getUsername());
                        networkClient.setPlayerName(loggedInUser.getUsername());
                        playerRows.getFirst().setName(loggedInUser.getUsername());
                        loginBtn.setVisible(false);  loginBtn.setManaged(false);
                        signupBtn.setVisible(false); signupBtn.setManaged(false);
                        logoutBtn.setVisible(true);  logoutBtn.setManaged(true);
                        this.currentUser = loggedInUser;
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Invalid username or password!").show();
                    }
                });
            });

            logoutBtn.setOnAction(e -> {
                userLabel.setText("Welcome, Guest");
                networkClient.setPlayerName("Guest");
                playerRows.getFirst().setName("General 1");
                loginBtn.setVisible(true);  loginBtn.setManaged(true);
                signupBtn.setVisible(true); signupBtn.setManaged(true);
                logoutBtn.setVisible(false); logoutBtn.setManaged(false);
                this.currentUser = null;
            });

        } else {
            // Client mode — just show a label, no login available
            Label clientLabel = new Label("🎮 Client Mode");
            clientLabel.setTextFill(Color.LIGHTGRAY);
            clientLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
            authBox.getChildren().addAll(userLabel, clientLabel);
        }

        getChildren().addAll(mainContent, btnRules, authBox);
    }

    // --- Lobby ---
    private void showLobby(String roomCode, boolean isHost, RiskWebSocketClient networkClient, VBox mainContent) {
        String myName = (currentUser != null) ? currentUser.getUsername() : networkClient.getPlayerName();
        List<String> lobbyPlayers = new ArrayList<>();
        lobbyPlayers.add(myName);

        VBox lobbyBox = new VBox(20);
        lobbyBox.setAlignment(Pos.CENTER);

        Label title = new Label("Waiting Room: " + roomCode);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Waiting for players to join...");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 20));
        subtitle.setTextFill(Color.LIGHTGRAY);

        TextArea playerList = new TextArea("Players in room:\n- You (" + myName + ")\n");
        playerList.setEditable(false);
        playerList.setMaxWidth(400);
        playerList.setPrefHeight(200);
        playerList.setFont(Font.font("Segoe UI", 16));

        Button startMultiplayerBtn = new Button("START MULTIPLAYER GAME");
        startMultiplayerBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        startMultiplayerBtn.setStyle("-fx-background-color: #e13c3c; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 5;");
        startMultiplayerBtn.setCursor(javafx.scene.Cursor.HAND);
        startMultiplayerBtn.setVisible(isHost);

        startMultiplayerBtn.setOnAction(e -> {
            long randomSeed = new java.util.Random().nextLong();
            String payload = randomSeed + ":" + String.join(",", lobbyPlayers);
            networkClient.sendAction("START_GAME", roomCode, payload);
        });

        lobbyBox.getChildren().addAll(title, subtitle, playerList, startMultiplayerBtn);

        networkClient.setOnMessageReceived(message -> {
            javafx.application.Platform.runLater(() -> {
                if (message.type().equals("PLAYER_JOINED")) {
                    lobbyPlayers.add(message.content());
                    playerList.appendText("- " + message.content() + " has joined!\n");
                } else if (message.type().equals("PLAYER_DISCONNECTED")) {
                    playerList.appendText("⚠ A player disconnected.\n");
                } else if (message.type().equals("GAME_STARTED")) {
                    javafx.application.Platform.runLater(() -> {
                        networkClient.setRoomId(message.roomId());
                        String[] parts = message.content().split(":");
                        long seed = Long.parseLong(parts[0]);
                        networkClient.setGameSeed(seed);
                        String[] playerNames = parts[1].split(",");

                        List<PlayerSetup> players = new ArrayList<>();
                        for (int i = 0; i < playerNames.length; i++) {
                            double hue = i * (360.0 / playerNames.length);
                            Color dynamicColor = Color.hsb(hue, 0.85, 0.9);
                            players.add(new PlayerSetup(playerNames[i], dynamicColor, "Human"));
                        }
                        onStartGame.accept(players, networkClient);
                    });
                }
            });
        });

        mainContent.setVisible(false);
        this.getChildren().add(lobbyBox);
    }

    private static class PlayerRow extends HBox {
        private final TextField nameField;
        private final ColorPicker colorPicker;
        private final ComboBox<String> typeBox;

        public PlayerRow(int playerNum, Color defaultColor) {
            setSpacing(15);
            setAlignment(Pos.CENTER);

            Label lbl = new Label("Player " + playerNum + ":");
            lbl.setTextFill(Color.WHITE);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            lbl.setPrefWidth(80);

            nameField = new TextField("General " + playerNum);
            nameField.setPrefWidth(150);

            colorPicker = new ColorPicker(defaultColor);
            colorPicker.setStyle("-fx-color-label-visible: false;");

            typeBox = new ComboBox<>();
            typeBox.getItems().addAll("None", "Human", "AI - Balanced", "AI - Defensive", "AI - Offensive");

            if (playerNum == 1) typeBox.setValue("Human");
            else if (playerNum == 2) typeBox.setValue("AI - Balanced");
            else typeBox.setValue("None");

            typeBox.setOnAction(e -> {
                boolean disabled = typeBox.getValue().equals("None");
                nameField.setDisable(disabled);
                colorPicker.setDisable(disabled);
            });

            getChildren().addAll(lbl, nameField, colorPicker, typeBox);
        }

        public String getName()  { return nameField.getText(); }
        public Color getColor()  { return colorPicker.getValue(); }
        public String getType()  { return typeBox.getValue(); }
        public void setName(String newName) { nameField.setText(newName); }
    }
}