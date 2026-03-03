package Model;

import javafx.scene.shape.SVGPath;
import lombok.Getter;
import lombok.Setter;

import javafx.scene.shape.Polygon;
import java.util.ArrayList;
import java.util.List;


/**
 * מדינה (Territory) במשחק ריסק – צומת בגרף השכנויות.
 * כל מדינה מכילה: מזהה, שם, בעלים, צבאות, שכנים, מיקום על המפה,
 * צורת פוליגון לציור, והפנייה ליבשת שאליה שייכת.
 */
@Getter
public class Country {
    // Getters & Setters
    private final int id;
    private final String name;
    @Setter
    private Player owner;
    private int armies;
    private final List<Country> neighbors;

    // מיקום מרכזי
    // קואורדינטות מרכז לציור ולהתמצאות
    @Setter
    private int x;
    @Setter
    private int y;

    // פוליגון – צורה גיאוגרפית
    // צורת הטריטוריה כפוליגון (OOP – הנתונים שייכים לאובייקט)
    @Setter
    private SVGPath shape;

    // יבשת
    // הפנייה ליבשת (back-reference)
    @Setter
    private Continent continent;

    public Country(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.neighbors = new ArrayList<>();
        this.armies = 0;
    }

    public void addNeighbor(Country neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
        }
    }

    public void addArmies(int amount) { this.armies += amount; }
    public void removeArmies(int amount) { this.armies -= amount; }

}