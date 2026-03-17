package View;

import Model.RiskGame;
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

    @Getter private final Button btnNextPhase;
    @Getter private final Button btnCards;

    private final Label messageLabel;
    private final Label playerLabel;
    private final Label phaseLabel;
    private final Label armiesLabel;

    public ControlPane(RiskGame game) {
        setPadding(new Insets(15));
        setSpacing(30);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #1a2a42; -fx-border-color: #3c64aa; -fx-border-width: 2 0 0 0;");

        playerLabel = createStyledLabel("Player: -");
        phaseLabel = createStyledLabel("Phase: -");
        armiesLabel = createStyledLabel("Draft Armies: 0");
        messageLabel = createStyledLabel("Welcome to Risk!");

        VBox infoBox = new VBox(5, playerLabel, phaseLabel, armiesLabel);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        btnNextPhase = new Button("Next Phase");
        btnCards = new Button("🃏 Cards");

        btnNextPhase.setStyle("-fx-background-color: #2eaa50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnCards.setStyle("-fx-background-color: #dc8c28; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        getChildren().addAll(infoBox, messageLabel, btnNextPhase, btnCards);

        // --- DATA BINDING MAGIC ---
        setupBindings(game);
    }

    private void setupBindings(RiskGame game) {
        // 1. Listen for Player changes
        game.currentPlayerProperty().addListener((obs, oldPlayer, newPlayer) -> {
            if (newPlayer != null) {
                playerLabel.setText("Player: " + newPlayer.getName());

                // Unbind from the old player and bind to the NEW player's draft armies!
                armiesLabel.textProperty().unbind();
                armiesLabel.textProperty().bind(newPlayer.draftArmiesProperty().asString("Draft Armies: %d"));
            }
        });

        // 2. Listen for Phase changes
        game.currentStateProperty().addListener((obs, oldState, newState) -> {
            if (newState != null) {
                phaseLabel.setText("Phase: " + newState.getPhaseName());
            }
        });
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        return label;
    }

    public void setMessage(String msg) {
        messageLabel.setText(msg);
    }
}