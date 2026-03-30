package com.example.demo.view;

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
    private void initializeSeaRoutes() {
        // רשימת צמדי מדינות המחוברות בנתיב ימי (לפי ה-ID שלהן ב-Board.json)
        int[][] seaRoutes = {
                {3, 14}, {14, 17}, {14, 15}, {17, 15}, {17, 18}, {17, 19}, // האוקיינוס האטלנטי והים הצפוני
                {18, 21}, {20, 21}, {20, 22}, // הים התיכון (אירופה-אפריקה)
                {12, 21}, // דרום האוקיינוס האטלנטי (ברזיל - צפון אפריקה)
                {23, 26}, {25, 26}, // אזור מדגסקר
                {36, 23}, // ים סוף (המזרח התיכון - מזרח אפריקה)
                {33, 30}, {33, 32}, // אזור יפן
                {38, 39}, {39, 40}, {39, 41}, {40, 42} // אינדונזיה ואוסטרליה
        };

        for (int[] route : seaRoutes) {
            Country c1 = board.getCountry(route[0]);
            Country c2 = board.getCountry(route[1]);

            if (c1 != null && c2 != null) {
                // מותח קו ממרכז מדינה א' למרכז מדינה ב'
                Line line = new Line(c1.getX(), c1.getY(), c2.getX(), c2.getY());
                styleSeaRoute(line);
                routesLayer.getChildren().add(line);
            }
        }

        // --- טיפול מיוחד באלסקה וקמצ'טקה (חיבור שעוקף את העולם) ---
        Country alaska = board.getCountry(1);
        Country kamchatka = board.getCountry(30);

        if (alaska != null && kamchatka != null) {
            // קו מאלסקה שמאלה אל מחוץ למסך
            Line lineAlaska = new Line(alaska.getX(), alaska.getY(), alaska.getX() - 150, alaska.getY() + 20);
            styleSeaRoute(lineAlaska);

            // קו מקמצ'טקה ימינה אל מחוץ למסך
            Line lineKamchatka = new Line(kamchatka.getX(), kamchatka.getY(), kamchatka.getX() + 150, kamchatka.getY() - 20);
            styleSeaRoute(lineKamchatka);

            routesLayer.getChildren().addAll(lineAlaska, lineKamchatka);
        }
    }

    private void styleSeaRoute(Line line) {
        line.setStroke(Color.WHITE); // צבע הקו
        line.setStrokeWidth(2.5); // עובי
        line.setOpacity(0.5); // חצי שקוף כדי שישתלב יפה עם מי האוקיינוס ולא יבלוט מדי
        line.getStrokeDashArray().addAll(8d, 6d); // יצירת האפקט המקווקו (אורך קו: 8, אורך רווח: 6)
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

    public void refreshMap() {
        for (Map.Entry<Country, CountryView> entry : countryViews.entrySet()) {
            Country country = entry.getKey();
            CountryView view = entry.getValue();

            // אם המדינה כרגע נבחרת על ידי השחקן
            if (country.equals(selectedCountry)) {
                view.setHighlight(Color.YELLOW, 4.0);
            } else {
                // בדיקה: האם המדינה באסיה וגובלת באירופה?
                boolean isAsiaBorderingEurope = false;
                if (country.getContinent() != null && "Asia".equals(country.getContinent().getName())) {
                    for (Country neighbor : country.getNeighbors()) {
                        if (neighbor.getContinent() != null && "Europe".equals(neighbor.getContinent().getName())) {
                            isAsiaBorderingEurope = true;
                            break;
                        }
                    }
                }

                // החלת העיצוב בהתאם לבדיקה
                if (isAsiaBorderingEurope) {
                    // מדינת תפר (אסיה שנוגעת באירופה) - קו עבה יותר ובולט
                    view.setHighlight(Color.rgb(15, 15, 15, 0.85), 2.5);
                } else {
                    // מדינה רגילה - קו דק, חלש ועדין
                    view.setHighlight(Color.rgb(15, 15, 15, 0.4), 1.0);
                }
            }
        }
    }

    public void highlightTargets(Set<Country> validTargets) {
        clearHighlights();
        for (Country c : validTargets) {
            if (countryViews.containsKey(c)) countryViews.get(c).setHighlight(Color.GREEN, 4.0);
        }
    }

    public void clearHighlights() { refreshMap(); }
    public void setOnCountryClick(Consumer<Country> listener) { this.onCountryClickListener = listener; }
    public void setSelectedCountry(Country c) { this.selectedCountry = c; refreshMap(); }
}