package Model.AIAgent;

import Model.Country;
import Model.Player;

public class BalancedStrategy extends AbstractHeuristicStrategy {

    // משקלי חשיבות (טקטיקה בסיסית)
    private static final double WEIGHT_WIN_PROBABILITY = 1.5;
    private static final double WEIGHT_CONTINENT_BONUS = 2.0;
    private static final double WEIGHT_STRATEGIC_VALUE = 1.2;
    private static final double WEIGHT_EXPECTED_CASUALTIES = 1.0;

    // מקדמים פנימיים
    private static final double ARTICULATION_POINT_BONUS = 5.0;
    private static final double CASUALTIES_MULTIPLIER = 0.6;
    private static final double EXPOSURE_PENALTY_MULTIPLIER = 1.5;


    // ספים לביצוע מהלך
    private static final double ATTACK_THRESHOLD = 0.0;
    private static final int MIN_ARMY_ADVANTAGE = 2;

    public BalancedStrategy() {
        super(WEIGHT_WIN_PROBABILITY,
                WEIGHT_CONTINENT_BONUS,
                WEIGHT_STRATEGIC_VALUE,
                WEIGHT_EXPECTED_CASUALTIES,
                ARTICULATION_POINT_BONUS,
                CASUALTIES_MULTIPLIER,
                EXPOSURE_PENALTY_MULTIPLIER,
                ATTACK_THRESHOLD,
                MIN_ARMY_ADVANTAGE);
    }

}