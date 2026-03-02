package View;

import Model.RiskGame;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/**
 * רכיב התצוגה הראשי המכיל את המפה ואת פאנל השליטה התחתון.
 */
public class GameRoot extends BorderPane {

    private final MapPane mapPane;
    private final ControlPane controlPane; // גרסת ה-JavaFX של ControlPanel

    public GameRoot(RiskGame game) {
        // הגדרת צבע רקע כהה לכל המסך (כמו שהיה לך ב-Swing)
        BackgroundFill bgFill = new BackgroundFill(Color.rgb(8, 16, 35), CornerRadii.EMPTY, Insets.EMPTY);
        setBackground(new Background(bgFill));

        // אתחול הרכיבים
        this.mapPane = new MapPane(game.getBoard());
        this.controlPane = new ControlPane(); // ניצור את זה בשלב הבא

        // סידור הרכיבים על המסך (מקביל ל-BorderLayout)
        setCenter(mapPane);
        setBottom(controlPane);
    }

    public MapPane getMapPane() {
        return mapPane;
    }

    public ControlPane getControlPane() {
        return controlPane;
    }
}