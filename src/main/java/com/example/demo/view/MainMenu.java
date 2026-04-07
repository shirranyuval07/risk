package com.example.demo.view;

import com.example.demo.view.dialog.DialogManager;
import com.example.demo.network.shared.GameAction;
import com.example.demo.network.shared.GameMessage;
import com.example.demo.network.client.RiskWebSocketClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MainMenu extends StackPane {

    public record PlayerSetup(String name, Color color, String type) {}

    // =========================================================================
    //  Fields
    // =========================================================================

    private final List<PlayerRow> playerRows = new ArrayList<>();
    private final BiConsumer<List<PlayerSetup>, RiskWebSocketClient> onStartGame;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public MainMenu(BiConsumer<List<PlayerSetup>, RiskWebSocketClient> onStartGame) {
        this.onStartGame = onStartGame;

        setBackground(new Background(
                new BackgroundFill(Color.rgb(8, 16, 35), CornerRadii.EMPTY, Insets.EMPTY)));

        RiskWebSocketClient networkClient = new RiskWebSocketClient("Guest");
        networkClient.connect();

        VBox mainContent = buildMainContent(networkClient);


        Button btnRules = buildRulesButton();

        StackPane.setAlignment(btnRules, Pos.TOP_RIGHT);
        StackPane.setMargin(btnRules, new Insets(20));

        getChildren().addAll(mainContent, btnRules);

        // Listen for server responses to CREATE_ROOM / JOIN_ROOM
        networkClient.setOnMessageReceived(message -> Platform.runLater(() -> {
            switch (message.type()) {
                case ROOM_CREATED      -> openLobby(message, true,  networkClient, mainContent);
                case JOIN_ROOM_SUCCESS -> openLobby(message, false, networkClient, mainContent);
                case ERROR -> {
                    Map<String, Object> payload =  message.content();
                    showError(payload != null ? (String) payload.get("RoomNotFound") : "Unknown Error");
                }
                default -> {}
            }
        }));
    }

    // =========================================================================
    //  Building the main content
    // =========================================================================

    private VBox buildMainContent(RiskWebSocketClient networkClient) {
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50));

        Label title = new Label("⚔ RISK: GLOBAL CONQUEST ⚔");
        title.setFont(Font.font("Segue UI", FontWeight.BOLD, 40));
        title.setTextFill(Color.WHITE);

        content.getChildren().addAll(
                title,
                buildPlayerRows(),
                buildLocalStartButton(),
                buildMultiplayerButtons(networkClient)
        );
        return content;
    }

    private VBox buildPlayerRows() {
        Color[] defaultColors = {
                Color.rgb(225, 60, 60),
                Color.rgb(0, 85, 225),
                Color.rgb(225, 143, 60),
                Color.rgb(46, 170, 80),
                Color.rgb(140, 80, 190),
                Color.rgb(210, 175, 45)
        };

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);

        for (int i = 0; i < 6; i++) {
            PlayerRow row = new PlayerRow(i + 1, defaultColors[i]);
            playerRows.add(row);
            box.getChildren().add(row);
        }
        return box;
    }

    private Button buildLocalStartButton() {
        Button btn = styledButton("START LOCAL CONQUEST", "#2eaa50", 20);
        btn.setOnAction(e -> {
            List<PlayerSetup> activePlayers = getActivePlayers();
            if (activePlayers.size() >= 2) {
                onStartGame.accept(activePlayers, null);
            } else {
                new Alert(Alert.AlertType.WARNING, "You need at least 2 players to start!").show();
            }
        });
        return btn;
    }

    private HBox buildMultiplayerButtons(RiskWebSocketClient networkClient) {
        Button createBtn = styledButton("CREATE ROOM", "#0055e1", 16);
        Button joinBtn   = styledButton("JOIN ROOM",   "#e18f3c", 16);

        createBtn.setOnAction(e -> {
            Map<String, Object> payload = new HashMap<>();
            networkClient.sendAction(GameAction.CREATE_ROOM, "", payload);
        });

        joinBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Join Room");
            dialog.setHeaderText("Enter the 4-character Room Code:");
            dialog.showAndWait().ifPresent(code -> {
                if (!code.trim().isEmpty()) {
                    Map<String, Object> payload = new HashMap<>();
                    networkClient.sendAction(GameAction.JOIN_ROOM, code.toUpperCase(), payload);
                }
            });
        });

        HBox box = new HBox(20, createBtn, joinBtn);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Button buildRulesButton() {
        Button btn = new Button("📖 Rules");
        btn.setStyle("-fx-background-color: #4a6a92; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setOnAction(e -> DialogManager.showRulesDialog());
        return btn;
    }

    // =========================================================================
    //  Lobby
    // =========================================================================

    private void openLobby(GameMessage message, boolean isHost,
                           RiskWebSocketClient networkClient, VBox mainContent) {
        // Here message.content() is the Map we sent from the Server in CREATE_ROOM or JOIN_ROOM
        // We can optionally use the generated index or ID, but for now we rely on the network client's name
        String myName = networkClient.getPlayerName();

        // If it's a join, we append the assigned index (optional)
        if (!isHost && message.content() != null) {
            Map<String, Object> payload = message.content();
            if (payload.containsKey("playerID")) {
                myName += payload.get("playerID").toString();
            }
        }

        networkClient.setPlayerName(myName);
        LobbyScreen lobby = new LobbyScreen(message.roomId(), isHost, myName, networkClient, onStartGame);
        mainContent.setVisible(false);
        getChildren().add(lobby);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private List<PlayerSetup> getActivePlayers() {
        List<PlayerSetup> active = new ArrayList<>();
        for (PlayerRow row : playerRows) {
            if (!row.getType().equals("None"))
                active.add(new PlayerSetup(row.getName(), row.getColor(), row.getType()));
        }
        return active;
    }

    private static Button styledButton(String text, String hexColor, int fontSize) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segue UI", FontWeight.BOLD, fontSize));
        btn.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        btn.setCursor(javafx.scene.Cursor.HAND);
        return btn;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.show();
    }


    // =========================================================================
    //  Inner class: LobbyScreen — waiting room after creating / joining a room
    // =========================================================================

    private static class LobbyScreen extends VBox {

        public LobbyScreen(String roomCode,
                           boolean isHost,
                           String myName,
                           RiskWebSocketClient networkClient,
                           BiConsumer<List<PlayerSetup>, RiskWebSocketClient> onStartGame) {
            setAlignment(Pos.CENTER);
            setSpacing(20);

            List<String> lobbyPlayers = new ArrayList<>();
            lobbyPlayers.add(myName);

            Label title = new Label("Waiting Room: " + roomCode);
            title.setFont(Font.font("Segue UI", FontWeight.BOLD, 40));
            title.setTextFill(Color.WHITE);

            Label subtitle = new Label("Waiting for players to join...");
            subtitle.setFont(Font.font("Segue UI", FontWeight.NORMAL, 20));
            subtitle.setTextFill(Color.LIGHTGRAY);

            TextArea playerList = new TextArea("Players in room:\n- You (" + myName + ")\n");
            playerList.setEditable(false);
            playerList.setMaxWidth(400);
            playerList.setPrefHeight(200);
            playerList.setFont(Font.font("Segue UI", 16));

            Button startBtn = styledButton("START MULTIPLAYER GAME", "#e13c3c", 20);
            startBtn.setVisible(isHost);
            startBtn.setOnAction(e -> {
                long seed = new java.util.Random().nextLong();
                Map<String, Object> payload = new HashMap<>();
                payload.put("seed", seed);
                payload.put("players", lobbyPlayers);

                networkClient.sendAction(GameAction.START_GAME, roomCode, payload);
            });

            getChildren().addAll(title, subtitle, playerList, startBtn);

            // Handle incoming lobby events
            networkClient.setOnMessageReceived(message ->
                    javafx.application.Platform.runLater(() -> {
                        Map<String, Object> payload = message.content();

                        switch (message.type()) {
                            case PLAYER_JOINED -> {
                                String newPlayer = (String) payload.get("PlayerNameWithID");
                                lobbyPlayers.add(newPlayer);
                                playerList.appendText("- " + newPlayer + " has joined!\n");
                            }

                            // אם נוסיף בעתיד PLAYER_DISCONNECTED, נטפל בו פה

                            case GAME_STARTED -> {
                                networkClient.setRoomId(message.roomId());

                                long seed = ((Number) payload.get("seed")).longValue();
                                @SuppressWarnings("unchecked")
                                List<String> playersList = (List<String>) payload.get("players");

                                networkClient.setGameSeed(seed);
                                onStartGame.accept(buildPlayerSetups(playersList), networkClient);
                            }
                            default -> {}
                        }
                    })
            );
        }

        private List<PlayerSetup> buildPlayerSetups(List<String> playerNames) {
            List<PlayerSetup> players = new ArrayList<>();
            for (int i = 0; i < playerNames.size(); i++) {
                double hue = i * (360.0 / playerNames.size());
                players.add(new PlayerSetup(playerNames.get(i), Color.hsb(hue, 0.85, 0.9), "Human"));
            }
            return players;
        }
    }

    // =========================================================================
    //  Inner class: PlayerRow — one row in the player configuration table
    // =========================================================================

    static class PlayerRow extends HBox {
        private final TextField   nameField;
        private final ColorPicker colorPicker;
        private final ComboBox<String> typeBox;

        public PlayerRow(int playerNum, Color defaultColor) {
            setSpacing(15);
            setAlignment(Pos.CENTER);

            Label lbl = new Label("Player " + playerNum + ":");
            lbl.setTextFill(Color.WHITE);
            lbl.setFont(Font.font("Segue UI", FontWeight.BOLD, 16));
            lbl.setPrefWidth(80);

            nameField   = new TextField("General " + playerNum);
            nameField.setPrefWidth(150);

            colorPicker = new ColorPicker(defaultColor);
            colorPicker.setStyle("-fx-color-label-visible: false;");

            typeBox = new ComboBox<>();
            typeBox.getItems().addAll("None", "Human", "AI - Balanced", "AI - Defensive", "AI - Offensive");
            typeBox.setValue(playerNum == 1 ? "Human" : playerNum == 2 ? "AI - Balanced" : "None");
            typeBox.setOnAction(e -> {
                boolean disabled = typeBox.getValue().equals("None");
                nameField.setDisable(disabled);
                colorPicker.setDisable(disabled);
            });

            getChildren().addAll(lbl, nameField, colorPicker, typeBox);
        }

        public String getName()            { return nameField.getText(); }
        public Color  getColor()           { return colorPicker.getValue(); }
        public String getType()            { return typeBox.getValue(); }
        public void   setName(String name) { nameField.setText(name); }
    }
}