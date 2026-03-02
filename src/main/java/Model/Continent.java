package Model;

import lombok.Getter;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * יבשת במשחק ריסק – מקבצת מדינות יחד עם בונוס וצבע ייחודי.
 * כל יבשת מוגדרת בגרף העולמי עם צבע נושאי שמשקף את זהותה הגיאוגרפית.
 */
public class Continent {
    @Getter
    private final String name;
    @Getter
    private final int bonusValue;
    private final Color continentColor; // צבע ייחודי ליבשת
    @Getter
    private final List<Country> countries;

    public Continent(String name, int bonusValue, Color continentColor) {
        this.name = name;
        this.bonusValue = bonusValue;
        this.continentColor = continentColor;
        this.countries = new ArrayList<>();
    }

    public void addCountry(Country country) {
        countries.add(country);
    }

    public Color getColor() { return continentColor; }

    // בודק האם שחקן מסוים שולט בכל המדינות ביבשת הזו
    public boolean isOwnedBy(Player player) {
        for (Country c : countries) {
            if (c.getOwner() != player) {
                return false;
            }
        }
        return true;
    }
}