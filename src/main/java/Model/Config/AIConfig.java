package Model.Config;

import Model.AIAgent.Strategies.ConfigurableHeuristicStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AIConfig {

    private final AIProperties props;

    @Bean
    public ConfigurableHeuristicStrategy balancedStrategy() {
        return buildStrategy(props.getBalanced(),
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies - 3)));
    }

    @Bean
    public ConfigurableHeuristicStrategy defensiveStrategy() {
        return buildStrategy(props.getDefensive(),
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies / 2)));
    }

    @Bean
    public ConfigurableHeuristicStrategy offensiveStrategy() {
        return buildStrategy(props.getOffensive(),
                (totalArmies, minMove, maxMove) -> maxMove);
    }

    // פונקציית עזר שמונעת שכפול קוד בבניית האובייקט
    private ConfigurableHeuristicStrategy buildStrategy(AIStrategyProps p, ConfigurableHeuristicStrategy.TroopMovementBehavior movement) {
        return new ConfigurableHeuristicStrategy(
                p.getWeightWinProbability(), p.getWeightContinentBonus(), p.getWeightStrategicValue(),
                p.getWeightExpectedCasualties(), p.getArticulationPointBonus(), p.getCasualtiesMultiplier(),
                p.getExposurePenaltyMultiplier(), p.getAttackThreshold(), p.getMinArmyAdvantage(),
                p.getWeightFutureThreat(), p.getContinentBreakMultiplier(), p.getBonusFocus(),
                p.getProgressFocus(), p.getResistanceAvoidance(), p.getSetupStackingWeight(),
                movement
        );
    }
}