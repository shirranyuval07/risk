package com.example.demo.config;

import com.example.demo.model.AIAgent.Rules.HeuristicRule;
import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

/*This tell the spring framework that this class contains methods
annotated with @Bean and that Spring should process them to generate
beans to be managed by the application context.*/
@Configuration
/*This is a Lombok annotation that automatically generates a constructor
for all final fields. In this case,it injects the AIProperties props
into this class automatically.*/
@RequiredArgsConstructor
public class AIConfig {
    //האובייקט מחזיק מספר משקלים ותכונות שונות שמחולקים ל3 קבוצות - מאוזן, הגנתי, התקפי
    private final AIProperties props;

    //הבינס רשומים בתוך הApplication context שיוכלו להיות מיושמים לשחקני AI אח"כ

    @Bean
    public HeuristicStrategy.Configurable balancedStrategy() {
        return buildStrategy(props.getBalanced(),
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies - 3)));
    }

    @Bean
    public HeuristicStrategy.Configurable defensiveStrategy() {
        HeuristicStrategy.Configurable strategy = buildStrategy(props.getDefensive(),
                (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies / 2)));
        
        // Add the card farming rule only to the defensive strategy
        strategy.addRule(com.example.demo.model.AIAgent.Rules.HeuristicRule.cardFarmingRule(), 2.0);

        return strategy;
    }

    @Bean
    public HeuristicStrategy.Configurable offensiveStrategy() {
        return buildStrategy(props.getOffensive(),
                (totalArmies, minMove, maxMove) -> maxMove);
    }

    // פונקציית עזר שמונעת שכפול קוד בבניית האובייקט
    //שולחים את הביטוי למבדה המתאים ע"י שימוש בTroopMovementBehavior שנמצא באסטרטגיה
    private HeuristicStrategy.Configurable buildStrategy(AIStrategyProps p, HeuristicStrategy.Configurable.TroopMovementBehavior movement) {
        HeuristicStrategy.HeuristicWeights weights = new HeuristicStrategy.HeuristicWeights(
                p.getWeightWinProbability(),
                p.getWeightContinentBonus(),
                p.getWeightStrategicValue(),
                p.getWeightExpectedCasualties(),
                p.getArticulationPointBonus(),
                p.getCasualtiesMultiplier(),
                p.getExposurePenaltyMultiplier()
        );
        HeuristicStrategy.ThresholdConfig thresholds = new HeuristicStrategy.ThresholdConfig(
                p.getAttackThreshold(),
                p.getMinArmyAdvantage(),
                p.getSetupStackingWeight()
        );
        return new HeuristicStrategy.Configurable(
                weights,
                thresholds,
                p.getWeightFutureThreat(),
                p.getContinentBreakMultiplier(),
                p.getBonusFocus(),
                p.getProgressFocus(),
                p.getResistanceAvoidance(),
                movement
        );
    }
}