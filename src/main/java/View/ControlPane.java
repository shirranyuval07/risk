package View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.Getter;

public class ControlPane extends HBox {

    @Getter
    private final Button btnNextPhase;
    @Getter
    private final Button btnCards;

    // תוויות לתצוגת מידע
    private final Label messageLabel;
    private final Label playerLabel;
    private final Label phaseLabel;
    private final Label armiesLabel;

    public ControlPane() {
        // הגדרות עיצוב כלליות לפאנל
        setPadding(new Insets(15));
        setSpacing(30);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #1a2a42; -fx-border-color: #3c64aa; -fx-border-width: 2 0 0 0;");

        // יצירת התוויות (Labels)
        playerLabel = createStyledLabel("Player: -");
        phaseLabel = createStyledLabel("Phase: -");
        armiesLabel = createStyledLabel("Armies: 0");
        messageLabel = createStyledLabel("Welcome to Risk!");

        // יצירת קופסה אנכית שתאגד את פרטי השחקן בצד שמאל
        VBox infoBox = new VBox(5, playerLabel, phaseLabel, armiesLabel);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        btnNextPhase = new Button("Next Phase");
        btnCards = new Button("🃏 Cards");

        // עיצוב כפתורים בסגנון JavaFX
        btnNextPhase.setStyle("-fx-background-color: #2eaa50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnCards.setStyle("-fx-background-color: #dc8c28; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        // הוספת כל הרכיבים לשורה (HBox) - פרטי שחקן, הודעה מרכזית, כפתורים
        getChildren().addAll(infoBox, messageLabel, btnNextPhase, btnCards);
    }

    // פונקציית עזר ליצירת טקסט מעוצב
    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        return label;
    }

    public void setMessage(String msg) {
        messageLabel.setText(msg);
    }

    // הפונקציה שהקונטרולר קורא לה כדי לעדכן את המסך
    public void updateView(String playerName, String phase, int armies) {
        playerLabel.setText("Player: " + playerName);
        phaseLabel.setText("Phase: " + phase);
        armiesLabel.setText("Draft Armies: " + armies);
    }
}