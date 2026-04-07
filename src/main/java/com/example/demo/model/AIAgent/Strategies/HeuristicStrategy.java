package com.example.demo.model.AIAgent.Strategies;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.AIAgent.Rules.HeuristicRule;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Interface and implementations for AI heuristic decision-making.
 * Consolidated into a single file to reduce class explosion while maintaining OCP.
 */
public interface HeuristicStrategy {
    double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer);
    double getAttackThreshold();
    double getMinArmyAdvantage();
    int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove);
    double getSetupStackingWeight();

    /**
     * Base implementation with core heuristic logic.
     */
    abstract class Abstract implements HeuristicStrategy {

        // קבועים עבור פונקציות ההערכה (מניעת מספרי קסם)
        private static final int MASSIVE_ARMY_THRESHOLD = 50;
        private static final double CERTAIN_WIN_RATIO = 0.95;
        private static final double CERTAIN_LOSS_RATIO = 0.80;
        private static final double CERTAIN_WIN_PROB = 0.99;
        private static final double CERTAIN_LOSS_PROB = 0.01;
        private static final double MASSIVE_TRANSITION_BASE_PROB = 0.5;
        private static final double MASSIVE_TRANSITION_OFFSET = 0.85;
        private static final double MASSIVE_TRANSITION_MULTIPLIER = 5.0;

        private static final double HIGH_ADVANTAGE_RATIO = 1.5;
        private static final double HIGH_ADVANTAGE_PROB = 0.95;
        private static final double MODERATE_ADVANTAGE_RATIO = 1.0;
        private static final double MODERATE_ADVANTAGE_PROB = 0.75;
        private static final double EQUILIBRIUM_RATIO = 0.924;
        private static final double EQUILIBRIUM_PROB = 0.50;
        private static final double MIN_WIN_PROB = 0.05;
        private static final double BELOW_EQUILIBRIUM_MULTIPLIER = 2.0;

        private static final int ISOLATION_BASE_NEIGHBORS = 3;
        private static final double ISOLATION_BONUS_MULTIPLIER = 0.5;

        private static final int SAFE_MOVEMENT_DIVISOR = 2;

        protected final double weightWinProb;
        protected final double weightContinentBonus;
        protected final double weightStrategicValue;
        protected final double weightExpectedCasualties;
        protected final double articulationPointBonus;
        protected final double casualtiesMultiplier;
        protected final double exposurePenaltyMultiplier;
        protected final double setupStackingWeight;
        protected final double attackThreshold;
        protected final double minArmyAdvantage;
        private final Map<HeuristicRule, Double> dynamicRules = new HashMap<>();

        // טבלת הסתברויות ניצחון מבוססת מחקר (Markov Chain Win Probabilities)
        // מה זה עושה: שומר מראש את אחוזי ההצלחה המדויקים לקרבות קטנים.
        // על מה זה מבוסס: המחקרים של ג'ייסון אוסבורן (2003) והאריס גיאורגיו (2004) הוכיחו
        // שחישוב מדויק של תוצאות הקרב חייב להיעשות דרך מודל של "שרשראות מרקוב עם מצבים בולעים".
        // מכיוון שחישוב זה כבד מדי לזמן ריצה אמיתי,  אפשר לשלוף את התוצאות שהוכנו מראש.
        // זה מאפשר ל-AI להבין שבמספרים קטנים (למשל 4 מול 3) יש למזל תפקיד מכריע,
        // ולא לצאת להרפתקאות מסוכנות.
        private static final double[][] MARKOV_WIN_PROBABILITIES = {
                // מגנים: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
                {0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // 0 תוקפים (לא חוקי במשחק)
                {1.00, 0.41, 0.10, 0.02, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // 1 תוקף (תכלס 2 חיילים במדינה)
                {1.00, 0.59, 0.22, 0.07, 0.02, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // 2 תוקפים
                {1.00, 0.75, 0.36, 0.20, 0.09, 0.04, 0.02, 0.00, 0.00, 0.00, 0.00}, // 3 תוקפים
                {1.00, 0.81, 0.47, 0.31, 0.16, 0.08, 0.04, 0.02, 0.00, 0.00, 0.00}, // 4 תוקפים
                {1.00, 0.86, 0.57, 0.42, 0.25, 0.14, 0.07, 0.04, 0.02, 0.00, 0.00}, // 5 תוקפים
                {1.00, 0.89, 0.65, 0.52, 0.34, 0.21, 0.12, 0.06, 0.03, 0.01, 0.00}, // 6 תוקפים
                {1.00, 0.91, 0.72, 0.60, 0.43, 0.28, 0.17, 0.10, 0.05, 0.03, 0.01}, // 7 תוקפים
                {1.00, 0.93, 0.77, 0.68, 0.52, 0.36, 0.24, 0.15, 0.08, 0.04, 0.02}, // 8 תוקפים
                {1.00, 0.94, 0.82, 0.74, 0.60, 0.44, 0.31, 0.20, 0.12, 0.07, 0.04}, // 9 תוקפים
                {1.00, 0.95, 0.85, 0.79, 0.67, 0.52, 0.39, 0.27, 0.17, 0.10, 0.06}  // 10 תוקפים
        };

        public Abstract(double wWin, double wCont, double wStrat, double wCost,
                        double artBonus, double casMult, double exposurePenalty,
                        double threshold, double minAdv, double setupStackingWeight) {
            this.weightWinProb = wWin;
            this.weightContinentBonus = wCont;
            this.weightStrategicValue = wStrat;
            this.weightExpectedCasualties = wCost;
            this.articulationPointBonus = artBonus;
            this.casualtiesMultiplier = casMult;
            this.exposurePenaltyMultiplier = exposurePenalty;
            this.attackThreshold = threshold;
            this.minArmyAdvantage = minAdv;
            this.setupStackingWeight = setupStackingWeight;
        }

        protected void addRule(HeuristicRule rule, double weight) {
            dynamicRules.put(rule, weight);
        }

        // פונקציית העל של הגישה החמדנית (Greedy Heuristic Calculation)
        // מה זה עושה: נותנת ציון (Score) לכל מהלך אפשרי. ככל שהציון גבוה יותר,
        // ה-AI יעדיף לבצע את המהלך הזה קודם.

        @Override
        public double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {
            double pWin = estimateWinProbability(source.getArmies(), target.getArmies());
            double sStrat = calculateStrategicValue(target, analyzer);
            double cCost = estimateExpectedLoss(target.getArmies(), source.getArmies());

            // עונש חשיפה: אם התקיפה תשאיר את המדינה שלנו חשופה להתקפת נגד, המחיר שלה עולה.
            if (isSourceExposedAfterAttack(source, target, player)) {
                cCost *= this.exposurePenaltyMultiplier;
            }

            // חישוב הפונקציה הליניארית: סיכוי ניצחון + ערך אסטרטגי פחות צפי אבדות.
            double baseScore = (weightWinProb * pWin) + (weightStrategicValue * sStrat) - (weightExpectedCasualties * cCost);

            double dynamicScore = 0;
            for (Map.Entry<HeuristicRule, Double> entry : dynamicRules.entrySet()) {
                dynamicScore += entry.getKey().evaluate(source, target, player, analyzer) * entry.getValue();
            }
            return baseScore + dynamicScore;
        }

        // הערכת סיכויי ניצחון א-פריוריים (A-Priori Probability Estimation)
        // מה זה עושה: מקבל כמות תוקפים ומגנים ומחזיר מספר בין 0 ל-1 המייצג את סיכוי ההצלחה.
        private double estimateWinProbability(int attackerArmies, int defenderArmies) {
            // החייל הראשון חייב להישאר בשטח שתוקפים ממנו ולכן לא נכנס לסטטיסטיקה של הקרב.
            int actualAttackers = attackerArmies - 1;

            if (actualAttackers <= 0) return 0.0;
            if (defenderArmies <= 0) return 1.0;

            int maxIndex = MARKOV_WIN_PROBABILITIES.length - 1;

            // 1. הילוך ראשון (עד 10 חיילים): שרשראות מרקוב
            // מבוסס על הצורך לדייק במספרים קטנים שבהם השונות והמזל (הקוביות) משחקים תפקיד מרכזי.
            if (actualAttackers <= maxIndex && defenderArmies <= MARKOV_WIN_PROBABILITIES[0].length - 1) {
                return MARKOV_WIN_PROBABILITIES[actualAttackers][defenderArmies];
            }

            double ratio = (double) actualAttackers / defenderArmies;

            // 2. הילוך שלישי: מספרי ענק (Massive Armies) - מעל 50 חיילים
            // על מה זה מבוסס: "חוק המספרים הגדולים" בסטטיסטיקה (Law of Large Numbers).
            // כשמטילים קוביות מאות פעמים, הסטייה מהתוחלת מתאפסת לחלוטין.
            // לכן קרב של 100 תוקפים מול 100 מגנים לא יגמר בשיוויון, אלא התוקף ינצח
            // בוודאות כמעט מוחלטת (כי התוחלת לגלגול בודד היא לטובתו).
            if (actualAttackers > MASSIVE_ARMY_THRESHOLD || defenderArmies > MASSIVE_ARMY_THRESHOLD) {
                if (ratio > CERTAIN_WIN_RATIO) return CERTAIN_WIN_PROB; // התוקף שוחק את המגן וינצח ב-99%
                if (ratio < CERTAIN_LOSS_RATIO) return CERTAIN_LOSS_PROB; // התוקף יישחק קודם ויפסיד בוודאות
                return MASSIVE_TRANSITION_BASE_PROB + ((ratio - MASSIVE_TRANSITION_OFFSET) * MASSIVE_TRANSITION_MULTIPLIER); // טווח מעבר למקרה שהכוחות סופר-שקולים (למשל 90 מול 100)
            }

            // 3. הילוך שני: מספרי ביניים (למשל 30 מול 20)
            // על מה זה מבוסס: המחקר המתמטי של אוסבורן. הוא הוכיח ש"נקודת שיווי המשקל"
            // בקרבות מרובי חיילים אינה יחס של 1:1, אלא יחס של 0.924.
            // כלומר, כדי להגיע ל-50% סיכוי ניצחון, התוקף זקוק רק ל-92.4% מהכוח של המגן.
            if (ratio >= HIGH_ADVANTAGE_RATIO) return HIGH_ADVANTAGE_PROB;
            if (ratio >= MODERATE_ADVANTAGE_RATIO) return MODERATE_ADVANTAGE_PROB;
            if (ratio >= EQUILIBRIUM_RATIO) return EQUILIBRIUM_PROB; // הוכחת אוסבורן לשיווי משקל

            // מתחת ליחס של 0.924, הסיכוי של התוקף צונח ליניארית
            return Math.max(MIN_WIN_PROB, EQUILIBRIUM_PROB - ((EQUILIBRIUM_RATIO - ratio) * BELOW_EQUILIBRIUM_MULTIPLIER));
        }

        // חישוב ערך אסטרטגי (Strategic Value Calculation)
        // מה זה עושה: מעריך כמה חשוב טקטית לכבוש מדינה מסוימת ללא קשר לכמות החיילים בה.
        private double calculateStrategicValue(Country target, AIGraphAnalyzer analyzer) {
            double score = 0;
            // בונוס קטן על כיבוש מדינה מבודדת יחסית (שיש לה פחות שכנים עוינים שיחזירו תקיפה)
            long enemyNeighbors = target.getNeighbors().stream().filter(n -> n.getOwner() != target.getOwner()).count();
            score += Math.max(0, ISOLATION_BASE_NEIGHBORS - enemyNeighbors) * ISOLATION_BONUS_MULTIPLIER;

            Player enemy = target.getOwner();
            if (enemy != null) {
                // על מה זה מבוסס: תורת הגרפים ואלגוריתם טרג'אן (Tarjan).
                // ה-AIGraphAnalyzer מחפש "נקודות חיתוך" (Articulation Points) בגרף המדינות.
                // נקודת חיתוך היא מדינה שאם נכבוש אותה, נחתוך את האימפריה של האויב לשני חלקים
                // שאינם מחוברים זה לזה. לכן ה-AI מקבל בונוס משמעותי לתקוף צווארי בקבוק אלו.
                Set<Country> enemyBottlenecks = analyzer.findArticulationPoints(enemy);
                if (enemyBottlenecks.contains(target)) {
                    score += this.articulationPointBonus;
                }
            }
            return score;
        }

        // הערכת תוחלת אובדן חיילים (Expected Loss / Casualties)
        // על מה זה מבוסס: היוריסטיקה של הערכת סיכונים. ה-AI לוקח בחשבון שגם אם ינצח,
        // הוא עלול לאבד יותר מדי חיילים ביחס לרווח האסטרטגי. אנו מניחים באופן פשטני
        // שמספר האבדות פרופורציונלי ליחס הכוחות (ככל שיש לי יותר כוח מהמגן, אאבד פחות).
        private double estimateExpectedLoss(int defenderArmies, int attackerArmies) {
            return ((double) defenderArmies / Math.max(attackerArmies, 1)) * this.casualtiesMultiplier;
        }

        // מה זה עושה: בודק אם לאחר התקיפה (בהנחה שננצח ונעביר חיילים), המדינה שממנה נעשתה התקיפה
        // תישאר עם מעט מדי חיילים מול שכן עוין חזק. מונע מה-AI לעשות "חורים" בהגנה שלו.
        private boolean isSourceExposedAfterAttack(Country source, Country target, Player player) {
            int remainingArmies = source.getArmies() - 1;
            for (Country neighbor : source.getNeighbors()) {
                if (neighbor == target) continue; // לא אכפת  מהיעד שאנחנו תוקפים כעת
                if (neighbor.getOwner() != player && neighbor.getArmies() >= remainingArmies) return true;
            }
            return false;
        }

        @Override public double getAttackThreshold() { return this.attackThreshold; }
        @Override public double getMinArmyAdvantage() { return this.minArmyAdvantage; }
        @Override public double getSetupStackingWeight() { return this.setupStackingWeight; }

        @Override
        public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
            boolean isSourceNowSafe = true;
            for (Country neighbor : source.getNeighbors()) {
                if (neighbor != target && neighbor.getOwner() != source.getOwner()) {
                    isSourceNowSafe = false;
                    break;
                }
            }
            // אם המדינה המקורית בטוחה (אין לה שכנים עוינים), נזרים את כל החיילים האפשריים החוצה לחזית.
            // אם היא לא בטוחה, נעביר לכל היותר חצי מהכוח כדי להשאיר משמר אחורי.
            if (isSourceNowSafe) return maxMove;
            return Math.max(minMove, Math.min(maxMove, source.getArmies() / SAFE_MOVEMENT_DIVISOR));
        }
    }

    /**
     * Concrete implementation that allows external configuration of weights and behaviors.
     */
    class Configurable extends Abstract {
        private final TroopMovementBehavior movementBehavior;

        public Configurable(double wWin, double wCont, double wStrat, double wCost, double artBonus,
                            double casMult, double expPenalty, double attackThresh, int minAdv,
                            double weightFutureThreat, double continentBreakMultiplier, double bonusFocus,
                            double progressFocus, double resistanceAvoidance, double setupWeight,
                            TroopMovementBehavior movementBehavior) {
            super(wWin, wCont, wStrat, wCost, artBonus, casMult, expPenalty, attackThresh, minAdv, setupWeight);
            this.addRule(HeuristicRule.futureThreatRule(), weightFutureThreat);
            this.addRule(HeuristicRule.continentProgressRule(continentBreakMultiplier, bonusFocus, progressFocus, resistanceAvoidance), wCont);
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
}