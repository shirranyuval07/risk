package com.example.demo.view.dialog;

import com.example.demo.model.Records.BattleResult;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.VBox;
import java.util.Arrays;

public class BattleResultDialog {

    public static void show(BattleResult result) {
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