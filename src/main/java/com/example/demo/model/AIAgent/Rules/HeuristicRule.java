package com.example.demo.model.AIAgent.Rules;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.manager.Continent;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

@FunctionalInterface
public interface HeuristicRule {

    /**
     * @return ציון מספרי המייצג את הערך של החוק עבור התקיפה הספציפית
     */
    double evaluate(Country source, Country target, Player currentPlayer, AIGraphAnalyzer analyzer);


    static HeuristicRule futureThreatRule()
    {
        return (source, target, currentPlayer, analyzer) ->
        {
            int maxThreat = 0;
            // סורקים את כל השכנים של המדינה המותקפת (היעד)
            for (Country neighbor : target.getNeighbors()) {
                if (neighbor.getOwner() != currentPlayer && neighbor.getArmies() > maxThreat)
                    maxThreat = neighbor.getArmies();

            }
            return (double) maxThreat / Math.max(source.getArmies(), 1);
        };
    }


    static HeuristicRule continentProgressRule(double enemyBreakMultiplier, double bonusFocus, double progressFocus, double resistanceAvoidance)
    {
        return (source, target, currentPlayer, analyzer) ->
        {
            Continent continent = target.getContinent();
            int totalCountries = continent.getCountries().size();
            int ownedCountries = 0;
            int enemyArmiesInContinent = 0;

            for (Country c : continent.getCountries())
            {
                if (c.getOwner() == currentPlayer)
                    ownedCountries++;
                else
                    enemyArmiesInContinent += c.getArmies();
            }
            double progressRatio = (double) (ownedCountries + 1) / totalCountries;
            double bonusScore = continent.getBonusValue() * bonusFocus;
            double progressScore = (progressRatio * GameConstants.EASY_WIN_ARMY_DIVISOR) * progressFocus;
            double strengthRatio = (double) enemyArmiesInContinent / Math.max(source.getArmies(), 1);
            double resistancePenalty = strengthRatio * resistanceAvoidance;
            double score = bonusScore + progressScore - resistancePenalty;
            Player enemy = target.getOwner();

            if (enemy != null)
            {
                boolean enemyOwnsContinent = true;
                for (Country c : continent.getCountries())
                {
                    if (c.getOwner() != enemy)
                    {
                        enemyOwnsContinent = false;
                        break;
                    }
                }
                if (enemyOwnsContinent)
                    score += continent.getBonusValue() * enemyBreakMultiplier;
            }
            return score;
        };
    }
    static HeuristicRule cardFarmingRule()
    {
        return (source, target, currentPlayer, analyzer) ->
        {
            if (target.getArmies() <= GameConstants.EASY_WIN_MIN_NEIGHBORS && source.getArmies() >= GameConstants.EASY_WIN_MAX_NEIGHBORS)
                return GameConstants.EASY_WIN_ARMY_THRESHOLD; // מעודד מאוד התקפה קלה ומהירה

            return GameConstants.EASY_WIN_BONUS_BASE; // מונע ממתקפות מסוכנות יותר
        };
    }
}