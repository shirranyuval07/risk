package Model.AIAgent.Strategies;

import Model.AIAgent.AbstractHeuristicStrategy;
import Model.AIAgent.Rules.ContinentProgressRule;
import Model.AIAgent.Rules.FutureThreatRule;
import Model.Country;
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
            @Value("${ai.defensive.min-army-advantage}") int minAdv,
            @Value("${ai.defensive.weight-future-threat}") double weightFutureThreat,
            @Value("${ai.defensive.continent-break-multiplier}") double continentBreakMultiplier,
            @Value("${ai.defensive.bonus-focus}") double bonusFocus,
            @Value("${ai.defensive.progress-focus}") double progressFocus,
            @Value("${ai.defensive.resistance-avoidance}") double resistanceAvoidance) {

        super(weightWin, weightCont, weightStrat, weightCas,
                artBonus, casMult, expPenalty, attackThresh, minAdv);
        this.addRule(new FutureThreatRule(), weightFutureThreat);
        this.addRule(new ContinentProgressRule(continentBreakMultiplier,bonusFocus,progressFocus,resistanceAvoidance), weightCont);
    }

    @Override
    public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
        int totalArmies = source.getArmies();

        // הבוט רוצה לחלק אותם חצי-חצי כדי להגן על שתי המדינות
        int desiredMove = totalArmies / 2;

        // נוודא שהבקשה לא חורגת מהחוקים (לא פחות מהמינימום ולא יותר מהמקסימום)
        return Math.max(minMove, Math.min(maxMove, desiredMove));
    }
}