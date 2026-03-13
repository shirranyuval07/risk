package Model.AIAgent.Strategies;

import Model.AIAgent.AbstractHeuristicStrategy;
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
            @Value("${ai.balanced.min-army-advantage}") int minAdv) {

        super(weightWin, weightCont, weightStrat, weightCas,
                artBonus, casMult, expPenalty, attackThresh, minAdv);
    }

}