package com.example.demo.model;

import com.example.demo.model.Records.BattleResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;

@Slf4j
public class CombatManager {
    public BattleResult resolveAttack(Country attacker, Country defender)
    {
        int aDiceCount = Math.min(3, attacker.getArmies() - 1);
        int dDiceCount = Math.min(2, defender.getArmies());

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
