package com.example.demo.view;

import com.example.demo.model.manager.Country;
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
    @Getter private final Text nameText; // משתנה השם החדש

    public CountryView(Country country, Group shapesLayer, Group symbolsLayer) {
        this.model = country;

        // 1. Create Shape
        this.shape = country.getShape();
        setupShapeEffects();
        shapesLayer.getChildren().add(shape);

        // 2. Create Disk & Text
        this.armyDisk = new Circle(country.getX(), country.getY(), 12);
        this.armyText = new Text(country.getX(), country.getY(), "0");

        // יצירת טקסט השם - ממוקם 20 פיקסלים מעל מרכז המדינה (Y - 20)
        this.nameText = new Text(country.getX(), country.getY() - 20, country.getName());

        setupSymbolEffects();

        // חשוב! כאן אנחנו מוסיפים גם את העיגול, גם את החיילים וגם את השם לשכבה העליונה
        symbolsLayer.getChildren().addAll(armyDisk, armyText, nameText);

        // 3. Connect Data Bindings!
        setupBindings();
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

        // הגדרות עיצוב לשם המדינה כדי שיהיה מאוד בולט
        nameText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        nameText.setFill(Color.WHITE);
        nameText.setStroke(Color.BLACK); // קו מתאר שחור
        nameText.setStrokeWidth(0.5);    // עובי קו המתאר
        nameText.setTextOrigin(VPos.CENTER);
        nameText.setEffect(new DropShadow(2.0, 1.0, 1.0, Color.rgb(0, 0, 0, 0.8)));
        nameText.setMouseTransparent(true); // מונע מהטקסט לחסום לחיצות עכבר
        nameText.setVisible(false);
    }

    private void setupBindings() {
        // MAGIC LINE 1
        armyText.textProperty().bind(model.armiesProperty().asString());

        // MAGIC LINE 2
        armyText.textProperty().addListener((obs, oldVal, newVal) -> {
            armyText.setX(model.getX() - armyText.getLayoutBounds().getWidth() / 2);
        });

        // MAGIC LINE 3
        model.ownerProperty().addListener((obs, oldOwner, newOwner) -> {
            updateColors();
        });

        // עדכון ראשוני
        updateColors();
        armyText.setX(model.getX() - armyText.getLayoutBounds().getWidth() / 2);

        // מירכוז שם המדינה בדיוק מעל העיגול
        nameText.setX(model.getX() - nameText.getLayoutBounds().getWidth() / 2);
    }

    private void updateColors() {
        Color color = (model.getOwner() != null) ? model.getOwner().getColor() : model.getContinent().getColor().desaturate();
        shape.setFill(color);

        if (model.getOwner() != null) {
            armyDisk.setFill(model.getOwner().getColor().darker());
        } else {
            armyDisk.setFill(Color.GRAY);
        }
    }

    public void setHighlight(Color color, double width) {
        shape.setStroke(color);
        shape.setStrokeWidth(width);
    }
}