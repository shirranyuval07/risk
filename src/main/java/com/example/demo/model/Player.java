package com.example.demo.model;

import com.example.demo.model.AIAgent.BotStrategy;
import com.example.demo.model.States.SetupState;
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

    @Getter private final List<Card> cards = new ArrayList<>();
    @Getter @Setter private boolean conqueredThisTurn = false; // דגל שיזכור אם הגיע לו קלף


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

            // טיפול בשלב ההתחלתי (Setup)
            if (game.getCurrentState() instanceof SetupState) {
                Country c = strategy.findSetUpCountry(this,game);
                game.placeArmy(c);
                return;
            }

            // --- קסם הקלפים של הבוטים (לפני שהם מתחילים לחשוב איפה לתקוף!) ---
            int tradeResult;
            do {
                tradeResult = tradeAnyValidSet(); // שימוש בפונקציה שבנינו להמרת קלפים
                if (tradeResult > 0) {
                    setDraftArmies(getDraftArmies() + tradeResult);
                    System.out.println("🤖 AI " + getName() + " traded cards for " + tradeResult + " extra armies!");
                }
            } while (tradeResult > 0);

            // --- Normal Game Phases ---
            // 1. הבוט מריץ את כל השלבים שלו (Draft, Attack, Fortify) לפי האסטרטגיה
            strategy.executeTurn(this, game);

            // 2. קידום שלבי המשחק באופן אוטומטי כדי להעביר את התור לשחקן הבא
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
    public void addCard(Card card) {
        cards.add(card);
    }

    // פונקציה חכמה שמוצאת את הסט הטוב ביותר וממירה אותו אוטומטית לצבאות!
    // פונקציה חכמה שמוצאת את הסט הטוב ביותר וממירה אותו אוטומטית לצבאות!
    public int tradeAnyValidSet() {
        int inf = java.util.Collections.frequency(cards, Card.INFANTRY);
        int cav = java.util.Collections.frequency(cards, Card.CAVALRY);
        int art = java.util.Collections.frequency(cards, Card.ARTILLERY);

        // קודם בודק אם יש אחד מכל סוג (הכי משתלם)
        if (inf > 0 && cav > 0 && art > 0) {
            // .forEach(cards::remove) removes exactly ONE instance of each card from the list
            List.of(Card.INFANTRY, Card.CAVALRY, Card.ARTILLERY).forEach(cards::remove);
            return 10;
        }
        // אחר כך בודק אם יש 3 מאותו סוג בעזרת פונקציית העזר
        if (art >= 3) return tradeMatchingCards(Card.ARTILLERY, 8);
        if (cav >= 3) return tradeMatchingCards(Card.CAVALRY, 6);
        if (inf >= 3) return tradeMatchingCards(Card.INFANTRY, 4);

        return 0; // אין סט חוקי
    }

    // פונקציית עזר שחוסכת כתיבת לולאות כפולות
    private int tradeMatchingCards(Card type, int reward) {
        for (int i = 0; i < 3; i++) {
            cards.remove(type); // removes the first occurrence of this card type
        }
        return reward;
    }
    // ---  פונקציה להסרת מדינה כאשר שחקן אחר כובש אותה ---
    public void removeCountry(Country c) {
        ownedCountries.remove(c);
    }

}