package com.example.demo.model.AIAgent.Rules;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.Continent;
import com.example.demo.model.Country;
import com.example.demo.model.Player;

@FunctionalInterface
public interface HeuristicRule {

    /**
     * @return ציון מספרי המייצג את הערך של החוק עבור התקיפה הספציפית
     */
    double evaluate(Country source, Country target, Player currentPlayer, AIGraphAnalyzer analyzer);

    // ==========================================
    // רשימת החוקים (Lambdas) מרוכזים כאן
    // ==========================================

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
}