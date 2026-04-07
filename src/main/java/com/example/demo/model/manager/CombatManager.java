package com.example.demo.model.manager;

import lombok.extern.slf4j.Slf4j;


import com.example.demo.config.GameConstants;
import com.example.demo.model.Records.GameRecords.BattleResult;
import java.util.Arrays;
import java.util.Collections;

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
     * 1. הטלת קוביות לשני הצדדים (מוגבל ל-3 תוקף, 2 מגן)
     * 2. השוואת תוצאות בסדר יורד
     * 3. חישוב הפסדים
     * 4. עדכון מספר חיילים
     * 5. בדיקה אם המדינה נכבשה
     * 
     * @param attacker המדינה התוקפת
     * @param defender המדינה המגוננת
     * @return BattleResult עם כל נתוני הקרב
     */
    public BattleResult resolveAttack(Country attacker, Country defender)
    {
        int aDiceCount = Math.min(GameConstants.MAX_ATTACKER_DICE, attacker.getArmies() - GameConstants.MIN_ARMIES_TO_STAY);
        int dDiceCount = Math.min(GameConstants.MAX_DEFENDER_DICE, defender.getArmies());

        Integer[] aRolls = Dice.roll(aDiceCount);
        Integer[] dRolls = Dice.roll(dDiceCount);

        int comparisons = Math.min(aDiceCount, dDiceCount);
        int aLoss = 0, dLoss = 0;

        Arrays.sort(aRolls, Collections.reverseOrder());
        Arrays.sort(dRolls, Collections.reverseOrder());

        for (int i = 0; i < comparisons; i++) {
            if (aRolls[i] > dRolls[i]) dLoss++;
            else aLoss++;
        }

        attacker.removeArmies(aLoss);
        defender.removeArmies(dLoss);

        String result = String.format("Attack Result: Attacker lost %d, Defender lost %d", aLoss, dLoss);
        int minMove = aDiceCount; // המינימום הוא כמות הקוביות שהתוקף הטיל
        int maxMove = attacker.getArmies() - 1; // המקסימום הוא כל החיילים פחות 1
        boolean isConquered = false;
        if (defender.getArmies() == 0) {
            result += " | COUNTRY CONQUERED!";
            isConquered = true;
        }

        log.info(result);
        return new BattleResult(aRolls,dRolls,aLoss,dLoss,isConquered,minMove,maxMove);
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
