package View;

import Model.RiskGame;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Getter;

@Getter
public class GameRoot extends StackPane {

    private final MapPane mapPane;
    private final ControlPane controlPane;

    public GameRoot(RiskGame game) {
        // הגדרת צבע רקע כהה לכל המסך
        BackgroundFill bgFill = new BackgroundFill(Color.rgb(8, 16, 35), CornerRadii.EMPTY, Insets.EMPTY);
        setBackground(new Background(bgFill));

        // אתחול הרכיבים
        this.mapPane = new MapPane(game.getBoard());
        this.controlPane = new ControlPane(game);

        // סידור הרכיבים על המסך הפנימי
        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(mapPane);
        mainLayout.setBottom(controlPane);

        // --- יצירת כפתור החוקים המרחף בפינה ---
        Button btnRules = new Button("📖 Rules");
        btnRules.setStyle("-fx-background-color: #4a6a92; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnRules.setCursor(javafx.scene.Cursor.HAND);
        btnRules.setOnAction(e -> RulesDialog.show()); // קריאה לחלון החוקים שלנו!

        // מיקום הכפתור בפינה הימנית-עליונה עם קצת מרווח
        StackPane.setAlignment(btnRules, Pos.TOP_RIGHT);
        StackPane.setMargin(btnRules, new Insets(20));

        // הוספת הכל לשכבות (הכפתור יושב מעל המפה)
        getChildren().addAll(mainLayout, btnRules);
    }
}