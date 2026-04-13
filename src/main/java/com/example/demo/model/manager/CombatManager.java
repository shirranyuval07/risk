package com.example.demo.model.manager;

import lombok.extern.slf4j.Slf4j;


import com.example.demo.model.Records.GameRecords.BattleResult;

/**
 * מנהל הקרבות - אחראי על כל ההיבטים של קרב בין שתי מדינות
 * 
 * תפקידיה:
 * - הטלת קוביות לשני הצדדים
 * - השוואת תוצאות והחלטה על הפסדים
 * - עדכון מספר החיילים לאחר קרב
 * - בדיקה אם המדינה נכבשה
 * - ניהול העברת בעלות כשמדינה נכבשת
 * 
 * השימוש:
 * - קריאה כשתוקף בוחר להתקיף מדינה
 * - עדכון נתוני קרב בממשק
 */
@Slf4j
public class CombatManager {
    
    /**
     * פתרון קרב בודד בין תוקף למגן
     *
     * תהליך:
     * 1. מאציל את חישוב הקרב ל-Dice.rollBattle (הטלה, השוואה, חישוב הפסדים)
     * 2. מעדכן את מספר החיילים של שתי המדינות לפי תוצאות הקרב
     * 3. מתעד את התוצאה בלוג
     *
     * @param attacker המדינה התוקפת
     * @param defender המדינה המגוננת
     * @return BattleResult עם כל נתוני הקרב
     */
    public BattleResult resolveAttack(Country attacker, Country defender)
    {
        BattleResult result = Dice.rollBattle(attacker.getArmies(), defender.getArmies());

        attacker.removeArmies(result.attackerLosses());
        defender.removeArmies(result.defenderLosses());

        String message = String.format("Attack Result: Attacker lost %d, Defender lost %d%s",
                result.attackerLosses(), result.defenderLosses(),
                result.conquered() ? " | COUNTRY CONQUERED!" : "");
        log.info(message);

        return result;
    }
    
    /**
     * ביצוע כיבוש מלא - העברת בעלות מדינה ותנועת חיילים
     * 
     * תהליך:
     * 1. העברת המדינה מהבעלים הישנים לחדשים
     * 2. העברת חיילים מהתוקף למגן
     * 3. סימון שהשחקן כיבש טריטוריה (קלף בסוף תור)
     * 
     * @param attacker המדינה התוקפת (שמעברת חיילים)
     * @param defender המדינה שנכבשה (מקבלת חיילים)
     * @param moveAmount מספר החיילים להעברה
     */
    public void executeConquest(Country attacker, Country defender, int moveAmount)
    {
        Player oldOwner = defender.getOwner();
        Player newOwner = attacker.getOwner();

        oldOwner.removeCountry(defender);
        newOwner.addCountry(defender);

        attacker.removeArmies(moveAmount);
        defender.addArmies(moveAmount);
        newOwner.setConqueredThisTurn(true);
    }

}
