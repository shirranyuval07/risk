package Model.AIAgent;

import Model.*;
import Model.AIAgent.Strategies.HeuristicStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractHeuristicStrategy implements HeuristicStrategy {

    // משקלי חשיבות (מועברים על ידי האסטרטגיות)
    protected final double weightWinProb;
    protected final double weightContinentBonus;
    protected final double weightStrategicValue;
    protected final double weightExpectedCasualties;

    // מקדמים פנימיים (מועברים על ידי האסטרטגיות כדי לשנות אופי)
    protected final double articulationPointBonus;    // מחליף את ה-5.0
    protected final double casualtiesMultiplier;      // מחליף את ה-0.6
    protected final double exposurePenaltyMultiplier; // מחליף את ה-1.5 (עונש חשיפת עורף)

    // ספים (Thresholds)
    protected final double attackThreshold;
    protected final double minArmyAdvantage;

    private final Map<HeuristicRule, Double> dynamicRules = new HashMap<>();

    public AbstractHeuristicStrategy(
            double wWin, double wCont, double wStrat, double wCost,
            double artBonus, double casMult, double exposurePenalty,
            double threshold, double minAdv) {

        this.weightWinProb = wWin;
        this.weightContinentBonus = wCont;
        this.weightStrategicValue = wStrat;
        this.weightExpectedCasualties = wCost;
        this.articulationPointBonus = artBonus;
        this.casualtiesMultiplier = casMult;
        this.exposurePenaltyMultiplier = exposurePenalty;
        this.attackThreshold = threshold;
        this.minArmyAdvantage = minAdv;
    }
    protected void addRule(HeuristicRule rule, double weight) {
        dynamicRules.put(rule, weight);
    }
    @Override
    public double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {

        // 1. הלוגיקה המקורית והבסיסית שלך (נשארת ללא שינוי)
        double pWin = estimateWinProbability(source.getArmies(), target.getArmies());
        double sStrat = calculateStrategicValue(target, analyzer);
        double cCost = estimateExpectedLoss(target.getArmies(), source.getArmies());

        if (isSourceExposedAfterAttack(source, target, player)) {
            cCost *= this.exposurePenaltyMultiplier;
        }

        double baseScore = (weightWinProb * pWin)
                + (weightStrategicValue * sStrat) - (weightExpectedCasualties * cCost);

        // 2. הפעלת מנוע החוקים הדינמי (OCP Pattern)
        double dynamicScore = 0;
        for (Map.Entry<HeuristicRule, Double> entry : dynamicRules.entrySet()) {
            HeuristicRule rule = entry.getKey();
            Double weight = entry.getValue();

            dynamicScore += rule.evaluate(source, target, player, analyzer) * weight;
        }

        // 3. החזרת הציון הכולל
        return baseScore + dynamicScore;
    }
    // ═══════════════════════════════════════════════════════════════════════════
    //  פונקציות העזר (הועברו מ-HeuristicEvaluator)
    // ═══════════════════════════════════════════════════════════════════════════

    private double estimateWinProbability(int attackerArmies, int defenderArmies) {
        double ratio = (double) (attackerArmies - 1) / Math.max(defenderArmies, 1);
        double prob = 0.5 + (ratio - 1.0) * 0.2;
        return Math.max(0.1, Math.min(0.95, prob));
    }

    private double calculateStrategicValue(Country target, AIGraphAnalyzer analyzer) {
        double score = 0;
        long enemyNeighbors = target.getNeighbors().stream()
                .filter(n -> n.getOwner() != target.getOwner())
                .count();
        score += Math.max(0, 3 - enemyNeighbors) * 0.5;

        Player enemy = target.getOwner();
        if (enemy != null) {
            Set<Country> enemyBottlenecks = analyzer.findArticulationPoints(enemy);
            if (enemyBottlenecks.contains(target)) {
                score += this.articulationPointBonus; // שימוש בפרמטר ההתנהגותי
            }
        }
        return score;
    }

    private double estimateExpectedLoss(int defenderArmies, int attackerArmies) {
        // שימוש ב-Math.max כדי למנוע קריסה של חלוקה ב-0 במקרה של באג
        return ((double) defenderArmies / Math.max(attackerArmies, 1)) * this.casualtiesMultiplier;
    }

    private boolean isSourceExposedAfterAttack(Country source, Country target, Player player) {
        int remainingArmies = source.getArmies() - 1;
        for (Country neighbor : source.getNeighbors()) {
            if (neighbor == target) continue;
            if (neighbor.getOwner() != player && neighbor.getArmies() >= remainingArmies) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double getAttackThreshold() {
        return this.attackThreshold;
    }

    @Override
    public double getMinArmyAdvantage() {
        return this.minArmyAdvantage;
    }
    @Override
    public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
        // בודקים האם למדינת המקור שממנה יצאנו נשארו שכנים עוינים (מלבד זו שכרגע כבשנו)
        boolean isSourceNowSafe = true;
        for (Country neighbor : source.getNeighbors()) {
            if (neighbor != target && neighbor.getOwner() != source.getOwner()) {
                isSourceNowSafe = false;
                break;
            }
        }

        if (isSourceNowSafe) {
            // "אסטרטגיית קליפה" - המרכז בטוח! מעבירים את המקסימום האפשרי קדימה.
            return maxMove;
        } else {
            // אם המקור עדיין בסכנה, הבוט יעביר את המינימום שהוא חייב, או חצי מהכוח
            // כדי להשאיר שם הגנה, תוך הקפדה על חוקי המינימום/מקסימום.
            int halfForce = source.getArmies() / 2;
            return Math.max(minMove, Math.min(maxMove, halfForce));
        }
    }
}