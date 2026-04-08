package com.example.demo.model.AIAgent.Strategies;

/**
 * מחשבת הסתברות ניצחון לקרבות Risk באמצעות:
 * 1. הסתברויות שרשרת מרקוב מחושבות מראש לצבאות קטנים (≤10)
 * 2. קירובים מתמטיים לצבאות בינוניים (11-50)
 * 3. פישוט חוק המספרים הגדולים לצבאות ענקיים (>50)
 * מבוסס על מחקר של Jason Osborne (2003) ו-Harris Georgiou (2004).

 */
class WinProbabilityCalculator
{

    private static final int MASSIVE_ARMY_THRESHOLD = 50;
    
    // Ratio thresholds for probability estimation
    private static final double CERTAIN_WIN_RATIO = 0.95;
    private static final double CERTAIN_LOSS_RATIO = 0.80;
    private static final double HIGH_ADVANTAGE_RATIO = 1.5;
    private static final double MODERATE_ADVANTAGE_RATIO = 1.0;
    private static final double EQUILIBRIUM_RATIO = 0.924; // Osborne's equilibrium point

    // Probability values for battle outcomes
    private static final double PROBABILITY_CERTAIN = 1.0;
    private static final double PROBABILITY_IMPOSSIBLE = 0.0;
    private static final double PROBABILITY_NEAR_CERTAIN = 0.99;
    private static final double PROBABILITY_NEAR_IMPOSSIBLE = 0.01;
    private static final double PROBABILITY_HIGH = 0.95;
    private static final double PROBABILITY_MODERATE = 0.75;
    private static final double PROBABILITY_EVEN = 0.50;
    private static final double PROBABILITY_MINIMUM = 0.05;
    
    // Calculation constants for massive battle transition zone
    private static final double TRANSITION_ZONE_CENTER = 0.85;
    private static final double TRANSITION_ZONE_MULTIPLIER = 5.0;
    
    // Linear drop multiplier for below-equilibrium probability
    private static final double BELOW_EQUILIBRIUM_MULTIPLIER = 2.0;

    // Pre-computed Markov chain win probabilities [attackers][defenders]
    private static final double[][] MARKOV_PROBABILITIES = {
        //    0     1     2     3     4     5     6     7     8     9    10  defenders
        {0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // 0 attackers
        {1.00, 0.41, 0.10, 0.02, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // 1 attacker
        {1.00, 0.59, 0.22, 0.07, 0.02, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // 2 attackers
        {1.00, 0.75, 0.36, 0.20, 0.09, 0.04, 0.02, 0.00, 0.00, 0.00, 0.00}, // 3 attackers
        {1.00, 0.81, 0.47, 0.31, 0.16, 0.08, 0.04, 0.02, 0.00, 0.00, 0.00}, // 4 attackers
        {1.00, 0.86, 0.57, 0.42, 0.25, 0.14, 0.07, 0.04, 0.02, 0.00, 0.00}, // 5 attackers
        {1.00, 0.89, 0.65, 0.52, 0.34, 0.21, 0.12, 0.06, 0.03, 0.01, 0.00}, // 6 attackers
        {1.00, 0.91, 0.72, 0.60, 0.43, 0.28, 0.17, 0.10, 0.05, 0.03, 0.01}, // 7 attackers
        {1.00, 0.93, 0.77, 0.68, 0.52, 0.36, 0.24, 0.15, 0.08, 0.04, 0.02}, // 8 attackers
        {1.00, 0.94, 0.82, 0.74, 0.60, 0.44, 0.31, 0.20, 0.12, 0.07, 0.04}, // 9 attackers
        {1.00, 0.95, 0.85, 0.79, 0.67, 0.52, 0.39, 0.27, 0.17, 0.10, 0.06}  // 10 attackers
    };

    /**
     * @param attackerArmies מספר הצבאות התוקפים (כולל זה שנשאר מאחור)
     * @param defenderArmies מספר הצבאות המגנים
     *                       טענת יציאה: הפונקציה מחזירה את ההסתברות ניצחון על פי המחקר של מרקוב.
     * */
    public double estimate(int attackerArmies, int defenderArmies)
    {
        int actualAttackers = attackerArmies - 1; // אחד חייב להישאר מאחורה.

        if (actualAttackers <= 0) return PROBABILITY_IMPOSSIBLE;
        if (defenderArmies <= 0) return PROBABILITY_CERTAIN;

        if (canUseLookupTable(actualAttackers, defenderArmies)) {
            return MARKOV_PROBABILITIES[actualAttackers][defenderArmies];
        }

        double ratio = (double) actualAttackers / defenderArmies;

        if (isMassiveBattle(actualAttackers, defenderArmies)) {
            return estimateMassiveBattleProbability(ratio);
        }

        return estimateMediumBattleProbability(ratio);
    }
    /**
     * @param attackers מספר התוקפים (כולל זה שנשאר מאחור)
     * @param defenders מספר המגנים
     *                       טענת יציאה: הפונקציה מחזירה אמת אם ניתן להשתמש בטבלת ההסתברויות של מרקוב, אחרת שקר.
     * */
    private boolean canUseLookupTable(int attackers, int defenders) {
        return attackers < MARKOV_PROBABILITIES.length 
            && defenders < MARKOV_PROBABILITIES[0].length;
    }
    /**
     * @param attackers מספר התוקפים (כולל זה שנשאר מאחור)
     * @param defenders מספר המגנים
     *                       טענת יציאה: הפונקציה מחזירה אמת אם מדובר בקרב עם צבאות גדולים מאוד (אחד מהם מעל סף מסוים), אחרת שקר.
     * */
    private boolean isMassiveBattle(int attackers, int defenders) {
        return attackers > MASSIVE_ARMY_THRESHOLD || defenders > MASSIVE_ARMY_THRESHOLD;
    }
    /**
     * @param ratio היחס בין מספר התוקפים למגנים
     *                       טענת יציאה: הפונקציה מחזירה את ההסתברות ניצחון לקרב עם צבאות גדולים מאוד על פי קירוב ליניארי בין נקודות סף מוגדרות,
     *                       כאשר מעל סף מסוים נחשב כמעט בטוח ונמוך מסף מסוים נחשב כמעט בלתי אפשרי.
     * */
    private double estimateMassiveBattleProbability(double ratio) {
        if (ratio > CERTAIN_WIN_RATIO) return PROBABILITY_NEAR_CERTAIN;
        if (ratio < CERTAIN_LOSS_RATIO) return PROBABILITY_NEAR_IMPOSSIBLE;
        return PROBABILITY_EVEN + ((ratio - TRANSITION_ZONE_CENTER) * TRANSITION_ZONE_MULTIPLIER);
    }
    /**
     * @param ratio היחס בין מספר התוקפים למגנים
     *                       טענת יציאה: הפונקציה מחזירה את ההסתברות ניצחון לקרב עם צבאות בינוניים על פי קירוב ליניארי בין נקודות סף מוגדרות,
     *                       כאשר מעל יחס מסוים נחשב יתרון גבוה, יחס מסוים נחשב יתרון מתון, יחס שווה נחשב שוויון,
     *                       ויחס מתחת לשוויון נחשב חיסרון עם ירידה ליניארית מתחת לשוויון עד נקודת סף מינימלית שבה ההסתברות מגיעה למינום סביר.
     * */
    private double estimateMediumBattleProbability(double ratio) {
        if (ratio >= HIGH_ADVANTAGE_RATIO) return PROBABILITY_HIGH;
        if (ratio >= MODERATE_ADVANTAGE_RATIO) return PROBABILITY_MODERATE;
        if (ratio >= EQUILIBRIUM_RATIO) return PROBABILITY_EVEN;

        return Math.max(PROBABILITY_MINIMUM, PROBABILITY_EVEN - ((EQUILIBRIUM_RATIO - ratio) * BELOW_EQUILIBRIUM_MULTIPLIER));
    }
}
