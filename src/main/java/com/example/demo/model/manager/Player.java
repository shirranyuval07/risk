package com.example.demo.model.manager;
import com.example.demo.model.AIAgent.BotStrategy;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Getter;
import lombok.Setter;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * שחקן במשחק Risk - אדם או בוט
 * תפקידיה:
 * - ניהול מידע אישי של שחקן (שם, צבע, סוג)
 * - רשימת מדינות בעלותו
 * - ניהול חיילים בהכנה/הטלה (דרך IntegerProperty)
 * - קלפים שנצברו מניצחון בקרבות
 * - אסטרטגיה לשחקנים מעוצבים (AI)
 * השימוש:
 * - ניהול מצב השחקן במהלך המשחק
 * - מעקב אחרי קנייה וירידה של טריטוריות
 * - הטלת חיילים לטריטוריות
 * - ערעור ניצחון (אם היה קרב מוצלח)
 */
public class Player {
    // --- Getters & Setters ---
    @Getter
    private final String name;
    @Getter
    private final Color color;
    // --- אסטרטגיה עבור שחקן מחשב ---
    @Getter
    private final boolean isAI;
    // --- ניהול מדינות ---
    @Getter
    private final List<Country> ownedCountries;

    private final IntegerProperty draftArmies = new SimpleIntegerProperty(0);

    @Getter private final List<Card> cards = new ArrayList<>();
    @Getter @Setter private boolean conqueredThisTurn = false; // דגל שיזכור אם הגיע לו קלף


    /**
     * קבלת מספר החיילים בהכנה (שעדיין לא הוצבו)
     * @return מספר החיילים המחכים להצבה
     */
    public int getDraftArmies() { return draftArmies.get(); }
    
    /**
     * קביעת מספר חיילים חדש בהכנה
     * @param amount מספר החיילים בהכנה
     */
    public void setDraftArmies(int amount) { draftArmies.set(amount); }
    
    /**
     * קבלת תכונת החיילים בהכנה לצורך binding ב-UI
     * @return IntegerProperty המחובר לתצוגה בממשק
     */
    public IntegerProperty draftArmiesProperty() { return draftArmies; }

    /**
     * הקטנת מספר החיילים בהכנה ב-1 (כשהושם חייל)
     */
    public void decreaseDraftArmies() {
        if (getDraftArmies() > 0) setDraftArmies(getDraftArmies() - 1);
    }
    
    /**
     * אסטרטגיה לשחקן מעוצב (AI)
     */
    @Setter @Getter
    private BotStrategy strategy;

    /**
     * בנאי שחקן - יצירת שחקן חדש
     * 
     * @param name שם השחקן
     * @param color צבע השחקן בממשק
     * @param isAI האם זה שחקן מעוצב (אחרת אדם)
     */
    public Player(String name, Color color, boolean isAI) {
        this.name = name;
        this.color = color;
        this.isAI = isAI;
        this.ownedCountries = new ArrayList<>();
        draftArmies.set(0);
    }

    /**
     * הוספת מדינה לרשימת המדינות של השחקן
     * 
     * @param c המדינה להוספה
     */
    public void addCountry(Country c) {
        if (!ownedCountries.contains(c)) {
            ownedCountries.add(c);
            c.setOwner(this);
        }
    }

    /**
     * הוספת קלף לאוסף הקלפים של השחקן
     * קלפים נתקבלים כשחוקר טריטוריה בקרב
     * 
     * @param card הקלף להוספה
     */
    public void addCard(Card card) {
        cards.add(card);
    }

    /**
     * הסרת מדינה מרשימת המדינות כאשר שחקן אחר כובש אותה
     * 
     * @param c המדינה להסרה
     */
    public void removeCountry(Country c) {
        ownedCountries.remove(c);
    }
}