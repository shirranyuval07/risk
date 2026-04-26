package com.example.demo.model.AIAgent.Rules;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.Logic.AIGraphAnalyzer;
import com.example.demo.model.manager.Continent;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

@FunctionalInterface
public interface HeuristicRule extends BaseRule<HeuristicRule.AttackContext> {

    /** הקשר התקפה – עוטף source + target לתוך אובייקט אחד כדי להתאים לחתימת BaseRule<C> */
    record AttackContext(Country source, Country target) {}

    /**
     * המתודה המופשטת היחידה – מקבלת AttackContext (כנדרש ע"י BaseRule<AttackContext>).
     */
    @Override
    double evaluate(AttackContext context, Player currentPlayer, AIGraphAnalyzer analyzer);

    /**
     * חוק הערכה שמודד את האיום העתידי של התקפה על מדינה מסוימת. הוא סורק את שכני היעד (המדינה המותקפת) ומחשב את האיום הגדול ביותר שהם מציבים
     * בהתבסס על מספר החיילים שלהם. התוצאה מחולקת במספר החיילים במדינת המקור כדי לקבל יחס שמייצג את רמת האיום העתידי.
     * * @return HeuristicRule אובייקט חוק לחישוב ציון האיום העתידי.
     */
    static HeuristicRule futureThreatRule()
    {
        return (ctx, currentPlayer, analyzer) ->
        {
            int maxThreat = 0;
            // סורקים את כל השכנים של המדינה המותקפת (היעד)
            for (Country neighbor : ctx.target().getNeighbors()) {
                if (neighbor.getOwner() != currentPlayer && neighbor.getArmies() > maxThreat)
                    maxThreat = neighbor.getArmies();

            }
            return (double) maxThreat / Math.max(ctx.source().getArmies(), 1) * -1;
        };
    }

    /**
     * חוק הערכה שמודד עד כמה ההתקפה מקדמת את ה-AI לקראת שליטה מלאה ביבשת של מדינת היעד,
     * או לחלופין, עד כמה היא שוברת מונופול של שחקן יריב על אותה יבשת.
     * החישוב עצמו מתבצע באמצעות הפעולה המשותפת ב-BaseRule כדי למנוע כפילות קוד בין חוקי ההתקפה לחוקי ההצבה (Setup).
     * * @param enemyBreakMultiplier משקל הבונוס שיינתן אם ההתקפה תשבור שליטה מלאה של אויב ביבשת.
     * @param bonusFocus משקל החשיבות של ערך הבונוס שמספקת היבשת (יבשות שנותנות יותר חיילים יקבלו עדיפות).
     * @param progressFocus משקל החשיבות הניתן לאחוז השליטה הנוכחי של ה-AI ביבשת (מעודד סיום כיבוש יבשות שכבר הותחלו).
     * @param resistanceAvoidance עונש המופחת מהציון ככל שיש יותר נוכחות של חיילי אויב ביבשת (מעודד הימנעות מקרבות התשה).
     * @return HeuristicRule אובייקט חוק לחישוב ציון ההתקדמות ביבשת.
     */
    static HeuristicRule continentProgressRule(double enemyBreakMultiplier, double bonusFocus, double progressFocus, double resistanceAvoidance) {
        return (ctx, currentPlayer, analyzer) ->
                BaseRule.calculateSharedContinentScore(
                        ctx.target().getContinent(),
                        currentPlayer,
                        ctx.target().getOwner(),
                        ctx.source().getArmies(),
                        enemyBreakMultiplier,
                        bonusFocus,
                        progressFocus,
                        resistanceAvoidance);
    }

    /**
     * חוק הערכה שמטרתו לאתר "ניצחונות קלים" (Easy Wins) כדי להבטיח ל-AI השגת קלף בסוף התור
     * במינימום מאמץ וסיכון של איבוד חיילים.
     * החוק בודק אם יחסי הכוחות נוטים בבירור לטובת ה-AI (מדינת יעד חלשה ומדינת מקור חזקה, בהתאם לקבועי המשחק).
     * אם התנאים מתקיימים, מוחזר ציון גבוה מאוד שמעודד את ה-AI לתקוף. אם לא, מוחזר ציון מינימלי בסיסי.
     * * @return HeuristicRule אובייקט חוק לחישוב הכדאיות של התקפה לצורך השגת קלף.
     */
    static HeuristicRule cardFarmingRule()
    {
        return (ctx, currentPlayer, analyzer) ->
        {
            if (ctx.target().getArmies() <= GameConstants.EASY_WIN_MIN_NEIGHBORS && ctx.source().getArmies() >= GameConstants.EASY_WIN_MAX_NEIGHBORS)
                return GameConstants.EASY_WIN_ARMY_THRESHOLD; // מעודד מאוד התקפה קלה ומהירה

            return GameConstants.EASY_WIN_BONUS_BASE; // מונע ממתקפות מסוכנות יותר
        };
    }
}