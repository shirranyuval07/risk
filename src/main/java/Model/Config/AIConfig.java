package Model.Config;

import Model.AIAgent.Strategies.ConfigurableHeuristicStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ConfigurableHeuristicStrategy balancedStrategy(
            @Value("${ai.balanced.weight-win-probability}") double wWin,
            @Value("${ai.balanced.weight-continent-bonus}") double wCont,
            @Value("${ai.balanced.weight-strategic-value}") double wStrat,
            @Value("${ai.balanced.weight-expected-casualties}") double wCost,
            @Value("${ai.balanced.articulation-point-bonus}") double artBonus,
            @Value("${ai.balanced.casualties-multiplier}") double casMult,
            @Value("${ai.balanced.exposure-penalty-multiplier}") double expPenalty,
            @Value("${ai.balanced.attack-threshold}") double attackThresh,
            @Value("${ai.balanced.min-army-advantage}") int minAdv,
            @Value("${ai.balanced.weight-future-threat}") double wFutureThreat,
            @Value("${ai.balanced.continent-break-multiplier}") double contBreakMult,
            @Value("${ai.balanced.bonus-focus}") double bonusFocus,
            @Value("${ai.balanced.progress-focus}") double progressFocus,
            @Value("${ai.balanced.resistance-avoidance}") double resistanceAvoidance,
            @Value("${ai.balanced.setup-stacking-weight}") double setupWeight
    ) {
        return new ConfigurableHeuristicStrategy(
                wWin, wCont, wStrat, wCost, artBonus, casMult, expPenalty, attackThresh, minAdv,
                wFutureThreat, contBreakMult, bonusFocus, progressFocus, resistanceAvoidance, setupWeight,
                // Balanced Movement: Leave 3 guards behind if possible
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies - 3))
        );
    }

    @Bean
    public ConfigurableHeuristicStrategy defensiveStrategy(
            @Value("${ai.defensive.weight-win-probability}") double wWin,
            @Value("${ai.defensive.weight-continent-bonus}") double wCont,
            @Value("${ai.defensive.weight-strategic-value}") double wStrat,
            @Value("${ai.defensive.weight-expected-casualties}") double wCost,
            @Value("${ai.defensive.articulation-point-bonus}") double artBonus,
            @Value("${ai.defensive.casualties-multiplier}") double casMult,
            @Value("${ai.defensive.exposure-penalty-multiplier}") double expPenalty,
            @Value("${ai.defensive.attack-threshold}") double attackThresh,
            @Value("${ai.defensive.min-army-advantage}") int minAdv,
            @Value("${ai.defensive.weight-future-threat}") double wFutureThreat,
            @Value("${ai.defensive.continent-break-multiplier}") double contBreakMult,
            @Value("${ai.defensive.bonus-focus}") double bonusFocus,
            @Value("${ai.defensive.progress-focus}") double progressFocus,
            @Value("${ai.defensive.resistance-avoidance}") double resistanceAvoidance,
            @Value("${ai.defensive.setup-stacking-weight}") double setupWeight
    ) {
        return new ConfigurableHeuristicStrategy(
                wWin, wCont, wStrat, wCost, artBonus, casMult, expPenalty, attackThresh, minAdv,
                wFutureThreat, contBreakMult, bonusFocus, progressFocus, resistanceAvoidance, setupWeight,
                // Defensive Movement: Split forces half-and-half
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies / 2))
        );
    }

    @Bean
    public ConfigurableHeuristicStrategy offensiveStrategy(
            @Value("${ai.offensive.weight-win-probability}") double wWin,
            @Value("${ai.offensive.weight-continent-bonus}") double wCont,
            @Value("${ai.offensive.weight-strategic-value}") double wStrat,
            @Value("${ai.offensive.weight-expected-casualties}") double wCost,
            @Value("${ai.offensive.articulation-point-bonus}") double artBonus,
            @Value("${ai.offensive.casualties-multiplier}") double casMult,
            @Value("${ai.offensive.exposure-penalty-multiplier}") double expPenalty,
            @Value("${ai.offensive.attack-threshold}") double attackThresh,
            @Value("${ai.offensive.min-army-advantage}") int minAdv,
            @Value("${ai.offensive.weight-future-threat}") double wFutureThreat,
            @Value("${ai.offensive.continent-break-multiplier}") double contBreakMult,
            @Value("${ai.offensive.bonus-focus}") double bonusFocus,
            @Value("${ai.offensive.progress-focus}") double progressFocus,
            @Value("${ai.offensive.resistance-avoidance}") double resistanceAvoidance,
            @Value("${ai.offensive.setup-stacking-weight}") double setupWeight
    ) {
        return new ConfigurableHeuristicStrategy(
                wWin, wCont, wStrat, wCost, artBonus, casMult, expPenalty, attackThresh, minAdv,
                wFutureThreat, contBreakMult, bonusFocus, progressFocus, resistanceAvoidance, setupWeight,
                // Offensive Movement: Move the maximum allowed armies
                (totalArmies, minMove, maxMove) -> maxMove
        );
    }
}