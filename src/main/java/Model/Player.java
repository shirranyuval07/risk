package Model;

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
    private int draftArmies;

    // --- אסטרטגיה עבור שחקן מחשב ---
    @Setter
    private BotStrategy strategy;

    public Player(String name, Color color, boolean isAI) {
        this.name = name;
        this.color = color;
        this.isAI = isAI;
        this.ownedCountries = new ArrayList<>();
        this.draftArmies = 0;
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

    public void decreaseDraftArmies() {
        if (draftArmies > 0) draftArmies--;
    }
}