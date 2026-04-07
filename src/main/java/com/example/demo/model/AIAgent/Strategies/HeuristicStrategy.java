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
 * 
 * תפקידה:
 * - הגדרת ממשק לחישוב ניקוד התקפה (heuristic score)
 * - הגדרת ממשק לקבלת הגדרות AI שונות
 * - ספקת מחשבון הסתברות זכייה בקרבות
 * 
 * תת-מחלקות:
 * - Abstract: הבסיס לכל אסטרטגיה עם חישובי בסיס
 * - Configurable: אסטרטגיה מתאימה לכל משחק
 * 
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

        //  Win Probability Calculator
        private final WinProbabilityCalculator winCalculator = new WinProbabilityCalculator();

        public Abstract(HeuristicWeights weights, ThresholdConfig thresholds) {
            this.weights = weights;
            this.thresholds = thresholds;
        }

        public void addRule(HeuristicRule rule, double weight) {
            dynamicRules.put(rule, weight);
        }

        /**
         * חישוב הציון ההיוריסטי הכולל של התקפה
         * משלב: הסתברות נצחון + ערך אסטרטגי - עלות צפויה + בונוס "קל לנצח"
         * הנוסחה:
         * ציון סופי = (בסיס) + (דינמי) + (בונוס קל לנצח)
         * בסיס = (הסתברות × משקל) + (ערך × משקל) - (עלות × משקל)
         * דינמי = סכום כללי של כללים דינמיים מותאמים אישית
         * בונוס קל = בונוס גדול אם האויב בודד ודעיך
         */
        @Override
        public double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {
            // מרכיבים בסיסיים
            double winProbability = winCalculator.estimate(source.getArmies(), target.getArmies());
            double strategicValue = calculateStrategicValue(target, analyzer);
            double expectedCost = calculateExpectedCost(source, target, player);

            // הרכיבו לציון בסיס
            double baseScore = computeBaseScore(winProbability, strategicValue, expectedCost);
            
            // הוסף כללים דינמיים (ניתנים להתאמה אישית)
            double dynamicScore = computeDynamicScore(source, target, player, analyzer);
            
            // בונוס "קל לנצח" - אם האויב בודד וחלש
            double easyWinBonus = calculateEasyWinBonus(target, player, analyzer);

            return baseScore + dynamicScore + easyWinBonus;
        }

        /**
         * עוזר: בונוס "קל לנצח" לכשהאויב בודד וחלש מאוד
         * זה מעודד בוטים להסיים משחק קרוב לסיום
         * 
         * מערכת הבונוס:
         * - בונוס מדורג: לא 10.0 קבוע, אלא (חיילים אויב / מחלק) לסקילה דינמית
         * - כך הבונוס גדל ככל שהאויב חלש יותר (מדורג)
         * - קבוע לעטיפה/מוקף = בונוס חזק יותר
         */
        private double calculateEasyWinBonus(Country target, Player player, AIGraphAnalyzer analyzer) {
            Player targetOwner = target.getOwner();
            if (targetOwner == null) return 0;
            
            int enemyTerritories = targetOwner.getOwnedCountries().size();
            int enemyNeighbors = analyzer.countEnemyNeighbors(target, player);
            int targetArmies = target.getArmies();
            
            // בונוס לכיבוש המדינה האחרונה - משמעותי מאוד
            if (enemyTerritories == 1) {
                // דינמי: ככל שחלש יותר, בונוס גדול יותר
                double dynamicBonus = GameConstants.EASY_WIN_BONUS_BASE * (GameConstants.EASY_WIN_ARMY_DIVISOR / Math.max(targetArmies, 1));
                return Math.max(GameConstants.EASY_WIN_FINAL_TERRITORY_MULTIPLIER, dynamicBonus);
            }
            
            // בונוס חזק אם:
            // 1. לאויב יש רק 1-2 מדינות נותרות
            // 2. הוא מוקף (2+ אויבים סמוכים)
            // 3. הוא חלש (< 50 חיילים)
            if (enemyTerritories <= GameConstants.EASY_WIN_TERRITORY_THRESHOLD && 
                enemyNeighbors >= GameConstants.EASY_WIN_MIN_NEIGHBORS && 
                targetArmies <= GameConstants.EASY_WIN_ARMY_THRESHOLD) {
                
                // בונוס דינמי מדורג: ככל שחלש יותר, קבוע גדול יותר
                // (50 / 5) = 10, (25 / 5) = 5, (10 / 5) = 2 וכו'
                double dynamicBonus = GameConstants.EASY_WIN_BONUS_BASE * (GameConstants.EASY_WIN_ARMY_DIVISOR / Math.max(targetArmies, 1));
                // החזר מקסימום בין הבונוס הדינמי לבונוס הקבוע (אבל לא יותר מדי)
                return Math.min(GameConstants.EASY_WIN_STRONG_POSITION_MULTIPLIER, dynamicBonus * 2);
            }
            
            return 0;
        }

        /**
         * עוזר: חישוב הציון הבסיסי משלושה גורמים ראשיים
         */
        private double computeBaseScore(double winProb, double stratValue, double cost) {
            return (weights.winProbability * winProb)           // כמה סביר שנזכה
                 + (weights.strategicValue * stratValue)       // כמה חשוב הלכש הזה
                 - (weights.expectedCasualties * cost);        // כמה זה יעלה לנו
        }

        /**
         * עוזר: חישוב ציון דינמי מכללים מותאמים אישית
         * מאפשר התאמה עמוקה של אסטרטגיית AI
         */
        private double computeDynamicScore(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {
            return dynamicRules.entrySet().stream()
                    .mapToDouble(rule -> rule.getKey().evaluate(source, target, player, analyzer) * rule.getValue())
                    .sum();
        }

        /**
         * עוזר: חישוב עלות צפויה של התקפה
         * כולל: הפסדים צפויים + עונש חשיפה אם ננתקנו
         */
        private double calculateExpectedCost(Country source, Country target, Player player) {
            double baseCost = estimateExpectedLoss(target.getArmies(), source.getArmies());
            
            // אם ההתקפה תחשוף אותנו - הוסף עונש
            if (isSourceExposedAfterAttack(source, target, player)) {
                baseCost *= weights.exposurePenaltyMultiplier;
            }
            return baseCost;
        }

        /**
         * עוזר: חישוב הערך האסטרטגי של הלכש יעד
         * משלב: בונוס בדידות + בונוס נקודה קריטית
         */
        private double calculateStrategicValue(Country target, AIGraphAnalyzer analyzer) {
            double isolationBonus = calculateIsolationBonus(target);
            double bottleneckBonus = calculateArticulationBonus(target, analyzer);
            return isolationBonus + bottleneckBonus;
        }

        /**
         * עוזר: בונוס לתכונות בודדות
         * מדינות בודדות (מעט אויבים סמוכים) מפחיתות סכנה
         */
        private double calculateIsolationBonus(Country target) {
            long enemyNeighborsCount = target.getNeighbors().stream()
                    .filter(neighbor -> neighbor.getOwner() != target.getOwner())
                    .count();
            
            // בונוס = (מקסימום בדידות - אויבים ממשיים) × משקל
            return Math.max(MINIMUM_BONUS, DEFAULT_ISOLATION_CHECK - enemyNeighborsCount) * ISOLATION_BONUS_MULTIPLIER;
        }

        /**
         * עוזר: בונוס עבור נקודות ביטחון קריטיות (articulation points)
         * כיבוש כזה נקודה משדרג מאוד את ערכנו
         */
        private double calculateArticulationBonus(Country target, AIGraphAnalyzer analyzer) {
            Player targetOwner = target.getOwner();
            if (targetOwner == null) return MINIMUM_BONUS;

            Set<Country> criticalPoints = analyzer.findArticulationPoints(targetOwner);
            return criticalPoints.contains(target) ? weights.articulationPointBonus : MINIMUM_BONUS;
        }

        /**
         * עוזר: הערכה של הפסדים צפויים בהתקפה
         * הנוסחה: (חיילי הגנה / חיילי התקפה) × משקל הפסדים
         */
        private double estimateExpectedLoss(int defenderArmies, int attackerArmies) {
            return ((double) defenderArmies / Math.max(attackerArmies, 1)) * weights.casualtiesMultiplier;
        }

        /**
         * עוזר: בדיקה אם אנחנו חשופים אחרי התקפה
         * אם יש אויב סמוך שיכול לתקוף בחזור - זה רע!
         */
        private boolean isSourceExposedAfterAttack(Country source, Country target, Player player) {
            int remainingDefense = source.getArmies() - 1;
            
            return source.getNeighbors().stream()
                    .filter(neighbor -> neighbor != target && neighbor.getOwner() != player)
                    .anyMatch(enemy -> enemy.getArmies() >= remainingDefense);
        }

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