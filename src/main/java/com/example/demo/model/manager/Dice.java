package com.example.demo.model.manager;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import com.example.demo.config.GameConstants;
import com.example.demo.model.Records.GameRecords.BattleResult;

/**
 * כיתת הקוביות - אחראית על הטלות קוביות במשחק Risk
 * 
 * תפקידיה:
 * - הטלת קוביות (1-6) לפי מספר הקוביות הרצוי
 * - מיון התוצאות בסדר יורד (כנדרש בחוקי Risk)
 * - חישוב תוצאות קרב בין תוקף ומגן
 * - הספקת ניתוח הפסדים בקרב
 * 
 * השימוש: משמשת למערכת הקרבות וחישובי ההסתברות
 */
public class Dice {
    private static Random random = new Random();

    /**
     * בנאי ברירת מחדל - מאתחל מחולל מספרים אקראיים חדש
     */
    public Dice() {
        random = new Random();
    }

    /**
     * הטלת קוביות וסידורן בסדר יורד
     * 
     * @param numberOfDice מספר הקוביות להטלה
     * @return מערך של תוצאות הקוביות מהגבוה לנמוך
     * 
     * דוגמה: roll(3) עשוי להחזיר [6, 4, 1]
     */
    public static Integer[] roll(int numberOfDice)
    {
        Integer[] results = new Integer[numberOfDice];
        for (int i = 0; i < numberOfDice; i++) {
            results[i] = random.nextInt(GameConstants.DICE_MAX_VALUE) + GameConstants.DICE_MIN_VALUE;
        }
        // מיון הפוך: 6, 4, 1
        Arrays.sort(results, Collections.reverseOrder());
        return results;
    }

    /**
     * סימולציית קרב מלא בין תוקף ומגן
     * 
     * תהליך:
     * 1. הטלת קוביות לשני הצדדים (מוגבל ל-3 לתוקף, 2 למגן)
     * 2. השוואה של הקוביות בסדר יורד
     * 3. ספירת הפסדים לכל צד
     * 4. חישוב האפשרויות לתנועה אחרי כיבוש
     * 
     * @param attackerArmies מספר החיילים של התוקף
     * @param defenderArmies מספר החיילים של המגן
     * @return BattleResult המכיל תוצאות הקרב וניתוח הפסדים
     */
    public static BattleResult rollBattle(int attackerArmies, int defenderArmies) {
        int aDiceCount = Math.min(GameConstants.MAX_ATTACKER_DICE, attackerArmies - GameConstants.MIN_ARMIES_TO_STAY);
        int dDiceCount = Math.min(GameConstants.MAX_DEFENDER_DICE, defenderArmies);

        Integer[] aRolls = roll(aDiceCount);
        Integer[] dRolls = roll(dDiceCount);

        Arrays.sort(aRolls, Collections.reverseOrder());
        Arrays.sort(dRolls, Collections.reverseOrder());

        int comparisons = Math.min(aDiceCount, dDiceCount);
        int aLoss = 0, dLoss = 0;

        for (int i = 0; i < comparisons; i++) {
            if (aRolls[i] > dRolls[i]) dLoss++;
            else aLoss++;
        }

        boolean conquered = (defenderArmies - dLoss) <= 0;
        int minMove = aDiceCount;
        int maxMove = attackerArmies - 1 - aLoss;

        return new BattleResult(aRolls, dRolls, aLoss, dLoss, conquered, minMove, maxMove);
    }
}