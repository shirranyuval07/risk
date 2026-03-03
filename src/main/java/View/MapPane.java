package View;

import Model.Board;
import Model.Country;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MapPane extends Pane {
    private final Board board;

    // קבוצה (Group) שתכיל את כל חלקי המפה כדי שנוכל להגדיל/להקטין הכל יחד!
    private final Group mapGroup = new Group();

    // מילונים לשמירת הגישה לאובייקטים הגרפיים שלנו כדי שנוכל לעדכן אותם
    private final Map<Country, SVGPath> countryPolygons = new HashMap<>();
    private final Map<Country, Text> armyTexts = new HashMap<>();
    private final Map<Country, Circle> armyDisks = new HashMap<>();

    private Consumer<Country> onCountryClickListener;
    private Country selectedCountry;

    public MapPane(Board board) {
        this.board = board;

        // 1. רקע האוקיינוס
        Stop[] stops = new Stop[] { new Stop(0, Color.rgb(50, 180, 225)), new Stop(1, Color.rgb(15, 125, 185)) };
        LinearGradient oceanGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(oceanGradient, null, null)));

        // הוספת הקבוצה שמכילה את המפה לפאנל שלנו
        getChildren().add(mapGroup);

        // 2. הפעלת מנגנון זום אוטומטי בהתאם לגודל החלון!
        setupScaling();

        // 3. יצירת כל המדינות, הצבעים והצבאות
        initializeCountries();
    }

    private void setupScaling() {
        // המפה שלנו בנויה במקור על קנבס של בערך 1060 על 700
        double baseWidth = 1060.0;
        double baseHeight = 700.0;

        // חישוב יחס ההגדלה (Scale) - לוקח את המינימום מבין הרוחב והגובה כדי לשמור על פרופורציות
        NumberBinding scale = Bindings.min(
                widthProperty().divide(baseWidth),
                heightProperty().divide(baseHeight)
        );

        mapGroup.scaleXProperty().bind(scale);
        mapGroup.scaleYProperty().bind(scale);

        // מירכוז המפה באמצע המסך
        mapGroup.translateXProperty().bind(widthProperty().subtract(baseWidth).divide(2));
        mapGroup.translateYProperty().bind(heightProperty().subtract(baseHeight).divide(2));
    }

    private void initializeCountries() {
        DropShadow dropShadow = new DropShadow(5.0, 3.0, 3.0, Color.rgb(0, 0, 0, 0.5));

        for (Country c : board.getCountries()) {
            // א. יצירת הפוליגון (המדינה עצמה)
            SVGPath poly = c.getShape();
            poly.setStroke(Color.rgb(15, 15, 15));
            poly.setStrokeWidth(2.0);
            poly.setEffect(dropShadow);

            // אינטראקציה של העכבר
            poly.setOnMouseEntered(e -> {
                poly.setCursor(Cursor.HAND);
                poly.setOpacity(0.8); // אפקט הבהרה עדין
            });
            poly.setOnMouseExited(e -> poly.setOpacity(1.0));
            poly.setOnMouseClicked(e -> {
                if (onCountryClickListener != null) onCountryClickListener.accept(c);
            });

            countryPolygons.put(c, poly);
            mapGroup.getChildren().add(poly); // מוסיפים למפה
        }

        // ב. יצירת ה"דיסקיות" והמספרים של הצבאות (אחרי המדינות, כדי שיופיעו מעליהן)
        for (Country c : board.getCountries()) {
            // עיגול הרקע של הצבא
            Circle disk = new Circle(c.getX(), c.getY(), 12);
            disk.setStroke(Color.BLACK);
            disk.setStrokeWidth(1.5);
            disk.setMouseTransparent(true); // כדי שלחיצה על הטקסט תעבור למדינה שמתחתיו

            // צל קטן לדיסקית שייראה תלת-ממדי
            DropShadow tokenShadow = new DropShadow(3.0, 1.0, 1.0, Color.rgb(0, 0, 0, 0.7));
            disk.setEffect(tokenShadow);

            // הטקסט של המספר
            Text text = new Text(c.getX(), c.getY(), "0");
            text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            text.setFill(Color.WHITE);
            text.setTextOrigin(VPos.CENTER);
            text.setTextAlignment(TextAlignment.CENTER);
            // מירכוז מדויק של הטקסט
            text.setX(c.getX() - text.getLayoutBounds().getWidth() / 2);
            text.setMouseTransparent(true);

            armyDisks.put(c, disk);
            armyTexts.put(c, text);

            mapGroup.getChildren().addAll(disk, text); // הוספה למפה
        }

        // צביעה ראשונית
        refreshMap();
    }

    // פונקציות לתקשורת עם הקונטרולר
    public void setOnCountryClick(Consumer<Country> listener) {
        this.onCountryClickListener = listener;
    }

    public void setSelectedCountry(Country c) {
        this.selectedCountry = c;
        refreshMap(); // מרענן כדי להדגיש את המדינה הנבחרת
    }

    // הפונקציה החשובה ביותר: מעדכנת את הצבעים והמספרים בכל תור
    public void refreshMap() {
        for (Country c : board.getCountries()) {
            SVGPath poly = countryPolygons.get(c);
            Circle disk = armyDisks.get(c);
            Text text = armyTexts.get(c);

            // 1. עדכון צבע המדינה לפי השחקן (אם אין שחקן, שים צבע בסיס של היבשת)
            Color baseColor;
            if (c.getOwner() != null) {
                baseColor = c.getOwner().getColor();
            } else {
                baseColor = c.getContinent().getColor().desaturate(); // קצת אפור אם ריק
            }

            // אם המדינה נבחרה, תאיר אותה בצהוב!
            if (c.equals(selectedCountry)) {
                poly.setStroke(Color.YELLOW);
                poly.setStrokeWidth(4.0);
            } else {
                poly.setStroke(Color.rgb(15, 15, 15));
                poly.setStrokeWidth(2.0);
            }

            poly.setFill(baseColor);

            // 2. עדכון צבע הדיסקית והמספר
            if (c.getOwner() != null) {
                disk.setFill(c.getOwner().getColor().darker());
            } else {
                disk.setFill(Color.GRAY);
            }

            text.setText(String.valueOf(c.getArmies()));
            text.setX(c.getX() - text.getLayoutBounds().getWidth() / 2); // תיקון מירכוז אם המספר גדל ל-10+
        }
    }
}