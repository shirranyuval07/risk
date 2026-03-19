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
import java.util.function.Consumer;

// ייבוא הקליינט שלנו
import com.example.demo.RiskWebSocketClient;

public class MainMenu extends StackPane {

    public record PlayerSetup(String name, Color color, String type) {}

    private final List<PlayerRow> playerRows = new ArrayList<>();

    private final Consumer<List<PlayerSetup>> onStartGame;

    public MainMenu(Consumer<List<PlayerSetup>> onStartGame)
    {
        this.onStartGame = onStartGame;
        setBackground(new Background(new BackgroundFill(Color.rgb(8, 16, 35), CornerRadii.EMPTY, Insets.EMPTY)));

        // הקונטיינר המרכזי של התפריט
        VBox mainContent = new VBox(30);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(50));

        // כותרת
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

        // --- כפתור משחק מקומי (Hot-Seat) ---
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
                onStartGame.accept(activePlayers);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "You need at least 2 players to start!");
                alert.show();
            }
        });

        // --- קוד הרשת (Multiplayer) ---

        RiskWebSocketClient networkClient = new RiskWebSocketClient("Yuval");
        networkClient.connect();

        Button createRoomBtn = new Button("CREATE ROOM");
        createRoomBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        createRoomBtn.setStyle("-fx-background-color: #0055e1; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        createRoomBtn.setCursor(javafx.scene.Cursor.HAND);

        createRoomBtn.setOnAction(e -> {
            networkClient.sendAction("CREATE_ROOM", "", "Create me a room");
        });

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

        // טיפול בתשובות שמגיעות מהשרת (כשאנחנו בתפריט)
        networkClient.setOnMessageReceived(message -> {
            if (message.type().equals("ROOM_CREATED")) {
                showLobby(message.roomId(), true, networkClient, mainContent);
            }
            else if (message.type().equals("JOIN_ROOM_SUCCESS")) {
                showLobby(message.roomId(), false, networkClient, mainContent);
            }
            else if (message.type().equals("ERROR")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(message.content());
                alert.show();
            }
        });

        HBox multiplayerBox = new HBox(20);
        multiplayerBox.setAlignment(Pos.CENTER);
        multiplayerBox.getChildren().addAll(createRoomBtn, joinRoomBtn);

        // הוספת כל הכפתורים למסך הראשי
        mainContent.getChildren().addAll(title, playersBox, btnStart, multiplayerBox);

        // --- יצירת כפתור החוקים המרחף בפינה ---
        Button btnRules = new Button("📖 Rules");
        btnRules.setStyle("-fx-background-color: #4a6a92; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnRules.setCursor(javafx.scene.Cursor.HAND);
        btnRules.setOnAction(e -> RulesDialog.show());

        StackPane.setAlignment(btnRules, Pos.TOP_RIGHT);
        StackPane.setMargin(btnRules, new Insets(20));

        // הוספת התפריט הראשי וכפתור החוקים לשכבות של ה-StackPane
        getChildren().addAll(mainContent, btnRules);
    }

    // --- פונקציית הלובי (Waiting Room) ---
    private void showLobby(String roomCode, boolean isHost, RiskWebSocketClient networkClient, VBox mainContent) {
        VBox lobbyBox = new VBox(20);
        lobbyBox.setAlignment(Pos.CENTER);

        Label title = new Label("Waiting Room: " + roomCode);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Waiting for players to join...");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 20));
        subtitle.setTextFill(Color.LIGHTGRAY);

        TextArea playerList = new TextArea("Players in room:\n- You\n");
        playerList.setEditable(false);
        playerList.setMaxWidth(400);
        playerList.setPrefHeight(200);
        playerList.setFont(Font.font("Segoe UI", 16));

        Button startMultiplayerBtn = new Button("START MULTIPLAYER GAME");
        startMultiplayerBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        startMultiplayerBtn.setStyle("-fx-background-color: #e13c3c; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 5;");
        startMultiplayerBtn.setCursor(javafx.scene.Cursor.HAND);
        startMultiplayerBtn.setVisible(isHost); // מוסתר למי שאינו המארח

        startMultiplayerBtn.setOnAction(e -> {
            networkClient.sendAction("START_GAME", roomCode, "Let's go!");
        });

        lobbyBox.getChildren().addAll(title, subtitle, playerList, startMultiplayerBtn);

        networkClient.setOnMessageReceived(message -> {
            // Platform.runLater הוא קריטי - הוא אומר לג'אווה "תעדכני את המסך עכשיו"
            javafx.application.Platform.runLater(() -> {
                if (message.type().equals("PLAYER_JOINED")) {
                    // אם השרת שלח הודעה שמישהו הצטרף, נוסיף אותו לרשימה הלבנה
                    playerList.appendText("- " + message.content() + " has joined!\n");
                    System.out.println("UI Updated: " + message.content() + " is now visible!");
                }
                else if (message.type().equals("GAME_STARTED")) {
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("Starting the game map for everyone!");

                        // יצירת רשימת שחקנים זמנית כדי שהמפה תעלה
                        // בהמשך תוכל לשלוח את הרשימה המדויקת מהשרת
                        List<PlayerSetup> players = new ArrayList<>();
                        players.add(new PlayerSetup("Online Player 1", Color.RED, "Human"));
                        players.add(new PlayerSetup("Online Player 2", Color.BLUE, "Human"));

                        // הפקודה הקריטית: זה מה שסוגר את התפריט ופותח את המפה
                        onStartGame.accept(players);
                    });
                }
            });
        });

        // הסתרת התפריט המקורי והצגת הלובי
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

        public String getName() { return nameField.getText(); }
        public Color getColor() { return colorPicker.getValue(); }
        public String getType() { return typeBox.getValue(); }
    }
}