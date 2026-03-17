package Model;

import Model.AIAgent.BotStrategy;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Getter;
import lombok.Setter;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Player {
    // --- Getters & Setters ---
    @Getter
    private final String name;
    @Getter
    private final Color color;
    // --- חדש: פונקציות AI (מתאים ל-Class Diagram שלך) ---
    @Getter
    private final boolean isAI;
    // --- ניהול מדינות ---
    @Getter
    private final List<Country> ownedCountries;
    @Setter
    @Getter
    private final IntegerProperty draftArmies = new SimpleIntegerProperty(0);

    public int getDraftArmies() { return draftArmies.get(); }
    public void setDraftArmies(int amount) { draftArmies.set(amount); }
    public IntegerProperty draftArmiesProperty() { return draftArmies; }

    public void decreaseDraftArmies() {
        if (getDraftArmies() > 0) setDraftArmies(getDraftArmies() - 1);
    }
    // --- אסטרטגיה עבור שחקן מחשב ---
    @Setter
    private BotStrategy strategy;

    public Player(String name, Color color, boolean isAI) {
        this.name = name;
        this.color = color;
        this.isAI = isAI;
        this.ownedCountries = new ArrayList<>();
        draftArmies.set(0);
    }

    public void playTurn(RiskGame game) {
        if (isAI && strategy != null) {
            // 1. הבוט מריץ את כל השלבים שלו (Draft, Attack, Fortify) לפי האסטרטגיה
            strategy.executeTurn(this, game);

            // 2. קידום שלבי המשחק באופן אוטומטי כדי להעביר את התור לשחקן הבא
            // הבוט סיים, אז אנחנו מריצים את nextPhase עד שהתור עובר
            while(game.getCurrentPlayer() == this && !game.isGameOver()) {
                game.nextPhase();
            }
        }
    }

    public void addCountry(Country c) {
        if (!ownedCountries.contains(c)) {
            ownedCountries.add(c);
            c.setOwner(this);
        }
    }

    // ---  פונקציה להסרת מדינה כאשר שחקן אחר כובש אותה ---
    public void removeCountry(Country c) {
        ownedCountries.remove(c);
    }

}