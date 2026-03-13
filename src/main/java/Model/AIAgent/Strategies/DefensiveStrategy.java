package Model.AIAgent.Strategies;

import Model.AIAgent.AbstractHeuristicStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefensiveStrategy extends AbstractHeuristicStrategy {

    public DefensiveStrategy(
            @Value("${ai.defensive.weight-win-probability}") double weightWin,
            @Value("${ai.defensive.weight-continent-bonus}") double weightCont,
            @Value("${ai.defensive.weight-strategic-value}") double weightStrat,
            @Value("${ai.defensive.weight-expected-casualties}") double weightCas,
            @Value("${ai.defensive.articulation-point-bonus}") double artBonus,
            @Value("${ai.defensive.casualties-multiplier}") double casMult,
            @Value("${ai.defensive.exposure-penalty-multiplier}") double expPenalty,
            @Value("${ai.defensive.attack-threshold}") double attackThresh,
            @Value("${ai.defensive.min-army-advantage}") int minAdv) {

        super(weightWin, weightCont, weightStrat, weightCas,
                artBonus, casMult, expPenalty, attackThresh, minAdv);
    }
}