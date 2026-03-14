package Model.AIAgent.Strategies;

import Model.AIAgent.AbstractHeuristicStrategy;
import Model.AIAgent.Rules.ContinentProgressRule;
import Model.AIAgent.Rules.FutureThreatRule;
import Model.Country;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class BalancedStrategy extends AbstractHeuristicStrategy {

    public BalancedStrategy(
            @Value("${ai.balanced.weight-win-probability}") double weightWin,
            @Value("${ai.balanced.weight-continent-bonus}") double weightCont,
            @Value("${ai.balanced.weight-strategic-value}") double weightStrat,
            @Value("${ai.balanced.weight-expected-casualties}") double weightCas,
            @Value("${ai.balanced.articulation-point-bonus}") double artBonus,
            @Value("${ai.balanced.casualties-multiplier}") double casMult,
            @Value("${ai.balanced.exposure-penalty-multiplier}") double expPenalty,
            @Value("${ai.balanced.attack-threshold}") double attackThresh,
            @Value("${ai.balanced.min-army-advantage}") int minAdv,
    @Value("${ai.balanced.weight-future-threat}") double weightFutureThreat,
            @Value("${ai.balanced.continent-break-multiplier}") double continentBreakMultiplier,
            @Value("${ai.balanced.bonus-focus}") double bonusFocus,
            @Value("${ai.balanced.progress-focus}") double progressFocus,
            @Value("${ai.balanced.resistance-avoidance}") double resistanceAvoidance) {

        super(weightWin, weightCont, weightStrat, weightCas,
                artBonus, casMult, expPenalty, attackThresh, minAdv);
        this.addRule(new FutureThreatRule(), weightFutureThreat);
        this.addRule(new ContinentProgressRule(continentBreakMultiplier,bonusFocus,progressFocus,resistanceAvoidance), weightCont);
    }

    @Override
    public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
        int totalArmies = source.getArmies();
        int guardsToLeaveBehind = 3; // משאיר תמיד 3 שומרים אם אפשר

        int desiredMove = totalArmies - guardsToLeaveBehind;

        return Math.max(minMove, Math.min(maxMove, desiredMove));
    }
}