package com.example.demo.model.AIAgent.Rules;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.manager.Continent;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

/**
 * כלל היוריסטי לשלב ה-Setup - מעריך מדינה בודדת להצבת חייל.
 * בניגוד ל-HeuristicRule (שמעריך התקפה source→target),
 * כאן מעריכים מדינה בודדת שכבר בבעלותנו.
 */
@FunctionalInterface
public interface SetupHeuristicRule extends BaseRule<Country> {

    /**
     * @param country  המדינה המוערכת להצבה
     * @param player   השחקן שלנו
     * @param analyzer כלי ניתוח הגרף
     * @return ציון מספרי – ככל שגבוה יותר, המדינה עדיפה להצבה
     */
    double evaluate(Country country, Player player, AIGraphAnalyzer analyzer);

    /**
     * כלל: העדפת מדינות עם איום אויב גבוה בסביבתן.
     * ניקוד = סכום חיילי אויב בשכנים.
     */
    static SetupHeuristicRule enemyThreatRule() {
        return (country, player, analyzer) -> {
            int totalEnemyStrength = 0;
            for (Country neighbor : country.getNeighbors()) {
                if (neighbor.getOwner() != player)
                    totalEnemyStrength += neighbor.getArmies();
            }
            return totalEnemyStrength;
        };
    }

    /**
     * כלל: העדפת ערימה – בונוס למדינות שכבר יש בהן חיילים שלנו.
     * ניקוד = log(חיילים + 1) – תשואה פוחתת כדי למנוע ערימה אינסופית.
     * אסטרטגיה הגנתית (משקל 20) עדיין תעדיף ערימה, אבל מאוזנת/התקפית יתפזרו.
     */
    static SetupHeuristicRule stackingRule() {
        return (country, player, analyzer) -> Math.log(country.getArmies() + 1);
    }

    /**
     * כלל: התקדמות ביבשת – בונוס למדינות ביבשות שאנחנו מתקדמים בהן.
     * ניקוד = (אחוז השליטה ביבשת) × (ערך הבונוס של היבשת).
     */
    static SetupHeuristicRule continentProgressRule() {
        return (country, player, analyzer) -> {
            Continent continent = country.getContinent();
            if (continent == null) return 0;

            int totalCountries = continent.getCountries().size();
            int ownedCountries = 0;
            for (Country c : continent.getCountries()) {
                if (c.getOwner() == player)
                    ownedCountries++;
            }

            double progressRatio = (double) ownedCountries / totalCountries;
            return progressRatio * continent.getBonusValue();
        };
    }

    /**
     * כלל: כיסוי גבולות – בונוס למדינות שגובלות עם הרבה אויבים שונים.
     * ניקוד = מספר שכנים עוינים.
     */
    static SetupHeuristicRule borderCoverageRule() {
        return (country, player, analyzer) ->
                analyzer.countEnemyNeighbors(country, player);
    }
}

