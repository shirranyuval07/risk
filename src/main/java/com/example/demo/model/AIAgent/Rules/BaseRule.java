package com.example.demo.model.AIAgent.Rules;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.Logic.AIGraphAnalyzer;
import com.example.demo.model.manager.Continent;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

public interface BaseRule<C> {

    double evaluate(C context, Player player, AIGraphAnalyzer analyzer);

    /**
     * Shared logic for calculating continent progress across all game phases.
     */
    static double calculateSharedContinentScore(
            Continent continent,
            Player currentPlayer,
            Player targetOwner,
            int availableArmies,
            double enemyBreakMultiplier,
            double bonusFocus,
            double progressFocus,
            double resistanceAvoidance)
    {

        if (continent == null) return 0;

        int totalCountries = continent.getCountries().size();
        int ownedCountries = 0;
        int enemyArmiesInContinent = 0;
        boolean isAttack = (targetOwner != null);
        if (isAttack)
            ownedCountries++;

        for (Country c : continent.getCountries())
        {
            if (c.getOwner() == currentPlayer)
                ownedCountries++;
            else
                enemyArmiesInContinent += c.getArmies();

        }


        double progressRatio = (double) ownedCountries / Math.max(totalCountries, 1);
        double bonusScore = continent.getBonusValue() * bonusFocus;
        double progressScore = (progressRatio * GameConstants.EASY_WIN_ARMY_DIVISOR) * progressFocus;

        double strengthRatio = (double) enemyArmiesInContinent / Math.max(availableArmies, 1);
        double resistancePenalty = strengthRatio * resistanceAvoidance;

        double score = bonusScore + progressScore - resistancePenalty;

        if (isAttack && targetOwner != currentPlayer)
        {
            boolean enemyOwnsContinent = continent.getCountries().stream()
                    .allMatch(c -> c.getOwner() == targetOwner);
            if (enemyOwnsContinent)
                score += continent.getBonusValue() * enemyBreakMultiplier;

        }

        return score;
    }
}