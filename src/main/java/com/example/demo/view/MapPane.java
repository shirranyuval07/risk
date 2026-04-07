package com.example.demo.view;

import com.example.demo.config.GameConstants;
import com.example.demo.model.manager.Board;
import com.example.demo.model.manager.Country;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Line;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * MapPane - בחלון ראשי של משחק Risk עם המפה האינטראקטיבית
 * 
 * תפקידיה:
 * - ביצוע מדינות על המפה דרך CountryView
 * - תיווך ענייני אפילות של משחקנים במדינות
 * - ציור קשתות ימיות בין מדינות המחוברות
 * - הדגשת מדינות (בחור, זמינות, גבול יבשתי)
 * - קנה מידה וניהול מצפה המפה
 * - שילוב שכבות (מדינות, צבאות, נתיבים)
 * 
 * ויזואליקה:
 * - רקע אוקיינוס עם גרדיאנט כחול
 * - SVG המדינות מ-Board.json
 * - סימנים למספר חיילים בכל מדינה
 * 
 * השימוש: המרכז הוויזואלי של משחק Risk
 */
public class MapPane extends Pane {
    private final Board board;
    private final Group mapGroup = new Group();

    // הגדרת השכבות
    private final Group shapesLayer = new Group();  // שכבת המדינות (למטה)
    private final Group symbolsLayer = new Group(); // שכבת הצבאות (למעלה)

    private final Group routesLayer = new Group();
    private final Group continentsLayer = new Group();


    private final Map<Country, CountryView> countryViews = new HashMap<>();
    private Consumer<Country> onCountryClickListener;
    private Country selectedCountry;

    public MapPane(Board board) {
        this.board = board;
        setupBackground();

        // הוספת השכבות לפי הסדר: קודם מדינות, אז סמלים
        mapGroup.getChildren().addAll(routesLayer, continentsLayer, shapesLayer, symbolsLayer);
        getChildren().add(mapGroup);

        setupScaling();
        initializeCountries();
        initializeSeaRoutes();
    }
    /**
     * אתחול כל נתיבי הים בחזון המפה
     * יצורים קשתות מחוברות בין מדינות סמוכות
     * זרימה:
     * 1. קו לכל קשת קרקעית (מ-Board.json)
     * 2. קו מיוחד לאלסקה-קמצ'טקה (עוקף את היבל)
     */
    private void initializeSeaRoutes() {
        // רשימת כל הנתיבים הימיים (צמדי מדינות מחוברות)
        int[][] seaRoutes = {
                // האוקיינוס האטלנטי
                {3, 14}, {14, 17}, {14, 15}, {17, 15}, {17, 18}, {17, 19},
                // הים התיכון
                {18, 21}, {20, 21}, {20, 22},
                // דרום אטלנטי
                {12, 21},
                // אפריקה ודרומה
                {23, 26}, {25, 26},
                // הים האדום
                {36, 23},
                // אסיה
                {33, 30}, {33, 32},
                // אוקיינוס הודי
                {38, 39}, {39, 40}, {39, 41}, {40, 42}
        };

        // צייר כל נתיב ימי רגיל
        for (int[] route : seaRoutes) {
            drawSeaRoute(route[0], route[1]);
        }

        // טיפול מיוחד: אלסקה-קמצ'טקה (עוקף את היבל)
        drawAlaskaKamchatkaRoute();
    }

    /**
     * עוזר: צייר קו ימי בין שתי מדינות
     */
    private void drawSeaRoute(int countryId1, int countryId2) {
        Country country1 = board.getCountry(countryId1);
        Country country2 = board.getCountry(countryId2);

        if (country1 != null && country2 != null) {
            Line seaRouteLine = new Line(country1.getX(), country1.getY(), 
                                        country2.getX(), country2.getY());
            styleSeaRoute(seaRouteLine);
            routesLayer.getChildren().add(seaRouteLine);
        }
    }

    /**
     * עוזר: צייר קו ייעודי לנתיב אלסקה-קמצ'טקה שעוקף את היבל
     * זה דורש שני קטעים: מאלסקה שמאלה ומקמצ'טקה ימינה
     */
    private void drawAlaskaKamchatkaRoute() {
        Country alaska = board.getCountry(GameConstants.ALASKA_ID);
        Country kamchatka = board.getCountry(GameConstants.KAMCHATKA_ID);

        if (alaska != null && kamchatka != null) {
            // קו מאלסקה לשמאל החיצוני (מיתאר את קצה המפה)
            Line alaskaLine = new Line(alaska.getX(), alaska.getY(),
                                      alaska.getX() + GameConstants.ALASKA_ROUTE_OFFSET_X,
                                      alaska.getY() + GameConstants.ALASKA_ROUTE_OFFSET_Y);
            styleSeaRoute(alaskaLine);

            // קו מקמצ'טקה לימין החיצוני (מיתאר את קצה המפה)
            Line kamchatkaLine = new Line(kamchatka.getX(), kamchatka.getY(),
                                         kamchatka.getX() + GameConstants.KAMCHATKA_ROUTE_OFFSET_X,
                                         kamchatka.getY() + GameConstants.KAMCHATKA_ROUTE_OFFSET_Y);
            styleSeaRoute(kamchatkaLine);

            routesLayer.getChildren().addAll(alaskaLine, kamchatkaLine);
        }
    }

    /**
     * עוזר: עיצוב ויזואלי של קו ימי
     * צבע לבן, חצי שקוף, עם דפוס מקווקו
     */
    private void styleSeaRoute(Line line) {
        line.setStroke(Color.WHITE);
        line.setStrokeWidth(GameConstants.SEA_ROUTE_WIDTH);
        line.setOpacity(GameConstants.SEA_ROUTE_OPACITY);
        line.getStrokeDashArray().addAll(GameConstants.SEA_ROUTE_DASH_LENGTH, GameConstants.SEA_ROUTE_GAP_LENGTH);
    }
    public void toggleNames(boolean show) {
        for (CountryView cv : countryViews.values()) {
            cv.getNameText().setVisible(show);
        }
    }
    private void setupBackground() {
        Stop[] stops = new Stop[]{new Stop(0, Color.rgb(50, 180, 225)), new Stop(1, Color.rgb(15, 125, 185))};
        LinearGradient oceanGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(oceanGradient, null, null)));
    }

    private void setupScaling() {
        double baseWidth = 1060.0;
        double baseHeight = 700.0;
        NumberBinding scale = Bindings.min(widthProperty().divide(baseWidth), heightProperty().divide(baseHeight));
        mapGroup.scaleXProperty().bind(scale);
        mapGroup.scaleYProperty().bind(scale);
        mapGroup.translateXProperty().bind(widthProperty().subtract(baseWidth).divide(2));
        mapGroup.translateYProperty().bind(heightProperty().subtract(baseHeight).divide(2));
    }

    private void initializeCountries() {
        for (Country c : board.getCountries()) {
            // יצירת המודול ושליחתו לשכבות הנכונות
            CountryView cv = new CountryView(c, shapesLayer, symbolsLayer);

            SVGPath shape = cv.getShape();
            shape.setOnMouseEntered(e -> { shape.setCursor(Cursor.HAND); shape.setOpacity(0.8); });
            shape.setOnMouseExited(e -> shape.setOpacity(1.0));
            shape.setOnMouseClicked(e -> { if (onCountryClickListener != null) onCountryClickListener.accept(c); });

            countryViews.put(c, cv);
        }
        refreshMap();
    }

    /**
     * רענון כל תצוגות המדינות בעדכון סגנון הדגשה
     * לוגיקה: בחור → צהוב | תפר יבשת → גבול עבה | רגילה → דק
     */
    public void refreshMap() {
        for (Map.Entry<Country, CountryView> entry : countryViews.entrySet()) {
            Country country = entry.getKey();
            CountryView view = entry.getValue();

            if (country.equals(selectedCountry)) {
                // מדינה שנבחרה - הדגש חזק בצהוב
                view.setHighlight(Color.YELLOW, GameConstants.BORDER_WIDTH_HIGHLIGHT);
            } else {
                // בדוק אם זו מדינת תפר (אסיה-אירופה)
                boolean isBoundaryRegion = isContinentBoundary(country);
                
                if (isBoundaryRegion) {
                    // מדינת תפר - גבול עבה וברור
                    view.setHighlight(Color.rgb(15, 15, 15, GameConstants.OVERLAY_OPACITY_HIGH), 
                                    GameConstants.BORDER_WIDTH_SPECIAL);
                } else {
                    // מדינה רגילה - גבול דק וממוצע
                    view.setHighlight(Color.rgb(15, 15, 15, GameConstants.OVERLAY_OPACITY_LOW), 
                                    GameConstants.BORDER_WIDTH_NORMAL);
                }
            }
        }
    }

    /**
     * עוזר: בדוק אם מדינה היא על גבול בין יבשתות (בעיקר אסיה-אירופה)
     */
    private boolean isContinentBoundary(Country country) {
        if (country.getContinent() == null) return false;
        
        String countryContinent = country.getContinent().getName();
        if (!"Asia".equals(countryContinent)) return false;
        
        // בדוק אם יש לה שכן אירופאי
        for (Country neighbor : country.getNeighbors()) {
            if (neighbor.getContinent() != null && "Europe".equals(neighbor.getContinent().getName())) {
                return true;
            }
        }
        return false;
    }

     public void highlightTargets(Set<Country> validTargets) {
         clearHighlights();
         for (Country c : validTargets) {
             if (countryViews.containsKey(c)) countryViews.get(c).setHighlight(Color.GREEN, GameConstants.BORDER_WIDTH_HIGHLIGHT);
         }
     }

    public void clearHighlights() { refreshMap(); }
    public void setOnCountryClick(Consumer<Country> listener) { this.onCountryClickListener = listener; }
    public void setSelectedCountry(Country c) { this.selectedCountry = c; refreshMap(); }
}