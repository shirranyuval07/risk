package View;

import Model.Board;
import Model.Country;
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

    private final Map<Country, CountryView> countryViews = new HashMap<>();
    private Consumer<Country> onCountryClickListener;
    private Country selectedCountry;

    public MapPane(Board board) {
        this.board = board;
        setupBackground();

        // הוספת השכבות לפי הסדר: קודם מדינות, אז סמלים
        mapGroup.getChildren().addAll(shapesLayer, symbolsLayer);
        getChildren().add(mapGroup);

        setupScaling();
        initializeCountries();
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
            if (entry.getKey().equals(selectedCountry)) entry.getValue().setHighlight(Color.YELLOW, 4.0);
            else entry.getValue().setHighlight(Color.rgb(15, 15, 15), 2.0);
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