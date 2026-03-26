package com.example.demo.view.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

public class RulesDialog {

    public static void show() {
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
}