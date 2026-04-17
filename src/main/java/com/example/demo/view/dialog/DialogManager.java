package com.example.demo.view.dialog;

import com.example.demo.model.manager.Card;
import com.example.demo.model.manager.Player;
import com.example.demo.model.Records.GameRecords.BattleResult;
import javafx.scene.control.*;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.VBox;

import java.util.*;
// תוסיף כאן את שאר ה-Imports שתצטרך (כמו BattleResult, Player וכו')

public class DialogManager {

    // מונע יצירת מופעים של המחלקה - משתמשים רק במתודות הסטטיות
    private DialogManager() {}

    public static void showRulesDialog()
    {
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


    public static void showCardsDialog(Player player, Runnable onTradeSuccessful)
    {
        Dialog<List<Card>> dialog = new Dialog<>();
        dialog.setTitle("Your Cards");
        dialog.setHeaderText("Commander " + player.getName() + "'s Hand\nSelect exactly 3 cards to trade:");

        // יצירת כפתורי הדיאלוג
        ButtonType tradeButtonType = new ButtonType("Trade Selected", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(tradeButtonType, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        List<CheckBox> checkBoxes = new ArrayList<>();

        // יצירת CheckBox עבור כל קלף שיש לשחקן ביד
        for (Card card : player.getCards())
        {
            CheckBox cb = new CheckBox(card.name());
            cb.setUserData(card); // שומרים את האובייקט של הקלף מאחורי הקלעים
            checkBoxes.add(cb);
            vbox.getChildren().add(cb);
        }

        if (player.getCards().isEmpty())
            vbox.getChildren().add(new Label("You don't have any cards yet."));


        dialog.getDialogPane().setContent(vbox);

        // חסימת כפתור הטרייד כברירת מחדל
        javafx.scene.Node tradeButton = dialog.getDialogPane().lookupButton(tradeButtonType);
        tradeButton.setDisable(true);

        // הוספת מאזין (Listener) שפותח את הכפתור רק אם נבחרו בדיוק 3 קלפים
        for (CheckBox cb : checkBoxes)
        {
            cb.selectedProperty().addListener((obs, wasSelected, isNowSelected) ->
            {
                long count = checkBoxes.stream().filter(CheckBox::isSelected).count();
                tradeButton.setDisable(count != 3);
            });
        }

        // המרת הלחיצה לרשימה של הקלפים שנבחרו
        dialog.setResultConverter(dialogButton ->
        {
            if (dialogButton == tradeButtonType) {
                List<Card> selected = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isSelected()) {
                        selected.add((Card) cb.getUserData());
                    }
                }
                return selected;
            }
            return null;
        });

        // טיפול בתוצאה כשהחלון נסגר
        dialog.showAndWait().ifPresent(selectedCards ->
        {
            Card.Service cardService = new Card.Service();
            int bonusArmies = cardService.tradeSpecificCards(player, selectedCards);

            if (bonusArmies > 0)
            {
                player.setDraftArmies(player.getDraftArmies() + bonusArmies);
                Alert success = new Alert(Alert.AlertType.INFORMATION, "Success! You received " + bonusArmies + " armies!");
                success.show();
                onTradeSuccessful.run(); // רענון המסך
            }
            else
            {
                Alert error = new Alert(Alert.AlertType.ERROR, "Invalid card combination! Trade failed.");
                error.show();
            }
        });
    }
    public static void showBattleResultDialog(BattleResult result)
    {
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
        if(result.conquered())
        {
            conquered = new Label("Country Conquered!");
            conquered.setEffect(new Bloom());
            // כאן אנו משתמשים ב-add במקום addAll כי זה רק רכיב אחד
            content.getChildren().add(conquered);
        }

        alert.getDialogPane().setContent(content);
        alert.showAndWait(); // עוצר את התהליך (Blocking) עד שהשחקן מאשר
    }
}