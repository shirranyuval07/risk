package Model.AIAgent;

import Model.Country;
import Model.Player;

public class DefensiveStrategy extends AbstractHeuristicStrategy {

    // ─── קבועים ייחודיים לאסטרטגיה הגנתית (שמרנית) ───────────────────────

    // משקלי חשיבות
    private static final double WEIGHT_WIN_PROBABILITY = 1.0;
    private static final double WEIGHT_CONTINENT_BONUS = 0.5;
    private static final double WEIGHT_STRATEGIC_VALUE = 3.0;
    private static final double WEIGHT_EXPECTED_CASUALTIES = 1.5; // מתון יותר למניעת התבצרות מוחלטת

    // מקדמי חישוב פנימיים
    private static final double ARTICULATION_POINT_BONUS = 5.0;
    private static final double CASUALTIES_MULTIPLIER = 1.2;
    private static final double EXPOSURE_PENALTY_MULTIPLIER = 3.0;


    // ספים (Thresholds)
    private static final double ATTACK_THRESHOLD = 0.0; // הורדנו ל-0 כדי שעדיין יתקוף לפעמים
    private static final int MIN_ARMY_ADVANTAGE = 4;

    // ═════════════════════════════════════════════════════════════════════

    public DefensiveStrategy() {
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