package Model.AIAgent.Strategies;

import Model.AIAgent.Rules.ContinentProgressRule;
import Model.AIAgent.Rules.FutureThreatRule;
import Model.Country;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OffensiveStrategy extends AbstractHeuristicStrategy {

    public OffensiveStrategy(
            @Value("${ai.offensive.weight-win-probability}") double weightWin,
            @Value("${ai.offensive.weight-continent-bonus}") double weightCont,
            @Value("${ai.offensive.weight-strategic-value}") double weightStrat,
            @Value("${ai.offensive.weight-expected-casualties}") double weightCas,
            @Value("${ai.offensive.articulation-point-bonus}") double artBonus,
            @Value("${ai.offensive.casualties-multiplier}") double casMult,
            @Value("${ai.offensive.exposure-penalty-multiplier}") double expPenalty,
            @Value("${ai.offensive.attack-threshold}") double attackThresh,
            @Value("${ai.offensive.min-army-advantage}") int minAdv,
            @Value("${ai.offensive.weight-future-threat}") double weightFutureThreat,
            @Value("${ai.offensive.continent-break-multiplier}") double continentBreakMultiplier,
            @Value("${ai.offensive.bonus-focus}") double bonusFocus,
            @Value("${ai.offensive.progress-focus}") double progressFocus,
            @Value("${ai.offensive.resistance-avoidance}") double resistanceAvoidance) {

        super(weightWin, weightCont, weightStrat, weightCas,
                artBonus, casMult, expPenalty, attackThresh, minAdv);
        this.addRule(new FutureThreatRule(), weightFutureThreat);
        this.addRule(new ContinentProgressRule(continentBreakMultiplier,bonusFocus,progressFocus,resistanceAvoidance), weightCont);
    }

    @Override
    public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
        return maxMove;
    }
}