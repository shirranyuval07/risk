package com.example.demo.view.dialog;

import com.example.demo.model.Card;
import com.example.demo.model.Player;
import com.example.demo.model.Records.GameRecords.BattleResult;
import javafx.scene.control.*;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
// תוסיף כאן את שאר ה-Imports שתצטרך (כמו BattleResult, Player וכו')

public class DialogManager {

    // מונע יצירת מופעים של המחלקה - משתמשים רק במתודות הסטטיות
    private DialogManager() {}

    public static void showRulesDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Risk Rules");
        alert.setHeaderText("How to Play Risk: Global Conquest");

        String rulesText = """
                1. SETUP PHASE:
                Players take turns placing 1 army at a time on their territories until all starting armies are placed.

                2. DRAFT PHASE:
                At the start of your turn, you receive new armies based on the number of territories you own (minimum 3).
                Owning a whole continent gives you bonus armies! Place these armies on your territories.

                3. ATTACK PHASE:
                You can attack enemy territories that are connected to yours.
                You must have at least 2 armies in the attacking territory.
                Attacker rolls up to 3 dice, Defender rolls up to 2 dice. Highest dice are compared.

                4. FORTIFY PHASE:
                Move any number of armies from one of your territories to another connected territory you own.
                You must leave at least 1 army behind. This ends your turn.

                WINNING:
                Conquer all territories on the map to win the game!
                """;

        TextArea textArea = new TextArea(rulesText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(450);
        textArea.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        // מחליפים את התוכן הרגיל בתיבת טקסט יפה ונגללת
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    public static Optional<Pair<String, String>> showLoginDialog(String title) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Please enter your credentials:");

        // 2. הוספת כפתורים לחלון (למשל כפתור "אישור" וכפתור "ביטול")
        // המשימה שלך: ליצור ButtonType עבור אישור (למשל "Submit") ולהוסיף אותו לדיאלוג.
        ButtonType submitButton = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButton, cancelButton);


        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        GridPane grid = new GridPane();
        grid.add(new Label("Username:"), 0, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(usernameField, 1, 0);
        grid.add(passwordField, 1, 1);
        dialog.getDialogPane().setContent(grid);




        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButton) {
                return new Pair<>(usernameField.getText(), passwordField.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public static void showCardsDialog(Player player, Runnable onTradeSuccessful) {
        int inf = Collections.frequency(player.getCards(), Card.INFANTRY);
        int cav = Collections.frequency(player.getCards(), Card.CAVALRY);
        int art = Collections.frequency(player.getCards(), Card.ARTILLERY);

        String content = String.format("""
                You currently have:
                %d x Infantry (Soldier)
                %d x Cavalry (Horse)
                %d x Artillery (Cannon)
                
                Trade Values:
                3 Infantry = 4 Armies
                3 Cavalry = 6 Armies
                3 Artillery = 8 Armies
                1 of Each = 10 Armies
                """, inf, cav, art);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Your Cards");
        alert.setHeaderText("Commander " + player.getName() + "'s Hand");
        alert.setContentText(content);

        // אם יש לשחקן סט חוקי, אנחנו מוסיפים כפתור טרייד!
        ButtonType btnTrade = new ButtonType("Trade Best Set");
        if (hasValidSet(inf, cav, art)) {
            alert.getButtonTypes().add(btnTrade);
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == btnTrade) {
                int bonusArmies = player.tradeAnyValidSet();
                player.setDraftArmies(player.getDraftArmies()+bonusArmies); // הוספת החיילים לשחקן

                Alert success = new Alert(Alert.AlertType.INFORMATION, "Success! You received " + bonusArmies + " armies!");
                success.show();

                onTradeSuccessful.run(); // רענון המסך אחרי העסקה
            }
        });
    }
    private static boolean hasValidSet(int inf, int cav, int art) {
        return (inf > 0 && cav > 0 && art > 0) || inf >= 3 || cav >= 3 || art >= 3;
    }
    public static void showBattleResultDialog(BattleResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Battle Results");
        alert.setHeaderText("The battle is over!");

        VBox content = new VBox(10); // קונטיינר אנכי עם ריווח של 10 פיקסלים

        Integer[] aResults = result.attackerRolls();
        Integer[] dResults = result.defenderRolls();

        String aText = Arrays.toString(aResults);
        String dText = Arrays.toString(dResults);

        Label aLabel = new Label("Attacker Rolls: " + aText);
        Label dLabel = new Label("Defender Rolls: " + dText);

        Label lossesLabel = new Label("Attacker Losses: " + result.attackerLosses() + ", Defender Losses: " + result.defenderLosses());
        Label conquered;
        content.getChildren().addAll(aLabel, dLabel, lossesLabel);

        // לאחר מכן מוסיפים את רכיב הבונוס רק אם התנאי מתקיים
        if(result.conquered()) {
            conquered = new Label("Country Conquered!");
            conquered.setEffect(new Bloom());
            // כאן אנו משתמשים ב-add במקום addAll כי זה רק רכיב אחד
            content.getChildren().add(conquered);
        }

        alert.getDialogPane().setContent(content);
        alert.showAndWait(); // עוצר את התהליך (Blocking) עד שהשחקן מאשר
    }
}