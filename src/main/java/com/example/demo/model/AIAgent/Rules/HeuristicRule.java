package com.example.demo.model.AIAgent.Rules;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.Logic.AIGraphAnalyzer;
import com.example.demo.model.manager.Continent;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

@FunctionalInterface
public interface HeuristicRule extends BaseRule<HeuristicRule.AttackContext> {

    /** הקשר התקפה – עוטף source + target לתוך אובייקט אחד כדי להתאים לחתימת BaseRule<C> */
    record AttackContext(Country source, Country target) {}

    /**
     * המתודה המופשטת היחידה – מקבלת AttackContext (כנדרש ע"י BaseRule<AttackContext>).
     */
    @Override
    double evaluate(AttackContext context, Player currentPlayer, AIGraphAnalyzer analyzer);


    static HeuristicRule futureThreatRule()
    {
        return (ctx, currentPlayer, analyzer) ->
        {
            int maxThreat = 0;
            // סורקים את כל השכנים של המדינה המותקפת (היעד)
            for (Country neighbor : ctx.target().getNeighbors()) {
                if (neighbor.getOwner() != currentPlayer && neighbor.getArmies() > maxThreat)
                    maxThreat = neighbor.getArmies();

            }
            return (double) maxThreat / Math.max(ctx.source().getArmies(), 1);
        };
    }


    static HeuristicRule continentProgressRule(double enemyBreakMultiplier, double bonusFocus, double progressFocus, double resistanceAvoidance)
    {
        return (ctx, currentPlayer, analyzer) ->
        {
            Continent continent = ctx.target().getContinent();
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
            double strengthRatio = (double) enemyArmiesInContinent / Math.max(ctx.source().getArmies(), 1);
            double resistancePenalty = strengthRatio * resistanceAvoidance;
            double score = bonusScore + progressScore - resistancePenalty;
            Player enemy = ctx.target().getOwner();

            if (enemy != null)
             {
                 boolean enemyOwnsContinent = continent.getCountries().stream()
                     .allMatch(c -> c.getOwner() == enemy);
                 
                 if (enemyOwnsContinent)
                     score += continent.getBonusValue() * enemyBreakMultiplier;
             }
            return score;
        };
    }
    static HeuristicRule cardFarmingRule()
    {
        return (ctx, currentPlayer, analyzer) ->
        {
            if (ctx.target().getArmies() <= GameConstants.EASY_WIN_MIN_NEIGHBORS && ctx.source().getArmies() >= GameConstants.EASY_WIN_MAX_NEIGHBORS)
                return GameConstants.EASY_WIN_ARMY_THRESHOLD; // מעודד מאוד התקפה קלה ומהירה

            return GameConstants.EASY_WIN_BONUS_BASE; // מונע ממתקפות מסוכנות יותר
        };
    }
}