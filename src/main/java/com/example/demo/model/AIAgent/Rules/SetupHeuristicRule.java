package com.example.demo.model.AIAgent.Rules;

import com.example.demo.model.AIAgent.Logic.AIGraphAnalyzer;
import com.example.demo.model.manager.Continent;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

/**
 * כלל היוריסטי לשלב ה-Setup - מעריך מדינה בודדת להצבת חייל.
 * בניגוד ל-HeuristicRule (שמעריך התקפה source→target),
 * כאן מעריכים מדינה בודדת שכבר בבעלותנו.
 */
@FunctionalInterface
public interface SetupHeuristicRule extends BaseRule<Country>
{

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
    static SetupHeuristicRule enemyThreatRule()
    {
        return (country, player, analyzer) ->
                country.getNeighbors().stream()
                        .filter(neighbor -> neighbor.getOwner() != player)
                        .mapToInt(Country::getArmies)
                        .sum();
    }

    /**
     * כלל: העדפת ערימה – בונוס למדינות שכבר יש בהן חיילים שלנו.
     * ניקוד : log(חיילים + 1) – תשואה פוחתת כדי למנוע ערימה אינסופית.
     */
    static SetupHeuristicRule stackingRule()
    {
        return (country, player, analyzer) -> Math.log(country.getArmies() + 1);
    }

    /**
     * כלל: התקדמות ביבשת – בונוס למדינות ביבשות שאנחנו מתקדמים בהן.
     * ניקוד = (אחוז השליטה ביבשת) × (ערך הבונוס של היבשת).
     */
    static SetupHeuristicRule continentProgressRule(double enemyBreakMultiplier, double bonusFocus, double progressFocus, double resistanceAvoidance) {
        return (country, player, analyzer) ->
                BaseRule.calculateSharedContinentScore(
                country.getContinent(),
                player,
                null,
                1,
                enemyBreakMultiplier,
                bonusFocus,
                progressFocus,
                resistanceAvoidance);
    }

    /**
     * כלל: כיסוי גבולות – בונוס שלילי למדינות שגובלות עם הרבה אויבים שונים.
     * ניקוד = מספר שכנים עוינים.
     */
    static SetupHeuristicRule borderCoverageRule()
    {
        return (country, player, analyzer) ->
                analyzer.countEnemyNeighbors(country, player) * -1;
    }
}

