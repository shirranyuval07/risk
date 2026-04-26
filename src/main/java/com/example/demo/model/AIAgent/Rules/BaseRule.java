package com.example.demo.model.AIAgent.Rules;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.Logic.AIGraphAnalyzer;
import com.example.demo.model.manager.Continent;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

public interface BaseRule<C> {

    double evaluate(C context, Player player, AIGraphAnalyzer analyzer);

    /**
     * חישוב ציון היוריסטי ליבשת כדי להעריך עד כמה כדאי ל-AI להתמקד בה (לכיבוש, הצבה או הגנה).
     * החישוב מתבסס על מודל "מדרגות דחיפות" - ככל שחסרות פחות מדינות להשלמת היבשת, הציון מזנק באגרסיביות.
     *
     * @param continent             היבשת שעבורה מחושב הציון.
     * @param currentPlayer         השחקן (ה-AI) שמחשב את המהלך ומעריך את הלוח.
     * @param targetOwner           השחקן שמחזיק במדינה שאותה שוקלים לתקוף, או null אם מדובר בהערכת מצב כללית (כמו שלב הצבת החיילים).
     * @param availableArmies       מספר החיילים הפנויים שיש לשחקן הנוכחי לטובת ביצוע המהלך.
     * @param enemyBreakMultiplier  משקולת (מכפיל) המתגמלת שבירת יבשת שלמה שנמצאת בשליטת אויב.
     * @param bonusFocus            משקולת המייצגת את חשיבות בונוס החיילים הרשמי של היבשת.
     * @param progressFocus         משקולת המייצגת את חשיבות ההתקדמות והשליטה ביבשת (לפי מדרגות הדחיפות).
     * @param resistanceAvoidance   משקולת "עונש" הגורמת לבוט להימנע מיבשות שבהן לאויבים יש כוח צבאי גדול מדי.
     * @return                      ציון (double) המייצג את רמת האטרקטיביות של היבשת. ציון גבוה יותר שווה עדיפות גבוהה יותר למהלך.
     */
    static double calculateSharedContinentScore(
            Continent continent,
            Player currentPlayer,
            Player targetOwner,
            int availableArmies,
            double enemyBreakMultiplier,
            double bonusFocus,
            double progressFocus,
            double resistanceAvoidance)
    {
        if (continent == null) return 0;

        int totalCountries = continent.getCountries().size();



        // 1. ספירת המדינות שלך
        int ownedCountries = (int)continent.getCountries().stream()
                .filter(c -> c.getOwner() == currentPlayer)
                .count();

    // 2. סיכום חיילי האויב
        int enemyArmiesInContinent = continent.getCountries().stream()
                .filter(c -> c.getOwner() != currentPlayer)
                .mapToInt(Country::getArmies)
                .sum();

        boolean isAttack = (targetOwner != null);
        if (isAttack)
            ownedCountries++;
        // מדמים שכבשנו את המדינה הזו עכשיו
        // חישוב כמה מדינות נשארו לנו כדי לסיים את היבשת.
        // ה-Math.max(1, ...) מוודא שלעולם לא נחלק באפס במקרה שכבר השתלטנו על כולה.
        int missingCountries = Math.max(1, totalCountries - ownedCountries);

        // ככל שחסרות פחות מדינות, המכנה קטן, והתוצאה מזנקת למעלה באגרסיביות.
        double urgencyStep = (double) ownedCountries / missingCountries;

        double bonusScore = (continent.getBonusValue() * urgencyStep) * bonusFocus;

        double progressScore = urgencyStep * progressFocus;

        double strengthRatio = (double) enemyArmiesInContinent / Math.max(availableArmies, 1);

        double resistancePenalty = strengthRatio * resistanceAvoidance;

        double score = bonusScore + progressScore - resistancePenalty;

        if (isAttack && targetOwner != currentPlayer)
        {
            boolean enemyOwnsContinent = continent.getCountries().stream()
                    .allMatch(c -> c.getOwner() == targetOwner);
            if (enemyOwnsContinent)
                score += continent.getBonusValue() * enemyBreakMultiplier;
        }

        return score;
    }
}