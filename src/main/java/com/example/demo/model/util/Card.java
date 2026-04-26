package com.example.demo.model.util;

import com.example.demo.model.manager.Player;

import java.util.Collections;
import java.util.List;

/**
 * Card - קלף בונוס בהפקת חיילים במשחק Risk

 * סוגי קלפים:
 * - INFANTRY (חייל רגלי): שווה 1 חיילים
 * - CAVALRY (פרשים): שווה 5 חיילים
 * - ARTILLERY (תותחנים): שווה 10 חיילים

 * כללי הערעור:
 * - 3 קלפים זהים: 4, 6 או 8 חיילים (לפי סוג)
 * - 1 של כל סוג: 10 חיילים
 * - סדרת מתבקשת: כל קלף שלאחריו בסדר הופך לאחד

 * הקבלה: כאשר שחקן כובש טריטוריה במהלך פאזת Attack
 * השימוש: סחירה לחיילים נוספים בהתחלת כל תור
 */
public enum Card
{
    INFANTRY, // חייל רגלי
    CAVALRY,  // פרשים
    ARTILLERY; // תותחנים

    /**
     * בחירת קלף אקראי מתוך שלושת הסוגים
     * @return קלף אקראי
     */
    public static Card getRandom() {
        return values()[(int) (Math.random() * values().length)];
    }

    /**
     * שירות סחירת קלפים - ניהול הערעור של קלפים לחיילים
     */
    public static class Service
    {
        private static final int SET_SIZE = 3;
        /**
         * בדיקה וסחירה של סט ספציפי שבחר השחקן
         * 
         * @param player השחקן הקורא לסחירה
         * @param selectedCards בדיוק 3 קלפים שבחר השחקן
         * @return מספר חיילים שהתקבלו, או 0 אם הסט אינו חוקי
         */
        public int tradeSpecificCards(Player player, List<Card> selectedCards) {
            if (selectedCards.size() != SET_SIZE) return 0;

            int inf = Collections.frequency(selectedCards, Card.INFANTRY);
            int cav = Collections.frequency(selectedCards, Card.CAVALRY);
            int art = Collections.frequency(selectedCards, Card.ARTILLERY);

            if (inf == 1 && cav == 1 && art == 1)
            {
                tradeMatchingCards(player.getCards(), Card.INFANTRY, 0);
                tradeMatchingCards(player.getCards(), Card.CAVALRY, 0);
                tradeMatchingCards(player.getCards(), Card.ARTILLERY, 0);
                return 10;
            }
            if (art == SET_SIZE) return tradeMatchingCards(player.getCards(), Card.ARTILLERY, 8);
            if (cav == SET_SIZE) return tradeMatchingCards(player.getCards(), Card.CAVALRY, 6);
            if (inf == SET_SIZE) return tradeMatchingCards(player.getCards(), Card.INFANTRY, 4);

            return 0;
        }
        
        /**
         * עוזר: הסרת 3 קלפים זהים וחזרה של הגמול
         * 
         * @param cards רשימת קלפי השחקן
         * @param type סוג הקלף להסרה
         * @param reward הגמול לחיילים
         * @return הגמול שהתקבל
         */
        private int tradeMatchingCards(List<Card> cards, Card type, int reward)
        {
            for (int i = 0; i < SET_SIZE; i++)
                cards.remove(type);

            return reward;
        }
    }
}