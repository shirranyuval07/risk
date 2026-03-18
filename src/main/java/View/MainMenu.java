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

public class MainMenu extends StackPane {

    public record PlayerSetup(String name, Color color, String type) {}

    private final List<PlayerRow> playerRows = new ArrayList<>();

    public MainMenu(Consumer<List<PlayerSetup>> onStartGame) {
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

        Button btnStart = new Button("START CONQUEST");
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

        mainContent.getChildren().addAll(title, playersBox, btnStart);

        // --- יצירת כפתור החוקים המרחף בפינה ---
        Button btnRules = new Button("📖 Rules");
        btnRules.setStyle("-fx-background-color: #4a6a92; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnRules.setCursor(javafx.scene.Cursor.HAND);
        btnRules.setOnAction(e -> RulesDialog.show());

        StackPane.setAlignment(btnRules, Pos.TOP_RIGHT);
        StackPane.setMargin(btnRules, new Insets(20));

        // הוספת התפריט הראשי וכפתור החוקים לשכבות
        getChildren().addAll(mainContent, btnRules);
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