package Model.AIAgent.Rules;

import Model.AIAgent.AIGraphAnalyzer;
import Model.Continent;
import Model.Country;
import Model.Player;

public class ContinentProgressRule implements HeuristicRule {

    private final double enemyBreakMultiplier;
    private final double bonusFocus;
    private final double progressFocus;
    private final double resistanceAvoidance; // תוספת חדשה: רתיעה מהתנגדות

    public ContinentProgressRule(double enemyBreakMultiplier, double bonusFocus, double progressFocus, double resistanceAvoidance) {
        this.enemyBreakMultiplier = enemyBreakMultiplier;
        this.bonusFocus = bonusFocus;
        this.progressFocus = progressFocus;
        this.resistanceAvoidance = resistanceAvoidance;
    }

    @Override
    public double evaluate(Country source, Country target, Player currentPlayer, AIGraphAnalyzer analyzer) {
        Continent continent = target.getContinent();
        int totalCountries = continent.getCountries().size();

        int ownedCountries = 0;
        int myArmiesInContinent = 0;
        int enemyArmiesInContinent = 0;

        // סורקים את היבשת ואוספים מודיעין על כל הכוחות בה
        for (Country c : continent.getCountries()) {
            if (c.getOwner() == currentPlayer) {
                ownedCountries++;
                myArmiesInContinent += c.getArmies();
            } else {
                enemyArmiesInContinent += c.getArmies();
            }
        }

        double progressRatio = (double) (ownedCountries + 1) / totalCountries;
        double bonusScore = continent.getBonusValue() * this.bonusFocus;
        double progressScore = (progressRatio * 5.0) * this.progressFocus;

        // --- התיקון הקריטי: חישוב עונש על התנגדות ---
        // מה יחס הכוחות ביבשת? אם לאויב יש 50 ולנו יש 5, היחס הוא 10!
        double strengthRatio = (double) enemyArmiesInContinent / Math.max(source.getArmies(), 1);

        // קנס גדול שיוריד את החשק להתקרב ליבשות שורצות אויבים
        double resistancePenalty = strengthRatio * this.resistanceAvoidance;

        // הציון עכשיו מקזז את הבונוס עם כמות ההתנגדות!
        double score = bonusScore + progressScore - resistancePenalty;

        // חוק שבירת יבשת של אויב (נשאר כפי שהיה)
        Player enemy = target.getOwner();
        if (enemy != null) {
            boolean enemyOwnsContinent = true;
            for (Country c : continent.getCountries()) {
                if (c.getOwner() != enemy) {
                    enemyOwnsContinent = false;
                    break;
                }
            }
            if (enemyOwnsContinent) {
                score += continent.getBonusValue() * this.enemyBreakMultiplier;
            }
        }

        return score;
    }
}