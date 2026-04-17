package com.example.demo.config;

import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;


@Configuration
@RequiredArgsConstructor
public class AIConfig {

    private static final int BALANCED_ARMY_RESERVE = 3;
    private static final double CARD_FARMING_RULE_PRIORITY = 2.0;

    private final AIProperties props;

    @Bean
    public HeuristicStrategy.Configurable balancedStrategy() {
        return buildStrategy(props.getBalanced(),
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies - BALANCED_ARMY_RESERVE)),
                HeuristicStrategy.Configurable.DraftBehavior.defensive());
    }

    @Bean
    public HeuristicStrategy.Configurable defensiveStrategy() {
        HeuristicStrategy.Configurable strategy = buildStrategy(props.getDefensive(),
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies / 2)),
                HeuristicStrategy.Configurable.DraftBehavior.defensive());

        strategy.addRule(com.example.demo.model.AIAgent.Rules.HeuristicRule.cardFarmingRule(), CARD_FARMING_RULE_PRIORITY);

        return strategy;
    }

    @Bean
    public HeuristicStrategy.Configurable offensiveStrategy() {
        return buildStrategy(props.getOffensive(),
                (totalArmies, minMove, maxMove) -> maxMove,
                HeuristicStrategy.Configurable.DraftBehavior.aggressive());
    }

    // פונקציית עזר שמונעת שכפול קוד בבניית האובייקט
    private HeuristicStrategy.Configurable buildStrategy(AIStrategyProps p,
                                                         HeuristicStrategy.Configurable.TroopMovementBehavior movement,
                                                         HeuristicStrategy.Configurable.DraftBehavior draft) {
        return HeuristicStrategy.Configurable.from(p, movement, draft);
    }
}