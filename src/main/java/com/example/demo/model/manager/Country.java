package com.example.demo.model.manager;


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.shape.SVGPath;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * מדינה (טריטוריה) במשחק Risk

 * תפקידיה:
 * - ייצוג טריטוריה על הלוח עם שם, ID וקואורדינטות
 * - ניהול בעלות המדינה (שחקן מסוים)
 * - ניהול מספר החיילים בטריטוריה
 * - חיבור למדינות שכנות (גבול משותף)
 * - ויזואליזציה דרך SVGPath
 * - שיוך ליבשת לחישוב בונוס טריטוריות

 * השימוש: מהווה יחידת משחק בסיסית - כל מדינה היא ישות עצמאית
 * שימושים: 
 * - קרא מידע על בעלות וחיילים
 * - עדכן מספר חיילים בקרב/הגנה
 * - מצא שכנים להתקפה/הגנה
 */
public class Country
{
    @Getter
    private final int id;
    @Getter
    private final String name;

    // --- Data Binding Properties ---
    private final ObjectProperty<Player> owner = new SimpleObjectProperty<>(null);
    private final IntegerProperty armies = new SimpleIntegerProperty(0);

    @Getter
    private final List<Country> neighbors;

    @Getter @Setter
    private int x;
    @Getter @Setter
    private int y;

    @Getter @Setter
    private SVGPath shape;

    @Getter @Setter
    private Continent continent;

    /**
     * בנאי המדינה - אתחול מדינה בקואורדינטות מסוימות
     * 
     * @param id מזהה ייחודי של המדינה
     * @param name שם המדינה
     * @param x קואורדינטת X למיקום על המפה
     * @param y קואורדינטת Y למיקום על המפה
     */
    public Country(int id, String name, int x, int y)
    {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.neighbors = new ArrayList<>();
    }

    /**
     * הוספת מדינה שכנה (יוצרת קשר גבול משותף)
     * 
     * @param neighbor המדינה השכנה להוספה
     */
    public void addNeighbor(Country neighbor)
    {
        if (!neighbors.contains(neighbor))
            neighbors.add(neighbor);

    }


    /**
     * קבלת בעלי המדינה הנוכחי
     * @return השחקן שבעלותו המדינה, או null אם אין בעלים
     */
    public Player getOwner() { return owner.get(); }

    /**
     * קביעת בעלי חדש למדינה
     * @param newOwner השחקן החדש שיהיה בעלים
     */
    public void setOwner(Player newOwner) { owner.set(newOwner); }
    
    /**
     * קבלת תכונת הבעלות לצורך binding ב-UI
     * @return ObjectProperty שמאפשר קישור אוטומטי לממשק
     */
    public ObjectProperty<Player> ownerProperty() { return owner; }

    /**
     * קבלת מספר החיילים בטריטוריה
     * @return מספר החיילים הנוכחי
     */
    public int getArmies() { return armies.get(); }
    

    
    /**
     * קבלת תכונת החיילים לצורך binding ב-UI
     * @return IntegerProperty שמאפשר קישור אוטומטי לממשק
     */
    public IntegerProperty armiesProperty() { return armies; }

    /**
     * הוספת חיילים למדינה
     * @param amount מספר החיילים להוספה
     */
    public void addArmies(int amount) { this.armies.set(this.armies.get() + amount); }
    
    /**
     * הסרת חיילים מהמדינה
     * @param amount מספר החיילים להסרה
     */
    public void removeArmies(int amount) { this.armies.set(this.armies.get() - amount); }
}