package Model.AIAgent;

import Model.Continent;
import Model.Country;
import Model.Player;

import java.util.Set;

import static Model.AIAgent.GreedyAI.*;

public class HeuristicEvaluator
{
    private final AIGraphAnalyzer graphAnalyzer;

    public HeuristicEvaluator(AIGraphAnalyzer graphAnalyzer)
    {
        this.graphAnalyzer = graphAnalyzer;
    }


    /**
     * מחשב ציון תועלת לזוג (מקור, יעד) לפי הנוסחה:

     *   S = (W_WIN × P_win) + (W_BONUS × V_bonus) + (W_STRAT × S_strat) - (W_COST × C_cost)
     */
    public double calculateHeuristic(Country source, Country target, Player player) {

        // פרמטר 1: הסתברות ניצחון – מחושב לפי יחס כוחות, בתחום [0.1, 0.95]
        double pWin = estimateWinProbability(source.getArmies(), target.getArmies());

        // פרמטר 2: ערך יבשת – השלמת יבשת שלי, או שבירת יבשת של אויב
        double vBonus = 0;
        Continent continent = target.getContinent();
        if (wouldCompleteMyContinent(player, continent, target))
        {
            vBonus += continent.getBonusValue();         // השלמת יבשת – רווח מלא
        }
        Player targetOwner = target.getOwner();
        if (targetOwner != null && ownsContinent(targetOwner, continent)) {
            vBonus += continent.getBonusValue() * 0.8;  // שבירת יבשת אויב – רווח חלקי
        }

        // פרמטר 3: ערך אסטרטגי – מדינת צוואר בקבוק שווה יותר
        double sStrat = calculateStrategicValue(target);

        // פרמטר 4: עלות הסיכון – תוחלת אובדן + עונש אם העורף ייחשף
        double cCost = estimateExpectedLoss(target.getArmies());
        if (isSourceExposedAfterAttack(source, target, player)) {
            cCost *= 1.5;
        }

        return (W_WIN * pWin) + (W_BONUS * vBonus) + (W_STRAT * sStrat) - (W_COST * cCost);
    }

    /** בודק אם player שולט בכל מדינות continent. */
    private boolean ownsContinent(Player player, Continent continent)
    {
        return continent.getCountries().stream().allMatch(c -> c.getOwner() == player);
    }

    private boolean wouldCompleteMyContinent(Player player, Continent continent, Country target)
    {
        for (Country c : continent.getCountries())
        {
            // מתעלמים מהמטרה עצמה (כי אנחנו מניחים שניצחנו ונכבוש אותה)
            if (c == target)
                continue;
            // אם יש מדינה אחרת ביבשת שעדיין לא שייכת לנו - לא נשלים את היבשת
            if (c.getOwner() != player)
                return false;
        }
        // אם סיימנו את הלולאה, זה אומר שכל שאר המדינות ביבשת כבר שלנו!
        return true;
    }
    // ═══════════════════════════════════════════════════════════════════════════
    //  פונקציות עזר חישוביות
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * מעריך הסתברות ניצחון לפי יחס כוחות, מקוצץ ל-[0.1, 0.95].
     *
     */
    private double estimateWinProbability(int attackerArmies, int defenderArmies)
    {
        double ratio = (double)(attackerArmies - 1) / Math.max(defenderArmies, 1);
        double prob  = 0.5 + (ratio - 1.0) * 0.2;
        return Math.max(0.1, Math.min(0.95, prob));
    }

    /** תוחלת אובדן חיילים בקרב – מקדם 0.6 מהניסיון האמפירי של מנגנון הקוביות. */
    private double estimateExpectedLoss( int defenderArmies)
    {
        return defenderArmies * 0.6;
    }

    /**
     * ערך אסטרטגי מתקדם מבוסס תורת הגרפים.
     * בוחן האם מדינת היעד מקלה על ההגנה, והאם היא מהווה "נקודת חיתוך" באימפריה של היריב.
     */
    private double calculateStrategicValue(Country target)
    {
        double score = 0;
        //  הערכה בסיסית: טריטוריות עם פחות "מוצאים עוינים" קלות יותר להגנה
        long enemyNeighbors = target.getNeighbors().stream()
                .filter(n -> n.getOwner() != target.getOwner())
                .count();
        score += Math.max(0, 3 - enemyNeighbors) * 0.5;
        // זיהוי צווארי בקבוק טופולוגיים
        Player enemy = target.getOwner();
        if (enemy != null)
        {
            // חישוב נקודות החיתוך (Articulation Points) בתת-הגרף של האויב
            Set<Country> enemyBottlenecks = graphAnalyzer.findArticulationPoints(enemy);

            // אם המדינה המותקפת היא גשר קריטי המחבר את הטריטוריות של האויב
            if (enemyBottlenecks.contains(target))
            {
                score += 5.0; // בונוס אסטרטגי ענק על ביתור רצף האויב
                System.out.println("[AI STRATEGY] Identified " + target.getName() + " as a critical enemy bottleneck!");
            }
        }

        return score;
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
}
