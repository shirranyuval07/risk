package Model.AIAgent;

import Model.*;

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

    @Override
    public double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {

        // 1. חישוב הרווח הפוטנציאלי (תועלת)
        double pWin = estimateWinProbability(source.getArmies(), target.getArmies());
        double vBonus = calculateContinentValue(target, player);
        double sStrat = calculateStrategicValue(target, analyzer);

        // 2. חישוב המחיר והסיכון (עלות) - עכשיו משתמש ביחס בין התוקף למגן!
        double cCost = estimateExpectedLoss(target.getArmies(), source.getArmies());

        // עונש הגנתי אם התקיפה תשאיר את העורף שלנו חשוף
        if (isSourceExposedAfterAttack(source, target, player)) {
            cCost *= this.exposurePenaltyMultiplier;
        }

        // 3. שקלול סופי לפי ה"אופי" של הבוט
        return (weightWinProb * pWin) + (weightContinentBonus * vBonus)
                + (weightStrategicValue * sStrat) - (weightExpectedCasualties * cCost);
    }
    // ═══════════════════════════════════════════════════════════════════════════
    //  פונקציות העזר (הועברו מ-HeuristicEvaluator)
    // ═══════════════════════════════════════════════════════════════════════════

    private double estimateWinProbability(int attackerArmies, int defenderArmies) {
        double ratio = (double)(attackerArmies - 1) / Math.max(defenderArmies, 1);
        double prob  = 0.5 + (ratio - 1.0) * 0.2;
        return Math.max(0.1, Math.min(0.95, prob));
    }

    private double calculateContinentValue(Country target, Player player) {
        double vBonus = 0;
        Continent continent = target.getContinent();
        if (wouldCompleteMyContinent(player, continent, target)) {
            vBonus += continent.getBonusValue();
        }

        Player targetOwner = target.getOwner();
        if (targetOwner != null && ownsContinent(targetOwner, continent)) {
            vBonus += continent.getBonusValue() * 0.8;
        }
        return vBonus;
    }

    private boolean ownsContinent(Player player, Continent continent) {
        return continent.getCountries().stream().allMatch(c -> c.getOwner() == player);
    }

    private boolean wouldCompleteMyContinent(Player player, Continent continent, Country target) {
        for (Country c : continent.getCountries()) {
            if (c == target) continue;
            if (c.getOwner() != player) return false;
        }
        return true;
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
    public double getAttackThreshold() { return this.attackThreshold; }

    @Override
    public double getMinArmyAdvantage() { return this.minArmyAdvantage; }
}