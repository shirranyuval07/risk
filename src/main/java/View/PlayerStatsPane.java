package View;

import Model.Country;
import Model.Player;
import Model.RiskGame;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class PlayerStatsPane extends VBox {
    private final RiskGame game;

    public PlayerStatsPane(RiskGame game) {
        this.game = game;
        setPadding(new Insets(20));
        setSpacing(15);
        setPrefWidth(220); // רוחב הפאנל

        // רקע כהה חצי-שקוף שמשתלב יפה עם הים
        setBackground(new Background(new BackgroundFill(Color.rgb(10, 20, 40, 0.85), CornerRadii.EMPTY, Insets.EMPTY)));

        updateStats();
    }

    public void updateStats() {
        getChildren().clear();

        Label title = new Label("LEADERBOARD");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);
        getChildren().add(title);

        List<Player> players = game.getPlayers();

        for (Player p : players) {
            // חישוב סטטיסטיקות מדויקות מהמודל
            long territories = game.getBoard().getCountries().stream()
                    .filter(c -> c.getOwner() != null && c.getOwner().equals(p))
                    .count();

            int armiesOnBoard = game.getBoard().getCountries().stream()
                    .filter(c -> c.getOwner() != null && c.getOwner().equals(p))
                    .mapToInt(Country::getArmies)
                    .sum();

            int totalArmies = armiesOnBoard; // כולל חיילים שעדיין לא הוצבו

            // בניית הקוביה של השחקן
            VBox playerBox = new VBox(5);
            playerBox.setPadding(new Insets(10));

            // עיצוב: פס צד בצבע של השחקן, ורקע מעט מואר אם זה התור שלו
            String borderColor = toHexString(p.getColor());
            String bgColor = p.equals(game.getCurrentPlayer()) ? "rgba(255,255,255,0.15)" : "rgba(255,255,255,0.05)";
            playerBox.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 5; -fx-background-color: " + bgColor + ";");

            Label nameLabel = new Label(p.getName());
            nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            nameLabel.setTextFill(p.getColor().brighter());

            Label statsLabel = new Label("Territories: " + territories + "\nTotal Armies: " + totalArmies);
            statsLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
            statsLabel.setTextFill(Color.LIGHTGRAY);

            // טיפול בשחקן שהפסיד (הודח מהמשחק)
            if (territories == 0 && totalArmies == 0 && !(game.getCurrentState() instanceof Model.States.SetupState))
            {
                nameLabel.setText(p.getName() + " (ELIMINATED)");
                nameLabel.setTextFill(Color.GRAY);
                playerBox.setStyle("-fx-border-color: gray; -fx-border-width: 0 0 0 5; -fx-background-color: rgba(0,0,0,0.3);");
            }

            playerBox.getChildren().addAll(nameLabel, statsLabel);
            getChildren().add(playerBox);
        }
    }

    // פונקציית עזר להמרת צבע JavaFX לקוד Hex עבור ה-CSS
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}