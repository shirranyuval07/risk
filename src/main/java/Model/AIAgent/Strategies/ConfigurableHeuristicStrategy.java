package Model.AIAgent.Strategies;

import Model.AIAgent.Rules.ContinentProgressRule;
import Model.AIAgent.Rules.FutureThreatRule;
import Model.Country;

public class ConfigurableHeuristicStrategy extends AbstractHeuristicStrategy {

    private final TroopMovementBehavior movementBehavior;

    public ConfigurableHeuristicStrategy(
            double wWin, double wCont, double wStrat, double wCost, double artBonus,
            double casMult, double expPenalty, double attackThresh, int minAdv,
            double weightFutureThreat, double continentBreakMultiplier, double bonusFocus,
            double progressFocus, double resistanceAvoidance, double setupWeight,
            TroopMovementBehavior movementBehavior) {

        super(wWin, wCont, wStrat, wCost, artBonus, casMult, expPenalty, attackThresh, minAdv, setupWeight);
        this.addRule(new FutureThreatRule(), weightFutureThreat);
        this.addRule(new ContinentProgressRule(continentBreakMultiplier, bonusFocus, progressFocus, resistanceAvoidance), wCont);
        this.movementBehavior = movementBehavior;
    }

    @Override
    public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
        return movementBehavior.calculate(source.getArmies(), minMove, maxMove);
    }

    @FunctionalInterface
    public interface TroopMovementBehavior {
        int calculate(int totalArmies, int minMove, int maxMove);
    }
}