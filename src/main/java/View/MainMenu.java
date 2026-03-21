package View;

import com.example.demo.GameMessage;
import com.example.demo.RiskWebSocketClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Pair;
import lombok.Getter;
import service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * The main menu screen.
 * Coordinates player setup rows, local/multiplayer buttons, auth, and the lobby.
 * Inner classes:
 *  - AuthBox→ login / signup / logout UI
 *  - LobbyScreen → waiting room after creating or joining a room
 *  - PlayerRow→ one row in the player configuration table
 */
public class MainMenu extends StackPane {

    public record PlayerSetup(String name, Color color, String type) {}

    // =========================================================================
    //  Fields
    // =========================================================================

    private final List<PlayerRow> playerRows = new ArrayList<>();
    private final BiConsumer<List<PlayerSetup>, RiskWebSocketClient> onStartGame;
    private final AuthBox authBox;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public MainMenu(BiConsumer<List<PlayerSetup>, RiskWebSocketClient> onStartGame,
                    UserService userService) {
        this.onStartGame = onStartGame;

        setBackground(new Background(
                new BackgroundFill(Color.rgb(8, 16, 35), CornerRadii.EMPTY, Insets.EMPTY)));

        RiskWebSocketClient networkClient = new RiskWebSocketClient("Guest");
        networkClient.connect();

        VBox mainContent = buildMainContent(networkClient);

        // AuthBox calls back when login/logout changes the player name
        this.authBox = new AuthBox(userService, networkClient,
                newName -> playerRows.getFirst().setName(newName));

        Button btnRules = buildRulesButton();

        StackPane.setAlignment(authBox, Pos.TOP_LEFT);
        StackPane.setMargin(authBox, new Insets(20));
        StackPane.setAlignment(btnRules, Pos.TOP_RIGHT);
        StackPane.setMargin(btnRules, new Insets(20));

        getChildren().addAll(mainContent, btnRules, authBox);

        // Listen for server responses to CREATE_ROOM / JOIN_ROOM
        networkClient.setOnMessageReceived(message -> {
            switch (message.type()) {
                case "ROOM_CREATED"      -> openLobby(message, true,  networkClient, mainContent);
                case "JOIN_ROOM_SUCCESS" -> openLobby(message, false, networkClient, mainContent);
                case "ERROR"             -> showError(message.content());
            }
        });
    }

    // =========================================================================
    //  Building the main content
    // =========================================================================

    private VBox buildMainContent(RiskWebSocketClient networkClient) {
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50));

        Label title = new Label("⚔ RISK: GLOBAL CONQUEST ⚔");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
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

        createBtn.setOnAction(e ->
                networkClient.sendAction("CREATE_ROOM", "", "Create me a room"));

        joinBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Join Room");
            dialog.setHeaderText("Enter the 4-character Room Code:");
            dialog.showAndWait().ifPresent(code -> {
                if (!code.trim().isEmpty())
                    networkClient.sendAction("JOIN_ROOM", code.toUpperCase(), "Let me in");
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
        btn.setOnAction(e -> RulesDialog.show());
        return btn;
    }

    // =========================================================================
    //  Lobby
    // =========================================================================

    private void openLobby(GameMessage message, boolean isHost,
                           RiskWebSocketClient networkClient, VBox mainContent) {
        String myName = (authBox.getCurrentUser() != null)
                ? authBox.getCurrentUser().getUsername()
                : networkClient.getPlayerName() + message.content();

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
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        btn.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        btn.setCursor(javafx.scene.Cursor.HAND);
        return btn;
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.show();
    }

    // =========================================================================
    //  Inner class: AuthBox — login / signup / logout panel (top-left)
    // =========================================================================

    @Getter
    private static class AuthBox extends VBox {

        private Entity.User currentUser = null;

        public AuthBox(UserService userService,
                       RiskWebSocketClient networkClient,
                       java.util.function.Consumer<String> onNameChanged) {
            setAlignment(Pos.TOP_LEFT);
            setMaxWidth(150);
            setSpacing(15);

            Label userLabel = new Label("Welcome, Guest");
            userLabel.setTextFill(Color.WHITE);
            userLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

            if (userService != null) {
                buildFullAuthUI(userService, networkClient, userLabel, onNameChanged);
            } else {
                buildClientModeUI(userLabel);
            }
        }

        private void buildFullAuthUI(UserService userService,
                                     RiskWebSocketClient networkClient,
                                     Label userLabel,
                                     java.util.function.Consumer<String> onNameChanged) {
            Button loginBtn  = new Button("Login");
            Button signupBtn = new Button("Sign Up");
            Button logoutBtn = new Button("Logout");
            logoutBtn.setVisible(false);
            logoutBtn.setManaged(false);

            getChildren().addAll(userLabel, loginBtn, signupBtn, logoutBtn);

            signupBtn.setOnAction(e -> {
                Optional<Pair<String, String>> result = LoginDialog.show("Sign Up");
                result.ifPresent(creds -> {
                    if (userService.signup(creds.getKey(), creds.getValue())) {
                        new Alert(Alert.AlertType.INFORMATION, "Registration successful! You can now login.").show();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Username already exists!").show();
                    }
                });
            });

            loginBtn.setOnAction(e -> {
                Optional<Pair<String, String>> result = LoginDialog.show("Login");
                result.ifPresent(creds -> {
                    Entity.User user = userService.login(creds.getKey(), creds.getValue());
                    if (user != null) {
                        currentUser = user;
                        userLabel.setText("Commander: " + user.getUsername());
                        networkClient.setPlayerName(user.getUsername());
                        onNameChanged.accept(user.getUsername());

                        loginBtn.setVisible(false);  loginBtn.setManaged(false);
                        signupBtn.setVisible(false); signupBtn.setManaged(false);
                        logoutBtn.setVisible(true);  logoutBtn.setManaged(true);
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Invalid username or password!").show();
                    }
                });
            });

            logoutBtn.setOnAction(e -> {
                currentUser = null;
                userLabel.setText("Welcome, Guest");
                networkClient.setPlayerName("Guest");
                onNameChanged.accept("General 1");

                loginBtn.setVisible(true);  loginBtn.setManaged(true);
                signupBtn.setVisible(true); signupBtn.setManaged(true);
                logoutBtn.setVisible(false); logoutBtn.setManaged(false);
            });
        }

        private void buildClientModeUI(Label userLabel) {
            Label clientLabel = new Label("🎮 Client Mode");
            clientLabel.setTextFill(Color.LIGHTGRAY);
            clientLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
            getChildren().addAll(userLabel, clientLabel);
        }
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

            Button startBtn = styledButton("START MULTIPLAYER GAME", "#e13c3c", 20);
            startBtn.setVisible(isHost);
            startBtn.setOnAction(e -> {
                long seed = new java.util.Random().nextLong();
                networkClient.sendAction("START_GAME", roomCode,
                        seed + ":" + String.join(",", lobbyPlayers));
            });

            getChildren().addAll(title, subtitle, playerList, startBtn);

            // Handle incoming lobby events
            networkClient.setOnMessageReceived(message ->
                    javafx.application.Platform.runLater(() -> {
                        switch (message.type()) {
                            case "PLAYER_JOINED" -> {
                                lobbyPlayers.add(message.content());
                                playerList.appendText("- " + message.content() + " has joined!\n");
                            }
                            case "PLAYER_DISCONNECTED" ->
                                    playerList.appendText("⚠ A player disconnected.\n");
                            case "GAME_STARTED" -> {
                                networkClient.setRoomId(message.roomId());
                                String[] parts = message.content().split(":");
                                networkClient.setGameSeed(Long.parseLong(parts[0]));
                                onStartGame.accept(
                                        buildPlayerSetups(parts[1].split(",")),
                                        networkClient
                                );
                            }
                        }
                    })
            );
        }

        /** Assigns each player a unique color evenly spread around the color wheel */
        private List<PlayerSetup> buildPlayerSetups(String[] playerNames) {
            List<PlayerSetup> players = new ArrayList<>();
            for (int i = 0; i < playerNames.length; i++) {
                double hue = i * (360.0 / playerNames.length);
                players.add(new PlayerSetup(playerNames[i], Color.hsb(hue, 0.85, 0.9), "Human"));
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
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
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