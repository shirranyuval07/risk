package com.example.demo.model.manager;
import com.example.demo.model.AIAgent.BotStrategy;
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

    private final IntegerProperty draftArmies = new SimpleIntegerProperty(0);

    @Getter private final List<Card> cards = new ArrayList<>();
    @Getter @Setter private boolean conqueredThisTurn = false; // דגל שיזכור אם הגיע לו קלף


    public int getDraftArmies() { return draftArmies.get(); }
    public void setDraftArmies(int amount) { draftArmies.set(amount); }
    public IntegerProperty draftArmiesProperty() { return draftArmies; }

    public void decreaseDraftArmies() {
        if (getDraftArmies() > 0) setDraftArmies(getDraftArmies() - 1);
    }
    // --- אסטרטגיה עבור שחקן מחשב ---
    @Setter @Getter
    private BotStrategy strategy;

    public Player(String name, Color color, boolean isAI) {
        this.name = name;
        this.color = color;
        this.isAI = isAI;
        this.ownedCountries = new ArrayList<>();
        draftArmies.set(0);
    }

    public void addCountry(Country c) {
        if (!ownedCountries.contains(c)) {
            ownedCountries.add(c);
            c.setOwner(this);
        }
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    // ---  פונקציה להסרת מדינה כאשר שחקן אחר כובש אותה ---
    public void removeCountry(Country c) {
        ownedCountries.remove(c);
    }
}