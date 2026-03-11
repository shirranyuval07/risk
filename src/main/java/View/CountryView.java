package View;

import Model.Country;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.Getter;

public class CountryView {
    private final Country model;
    @Getter
    private final SVGPath shape;
    @Getter private final Circle armyDisk;
    @Getter private final Text armyText;

    public CountryView(Country country, Group shapesLayer, Group symbolsLayer) {
        this.model = country;

        // 1. יצירת הצורה (שכבת המפה)
        this.shape = country.getShape();
        setupShapeEffects();
        shapesLayer.getChildren().add(shape); // מוסיפים לשכבה התחתונה

        // 2. יצירת הדיסקית והטקסט (שכבת הסמלים)
        this.armyDisk = new Circle(country.getX(), country.getY(), 12);
        this.armyText = new Text(country.getX(), country.getY(), "0");
        setupSymbolEffects();

        symbolsLayer.getChildren().addAll(armyDisk, armyText); // מוסיפים לשכבה העליונה
    }

    private void setupShapeEffects() {
        shape.setStroke(Color.rgb(15, 15, 15));
        shape.setStrokeWidth(2.0);
        shape.setEffect(new DropShadow(5.0, 3.0, 3.0, Color.rgb(0, 0, 0, 0.5)));
    }

    private void setupSymbolEffects() {
        armyDisk.setStroke(Color.BLACK);
        armyDisk.setStrokeWidth(1.5);
        armyDisk.setEffect(new DropShadow(3.0, 1.0, 1.0, Color.rgb(0, 0, 0, 0.7)));
        armyDisk.setMouseTransparent(true);

        armyText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        armyText.setFill(Color.WHITE);
        armyText.setTextOrigin(VPos.CENTER);
        armyText.setMouseTransparent(true);
    }

    public void update() {
        Color color = (model.getOwner() != null) ? model.getOwner().getColor() : model.getContinent().getColor().desaturate();
        shape.setFill(color);

        if (model.getOwner() != null) armyDisk.setFill(model.getOwner().getColor().darker());
        else armyDisk.setFill(Color.GRAY);

        armyText.setText(String.valueOf(model.getArmies()));
        armyText.setX(model.getX() - armyText.getLayoutBounds().getWidth() / 2);
    }

    public void setHighlight(Color color, double width) {
        shape.setStroke(color);
        shape.setStrokeWidth(width);
    }
}