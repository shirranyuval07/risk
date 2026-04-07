package com.example.demo.model.AIAgent.Rules;

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


    static HeuristicRule futureThreatRule() {
        return (source, target, currentPlayer, analyzer) -> {
            int maxThreat = 0;
            // סורקים את כל השכנים של המדינה המותקפת (היעד)
            for (Country neighbor : target.getNeighbors()) {
                if (neighbor.getOwner() != currentPlayer && neighbor.getArmies() > maxThreat) {
                    maxThreat = neighbor.getArmies();
                }
            }
            return (double) maxThreat / Math.max(source.getArmies(), 1);
        };
    }

    /**
     * A heuristic rule that evaluates the potential value of attacking a target country based on the
     * progress toward conquering a continent, the bonus associated with controlling the continent, and
     * the resistance posed by enemy forces located in the same continent.
     *
     * @param enemyBreakMultiplier the multiplier applied when disrupting an enemy-owned continent to
     *                             increase the score.
     * @param bonusFocus           the weight factor for the continent bonus value in the score
     *                             calculation.
     * @param progressFocus        the weight factor for the progress toward conquering the continent.
     * @param resistanceAvoidance  the penalty weight factor for encountering enemy resistance within
     *                             the continent.
     * @return a {@code HeuristicRule} instance that computes a score indicating the value of attacking
     *         the target country based on its continental strategic importance.
     */
    static HeuristicRule continentProgressRule(double enemyBreakMultiplier, double bonusFocus, double progressFocus, double resistanceAvoidance) {
        return (source, target, currentPlayer, analyzer) -> {
            Continent continent = target.getContinent();
            int totalCountries = continent.getCountries().size();
            int ownedCountries = 0;
            int myArmiesInContinent = 0;
            int enemyArmiesInContinent = 0;

            for (Country c : continent.getCountries()) {
                if (c.getOwner() == currentPlayer) {
                    ownedCountries++;
                    myArmiesInContinent += c.getArmies();
                } else {
                    enemyArmiesInContinent += c.getArmies();
                }
            }

            double progressRatio = (double) (ownedCountries + 1) / totalCountries;
            double bonusScore = continent.getBonusValue() * bonusFocus;
            double progressScore = (progressRatio * 5.0) * progressFocus;
            double strengthRatio = (double) enemyArmiesInContinent / Math.max(source.getArmies(), 1);
            double resistancePenalty = strengthRatio * resistanceAvoidance;
            double score = bonusScore + progressScore - resistancePenalty;

            Player enemy = target.getOwner();
            if (enemy != null) {
                boolean enemyOwnsContinent = true;
                for (Country c : continent.getCountries()) {
                    if (c.getOwner() != enemy) {
                        enemyOwnsContinent = false;
                        break;
                    }
                }
                if (enemyOwnsContinent) {
                    score += continent.getBonusValue() * enemyBreakMultiplier;
                }
            }
            return score;
        };
    }
    // חוק הגנתי: מחפש את המטרה החלשה ביותר (1-2 חיילים) רק כדי להשיג קלף בתור הנוכחי
    static HeuristicRule cardFarmingRule() {
        return (source, target, currentPlayer, analyzer) -> {
            if (target.getArmies() <= 2 && source.getArmies() >= 4) {
                return 50.0; // מעודד מאוד התקפה קלה ומהירה
            }
            return 0; // מונע ממתקפות מסוכנות יותר
        };
    }
}