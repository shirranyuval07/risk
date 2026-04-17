package com.example.demo.view;


import com.example.demo.config.GameConstants;
import com.example.demo.model.States.GameState.SetupState;



import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import com.example.demo.model.manager.RiskGame;
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

/**
 * PlayerStatsPane - פאנל סטטיסטיקות הנמצא בצד הימני של ממשק המשחק
 * 
 * תפקידיה:
 * - הצגת רשימת כל השחקנים וסטטיסטיקות שלהם
 * - עדכון מידע בזמן אמת (binding JavaFX)
 * - הדגשת השחקן הנוכחי (תור שלו)
 * - הצגת מספר טריטוריות וחיילים לכל שחקן
 * - זיהוי שחקנים שהורחקו מהמשחק
 * 
 * עיצוב:
 * - רקע כהה שקוף (כחול-שחור)
 * - צבע שונה לכל שחקן
 * - פס צד בצבע שחקן (סימן זיהוי)
 * 
 * השימוש: עקיבה אחרי מצב המשחק וסטטיסטיקות
 */
public class PlayerStatsPane extends VBox
{
    private final RiskGame game;

    public PlayerStatsPane(RiskGame game)
    {
        this.game = game;
        setPadding(new Insets(20));
        setSpacing(15);
        setPrefWidth(220); // רוחב הפאנל

        // רקע כהה חצי-שקוף שמשתלב יפה עם הים
        setBackground(new Background(new BackgroundFill(Color.rgb(10, 20, 40, 0.85), CornerRadii.EMPTY, Insets.EMPTY)));

        updateStats();
    }

    public void updateStats()
    {
        getChildren().clear();

        Label title = new Label("LEADERBOARD");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, GameConstants.FONT_SIZE_TITLE));
        title.setTextFill(Color.WHITE);
        getChildren().add(title);

        List<Player> players = game.getPlayers();

        for (Player p : players)
        {
            // חישוב סטטיסטיקות מדויקות מהמודל
            long territories = game.getBoard().getCountries().stream()
                    .filter(c -> c.getOwner() != null && c.getOwner().equals(p))
                    .count();

            int totalArmies = game.getBoard().getCountries().stream()
                    .filter(c -> c.getOwner() != null && c.getOwner().equals(p))
                    .mapToInt(Country::getArmies)
                    .sum(); // כולל חיילים שעדיין לא הוצבו

            // בניית הקוביה של השחקן
            VBox playerBox = new VBox(GameConstants.UI_SMALL_SPACING);
            playerBox.setPadding(new Insets(GameConstants.BOX_PADDING));

            // עיצוב: פס צד בצבע של השחקן, ורקע מעט מואר אם זה התור שלו
            String borderColor = toHexString(p.getColor());
            String bgColor = p.equals(game.getCurrentPlayer()) ? "rgba(255,255,255," + GameConstants.OVERLAY_OPACITY_MEDIUM + ")" : "rgba(255,255,255," + GameConstants.OVERLAY_OPACITY_LOW + ")";
            playerBox.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 5; -fx-background-color: " + bgColor + ";");

            Label nameLabel = new Label(p.getName());
            nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, GameConstants.FONT_SIZE_HEADER));
            nameLabel.setTextFill(p.getColor().brighter());

            Label statsLabel = new Label("Territories: " + territories + "\nTotal Armies: " + totalArmies);
            statsLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, GameConstants.FONT_SIZE_BODY));
            statsLabel.setTextFill(Color.LIGHTGRAY);

            // טיפול בשחקן שהפסיד (הודח מהמשחק)
            if (territories == 0 && totalArmies == 0 && !(game.getCurrentState() instanceof SetupState))
            {
                nameLabel.setText(p.getName() + " (ELIMINATED)");
                nameLabel.setTextFill(Color.GRAY);
                playerBox.setStyle("-fx-border-color: gray; -fx-border-width: 0 0 0 5; -fx-background-color: rgba(0,0,0," + GameConstants.OVERLAY_OPACITY_HIGH + ");");
            }

            playerBox.getChildren().addAll(nameLabel, statsLabel);
            getChildren().add(playerBox);
        }
    }

    // פונקציית עזר להמרת צבע JavaFX לקוד Hex עבור ה-CSS
    private String toHexString(Color color)
    {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * GameConstants.COLOR_HEX_MAX),
                (int) (color.getGreen() * GameConstants.COLOR_HEX_MAX),
                (int) (color.getBlue() * GameConstants.COLOR_HEX_MAX));
    }
}