package View;

import Model.RiskGame;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import lombok.Getter;

/**
 * רכיב התצוגה הראשי המכיל את המפה ואת פאנל השליטה התחתון.
 */
@Getter
public class GameRoot extends BorderPane
{

    private final MapPane mapPane;
    private final ControlPane controlPane; // גרסת ה-JavaFX של ControlPanel

    public GameRoot(RiskGame game)
    {
        // הגדרת צבע רקע כהה לכל המסך
        BackgroundFill bgFill = new BackgroundFill(Color.rgb(8, 16, 35), CornerRadii.EMPTY, Insets.EMPTY);
        setBackground(new Background(bgFill));

        // אתחול הרכיבים
        this.mapPane = new MapPane(game.getBoard());
        this.controlPane = new ControlPane();

        // סידור הרכיבים על המסך
        setCenter(mapPane);
        setBottom(controlPane);
    }

}