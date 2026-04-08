package com.example.demo.model.AIAgent.Strategies;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.AIAgent.Rules.HeuristicRule;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * HeuristicStrategy - ממשק אסטרטגיות היוריסטיות לבוטי AI
 * תפקידה:
 * - הגדרת ממשק לחישוב ניקוד התקפה (heuristic score)
 * - הגדרת ממשק לקבלת הגדרות AI שונות
 * - ספקת מחשבון הסתברות זכייה בקרבות
 * תת-מחלקות:
 * - Abstract: הבסיס לכל אסטרטגיה עם חישובי בסיס
 * - Configurable: אסטרטגיה מתאימה לכל משחק
 * השימוש: מחלקה בסיסית לכל ישות AI בעלת אסטרטגיה
 */
public interface HeuristicStrategy {

    double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer);
    double getAttackThreshold();
    double getMinArmyAdvantage();
    int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove);
    double getSetupStackingWeight();


    abstract class Abstract implements HeuristicStrategy {
        private final static double ISOLATION_BONUS_MULTIPLIER = GameConstants.ISOLATION_BONUS_MULTIPLIER;
        private final static double DEFAULT_ISOLATION_CHECK = GameConstants.MAX_ISOLATION_CHECK;
        private final static double MINIMUM_BONUS = GameConstants.MINIMUM_BONUS;

        // Configuration Weights
        protected final HeuristicWeights weights;
        protected final ThresholdConfig thresholds;
        private final Map<HeuristicRule, Double> dynamicRules = new HashMap<>();

        // Win Probability Calculator
        private final WinProbabilityCalculator winCalculator = new WinProbabilityCalculator();

        public Abstract(HeuristicWeights weights, ThresholdConfig thresholds) {
            this.weights = weights;
            this.thresholds = thresholds;
        }

        public void addRule(HeuristicRule rule, double weight) {
            dynamicRules.put(rule, weight);
        }

        // ===================================================================================
        // פונקציית החישוב הראשית - המאגדת את כל המרכיבים
        // ===================================================================================

        /**
         @param player - השחקן המבצע את ההתקפה (לבדיקת חשיפה ואסטרטגיות מבוססות שחקן)
         @param analyzer - כלי עזר לניתוח הגרף של המפה (למציאת נקודות ביטחון, אויבים סמוכים וכו')
         @param source - המדינה שממנה מתבצעת ההתקפה
         @param target - המדינה שמטרתה של ההתקפה
         @return ניקוד התקפה שמייצג את עד כמה ההתקפה הזו טובה עבור השחקן, בהתחשב בכל הגורמים השונים (הסתברות זכייה, ערך אסטרטגי, עלות צפויה וכו')
         הנוסחה היא: basePath + (dynamicBonus * dynamicWeight) + easyWinBonus
         - basePath: שילוב של גורמים בסיסיים (הסתברות זכייה, ערך אסטרטגי, עלות צפויה)
         - dynamicBonus: סכום של כללים דינמיים מותאמים אישית (כגון איום עתידי, התקדמות ליבשת, חקירת אויב וכו')
         - easyWinBonus: בונוס מיוחד אם ההתקפה היא "קל לנצח" (האויב בודד וחלש מאוד) - זה מעודד בוטים לסיים משחק קרוב לסיום
         */
        @Override
        public double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {
            // מרכיבים בסיסיים
            double winProbability = winCalculator.estimate(source.getArmies(), target.getArmies());
            double strategicValue = calculateStrategicValue(target, analyzer);
            double expectedCost = calculateExpectedCost(source, target, player);

            // הרכיבו לציון בסיס
            double baseScore = computeBaseScore(winProbability, strategicValue, expectedCost);

            // חישוב חוקים דינמיים
            double dynamicScore = computeDynamicScore(source, target, player, analyzer);

            // בונוס "קל לנצח" - אם האויב בודד וחלש
            double easyWinBonus = calculateEasyWinBonus(target, player, analyzer);

            return baseScore + dynamicScore + easyWinBonus;
        }

        // ===================================================================================
        // קבוצה 1: חישובי ניקוד בסיס (Base Score) - הסתברות, ערך אסטרטגי וסיכונים
        // ===================================================================================

        /**
         @param cost - עלות צפויה של ההתקפה (כולל הפסדים צפויים ועונש חשיפה)
         @param stratValue - הערך האסטרטגי של הלכש יעד
         @param winProb - ההסתברות לזכייה בקרב
         @return ניקוד בסיס שמייצג את איכות ההתקפה בהתחשב בגורמים הבסיסיים
         הנוסחה היא: (winProbability × משקל) + (strategicValue × משקל) - (expectedCost × משקל)
         */
        private double computeBaseScore(double winProb, double stratValue, double cost) {
            return (weights.winProbability * winProb)           // כמה סביר שנזכה
                    + (weights.strategicValue * stratValue)       // כמה חשוב המהךף הזה
                    - (weights.expectedCasualties * cost);        // כמה זה יעלה לנו
        }

        /**
         @param analyzer - כלי עזר לניתוח הגרף של המפה
         @param target - המדינה שמטרתה של ההתקפה
         @return הערך האסטרטגי של היעד, בהתחשב בבידוד ונקודות ביטחון קריטיות
         */
        private double calculateStrategicValue(Country target, AIGraphAnalyzer analyzer) {
            double isolationBonus = calculateIsolationBonus(target);
            double bottleneckBonus = calculateArticulationBonus(target, analyzer);
            return isolationBonus + bottleneckBonus;
        }

        /**
         @param target - המדינה שמטרתה של ההתקפה
         @return בונוס בידוד: מדינות עם מעט אויבים סמוכים הן פחות מסוכנות ולכן בעלות ערך אסטרטגי גבוה יותר
         */
        private double calculateIsolationBonus(Country target) {
            long enemyNeighborsCount = target.getNeighbors().stream()
                    .filter(neighbor -> neighbor.getOwner() != target.getOwner())
                    .count();
            return Math.max(MINIMUM_BONUS, DEFAULT_ISOLATION_CHECK - enemyNeighborsCount) * ISOLATION_BONUS_MULTIPLIER;
        }

        /**
         @param target - המדינה שמטרתה של ההתקפה
         @param analyzer - כלי עזר לניתוח הגרף של המפה
         @return בונוס נקודת ביטחון (articulation point)
         */
        private double calculateArticulationBonus(Country target, AIGraphAnalyzer analyzer) {
            Player targetOwner = target.getOwner();
            if (targetOwner == null) return MINIMUM_BONUS;

            Set<Country> criticalPoints = analyzer.findArticulationPoints(targetOwner);
            return criticalPoints.contains(target) ? weights.articulationPointBonus : MINIMUM_BONUS;
        }

        /**
         @param player - השחקן המבצע את ההתקפה
         @param source - המדינה שממנה מתבצעת ההתקפה
         @param target - המדינה שמטרתה של ההתקפה
         @return עלות צפויה של ההתקפה, הכוללת הפסדים צפויים ועונש חשיפה
         */
        private double calculateExpectedCost(Country source, Country target, Player player) {
            double baseCost = estimateExpectedLoss(target.getArmies(), source.getArmies());

            if (isSourceExposedAfterAttack(source, target, player))
                baseCost *= weights.exposurePenaltyMultiplier;

            return baseCost;
        }

        /**
         @param attackerArmies - מספר החיילים התוקפים
         @param defenderArmies - מספר החיילים המגנים
         @return הערכת הפסדים צפויים עבור התוקף
         */
        private double estimateExpectedLoss(int defenderArmies, int attackerArmies) {
            return ((double) defenderArmies / Math.max(attackerArmies, 1)) * weights.casualtiesMultiplier;
        }

        /**
         * @param source - המדינה שממנה מתבצעת ההתקפה
         @param target - המדינה שמטרתה של ההתקפה
         @param player - השחקן המבצע את ההתקפה
         @return האם ההתקפה תחשוף את המקור לאויבים סמוכים שיכולים לתקוף בחזרה
         */
        private boolean isSourceExposedAfterAttack(Country source, Country target, Player player) {
            int remainingDefense = source.getArmies() - 1;

            return source.getNeighbors().stream()
                    .filter(neighbor -> neighbor != target && neighbor.getOwner() != player)
                    .anyMatch(enemy -> enemy.getArmies() >= remainingDefense);
        }

        // ===================================================================================
        // קבוצה 2: חישובי ניקוד דינמי (Dynamic Score) - חוקים היוריסטיים מותאמים אישית
        // ===================================================================================

        /**
         @param player - השחקן המבצע את ההתקפה
         @param source - המדינה שממנה מתבצעת ההתקפה
         @param target - המדינה שמטרתה של ההתקפה
         @param analyzer - כלי עזר לניתוח הגרף
         @return ניקוד דינמי שמייצג את ההשפעה של כללים מותאמים אישית על ההתקפה הזו
         */
        private double computeDynamicScore(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {
            double score = 0;
            for (var rule : dynamicRules.entrySet()) {
                score += rule.getKey().evaluate(source, target, player, analyzer) * rule.getValue();
            }
            return score;
        }

        // ===================================================================================
        // קבוצה 3: חישובי בונוס ניצחון קל (Easy Win Bonus) - אינסטינקט חיסול שחקנים
        // ===================================================================================

        /**
         * עוזר: בונוס "קל לנצח" לכשהאויב בודד וחלש מאוד
         * זה מעודד בוטים להסיים משחק קרוב לסיום
         */
        private double calculateEasyWinBonus(Country target, Player player, AIGraphAnalyzer analyzer) {
            Player targetOwner = target.getOwner();
            if (targetOwner == null) return 0;

            int enemyTerritories = targetOwner.getOwnedCountries().size();
            int enemyNeighbors = analyzer.countEnemyNeighbors(target, player);
            int targetArmies = target.getArmies();

            // בונוס לכיבוש המדינה האחרונה
            if (enemyTerritories == 1) {
                double dynamicBonus = GameConstants.EASY_WIN_BONUS_BASE * ((double) GameConstants.EASY_WIN_ARMY_DIVISOR / Math.max(targetArmies, 1));
                return Math.max(GameConstants.EASY_WIN_FINAL_TERRITORY_MULTIPLIER, dynamicBonus);
            }

            // בונוס חזק אם האויב מוקף וחלש
            if (enemyTerritories <= GameConstants.EASY_WIN_TERRITORY_THRESHOLD &&
                    enemyNeighbors >= GameConstants.EASY_WIN_MIN_NEIGHBORS &&
                    targetArmies <= GameConstants.EASY_WIN_ARMY_THRESHOLD) {

                double dynamicBonus = GameConstants.EASY_WIN_BONUS_BASE * ((double) GameConstants.EASY_WIN_ARMY_DIVISOR / Math.max(targetArmies, 1));
                return Math.min(GameConstants.EASY_WIN_STRONG_POSITION_MULTIPLIER, dynamicBonus * GameConstants.EASY_WIN_TERRITORY_THRESHOLD);
            }

            return 0;
        }

        // ===================================================================================
        // קבוצה 4: הגדרות מינימום, תנועת כוחות ומימושי ממשק נוספים
        // ===================================================================================

        @Override
        public double getAttackThreshold() { return thresholds.attackThreshold; }

        @Override
        public double getMinArmyAdvantage() { return thresholds.minArmyAdvantage; }

        @Override
        public double getSetupStackingWeight() { return thresholds.setupStackingWeight; }

        @Override
        public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
            boolean sourceIsSafe = source.getNeighbors().stream()
                    .allMatch(n -> n == target || n.getOwner() == source.getOwner());

            if (sourceIsSafe) return maxMove;
            return Math.max(minMove, Math.min(maxMove, source.getArmies() / GameConstants.ARMOR_DIVISOR));
        }
    }

    // ===================================================================================
    // מחלקות ורשומות עזר (Configurable, Records)
    // ===================================================================================

    /**
     * Configurable implementation with custom troop movement behavior.
     */
    class Configurable extends Abstract {
        private final TroopMovementBehavior movementBehavior;

        public Configurable(HeuristicWeights weights, ThresholdConfig thresholds,
                            double weightFutureThreat, double continentBreakMultiplier,
                            double bonusFocus, double progressFocus, double resistanceAvoidance,
                            TroopMovementBehavior movementBehavior) {
            super(weights, thresholds);
            this.addRule(HeuristicRule.futureThreatRule(), weightFutureThreat);
            this.addRule(HeuristicRule.continentProgressRule(
                            continentBreakMultiplier, bonusFocus, progressFocus, resistanceAvoidance),
                    weights.continentBonus());
            this.movementBehavior = movementBehavior;
        }

        @Override
        public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
            return movementBehavior.calculate(source.getArmies(), minMove, maxMove);
        }

        @FunctionalInterface
        public interface TroopMovementBehavior {
            int calculate(int totalArmies, int minMove, int maxMove);
        }
    }

    /**
     * Encapsulates all heuristic weight parameters.
     */
    record HeuristicWeights(
            double winProbability,
            double continentBonus,
            double strategicValue,
            double expectedCasualties,
            double articulationPointBonus,
            double casualtiesMultiplier,
            double exposurePenaltyMultiplier
    ) {}

    /**
     * Encapsulates threshold configuration.
     */
    record ThresholdConfig(
            double attackThreshold,
            double minArmyAdvantage,
            double setupStackingWeight
    ) {}
}