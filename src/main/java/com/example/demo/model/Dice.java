package com.example.demo.model;

import com.example.demo.model.Records.BattleResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class Dice {
    private static Random random = new Random();

    public Dice() {
        random = new Random();
    }

    // מחזיר מערך של תוצאות ממויינות מהגבוה לנמוך (קריטי לחוקי ריסק)
    public static Integer[] roll(int numberOfDice)
    {
        Integer[] results = new Integer[numberOfDice];
        for (int i = 0; i < numberOfDice; i++) {
            results[i] = random.nextInt(6) + 1; // 1 to 6
        }
        // מיון הפוך: 6, 4, 1
        Arrays.sort(results, Collections.reverseOrder());
        return results;
    }

    public static BattleResult rollBattle(int attackerArmies, int defenderArmies) {
        int aDiceCount = Math.min(3, attackerArmies - 1);
        int dDiceCount = Math.min(2, defenderArmies);

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