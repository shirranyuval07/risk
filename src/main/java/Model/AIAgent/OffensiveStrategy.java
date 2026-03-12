package Model.AIAgent;

import Model.Country;
import Model.Player;

public class OffensiveStrategy extends AbstractHeuristicStrategy {

    // ─── קבועים ייחודיים לאסטרטגיה התקפית (אגרסיבית) ────────────────────

    // משקלי חשיבות
    private static final double WEIGHT_WIN_PROBABILITY = 2.5;
    private static final double WEIGHT_CONTINENT_BONUS = 3.0;
    private static final double WEIGHT_STRATEGIC_VALUE = 0.5;
    private static final double WEIGHT_EXPECTED_CASUALTIES = 0.2;

    // מקדמי חישוב פנימיים
    private static final double ARTICULATION_POINT_BONUS = 1.0;
    private static final double CASUALTIES_MULTIPLIER = 0.5;
    private static final double EXPOSURE_PENALTY_MULTIPLIER = 1.0;


    // ספים (Thresholds)
    private static final double ATTACK_THRESHOLD = -1.0;
    private static final int MIN_ARMY_ADVANTAGE = 1;

    // ═════════════════════════════════════════════════════════════════════

    public OffensiveStrategy() {
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