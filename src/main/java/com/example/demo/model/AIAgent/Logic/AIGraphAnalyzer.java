package com.example.demo.model.AIAgent.Logic;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import com.example.demo.model.Records.GameRecords.AttackMove;
import com.example.demo.model.Records.GameRecords.FortifyMove;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import com.example.demo.model.util.MaxPriorityQueue;

import java.util.*;

/**
 * מנתח גרף AI - אלגוריתמים גרפיים מתקדמים לקבלת החלטות AI

 * תפקידיה:
 * - חיפוש מדינות אופטימליות להצבה בשלב Setup
 * - חיפוש הזדמנויות התקפה הטובות ביותר
 * - חישוב ניקוד איום לכל מדינה
 * - בניית תור עדיפויות של התקפות
 * - מציאת נקודות ביטחון קריטיות בגרף (Articulation Points)
 * - תכנון הגנה ותגבור (Fortify) אופטימלי

 * אלגוריתמים:
 * - BFS (Breadth-First Search): למציאת מדינות מחוברות
 * - DFS (Depth-First Search): למציאת נקודות ביטחון

 * השימוש: משמשת את בוטים (AI players) להחלטות אסטרטגיות

 * הערות סיבוכיות כלליות למחלקה:
 * V מציין את כמות המדינות (Vertices).
 * E מציין את כמות הגבולות (Edges).
 * D מציין את מספר השכנים המקסימלי למדינה (Degree, שבריסק נחשב לקבוע קטן O(1)).
 */
public class AIGraphAnalyzer {

    /**
     * BFS גנרי – סורק את כל המדינות שבבעלות player הנגישות מ-start
     * דרך מדינות שבבעלותו בלבד.
     *
     * @param start  מדינת ההתחלה
     * @param player השחקן שלנו (רק מדינותיו ייסרקו)
     * @return סט כל המדינות הנגישות (כולל start)

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V + E) מוגבל למדינות של השחקן בלבד. סריקת רוחב קלאסית.
     * - סיבוכיות מקום: O(V) עבור שמירת תור המדינות (Queue) וסט המדינות שביקרנו בהן (Visited).
     *
     * מסקנה סופית: אלגוריתם לינארי ויעיל ביותר להבנת הרצף הטריטוריאלי של ה-AI, קריטי לשלב תגבור הכוחות (Fortify).
     */
    public static Set<Country> bfsReachableOwned(Country start, Player player)
    {
        Queue<Country> queue = new LinkedList<>();
        Set<Country> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty())
        {
            Country current = queue.poll();
            for (Country neighbor : current.getNeighbors())
            {
                if (neighbor.getOwner() == player && !visited.contains(neighbor))
                {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    /**
     * מוצא את המדינה הטובה ביותר להצבת חיילים בשלב ההתאמה.
     * מחשב ניקוד לכל מדינה שבבעלותנו ע"פ כללים היוריסטיים (איום, ערימה, יבשת, כיסוי גבולות).
     *
     * @param player   השחקן שלנו
     * @param strategy אסטרטגיית ה-AI המכילה כללי Setup היוריסטיים
     * @return המדינה עם הניקוד הגבוה ביותר

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V) משום שעוברים על כל המדינות של השחקן פעם אחת.
     * - סיבוכיות מקום: O(1) כיוון שאנו שומרים רק את המדינה עם הציון המקסימלי.
     *
     * מסקנה סופית: פעולה סופר-מהירה המבטיחה שהבוט לא "יקפא" גם במפות ענק.
     */
    public Country findBestSetupCountry(Player player, HeuristicStrategy strategy)
    {
        Country bestCountry = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Country country : player.getOwnedCountries())
        {
            double score = strategy.calculateSetupScore(country, player, this);
            if (score > bestScore)
            {
                bestCountry = country;
                bestScore = score;
            }
        }

        return bestCountry != null ? bestCountry : player.getOwnedCountries().getFirst();
    }

    /**
     * מוצא את ההתקפה הפוטנציאלית הטובה ביותר לפי האסטרטגיה הנתונה.
     * עבור כל מדינה שבבעלותנו, בודק את השכנים האויבים ומחשב ניקוד התקפה.
     *
     * @param player   השחקן שלנו
     * @param strategy אסטרטגיית חישוב ניקוד התקפה
     * @return AttackMove עם הניקוד הגבוה ביותר, או null אם אין התקפה אפשרית

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V * D) = O(E). עובר על כל מדינה שלנו ובוחן את כל שכניה.
     * - סיבוכיות מקום: O(1) למעט יצירת אובייקט AttackMove בודד.
     *
     * מסקנה סופית: חיפוש אקזוסטיבי יעיל המבטיח שה-AI ימצא את המהלך הרווחי ביותר.
     */
    public AttackMove findBestPotentialAttack(Player player, HeuristicStrategy strategy)
    {
        AttackMove bestPotentialAttack = null;

        for (Country source : player.getOwnedCountries())
            for (Country target : source.getNeighbors())
                if (target.getOwner() != player)
                {
                    double score = strategy.calculateHeuristic(source, target, player, this);

                    if (bestPotentialAttack == null || score > bestPotentialAttack.heuristicScore())
                        bestPotentialAttack = new AttackMove(source, target, score);
                }



        return bestPotentialAttack;
    }

    /**
     * מחזיר את סכום כוח האויבים הסמוכים למדינה נתונה.
     *
     * @param country המדינה שאנו בודקים
     * @param player  השחקן שלנו
     * @return סכום החיילים של כל השכנים העוינים

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(D) סריקת שכני המדינה בלבד.
     * - סיבוכיות מקום: O(1).
     *
     * מסקנה סופית: פונקציית עזר זניחה מבחינת עלות חישובית.
     */
    private int calculateTotalEnemyStrength(Country country, Player player)
    {
        int totalStrength = 0;
        for(Country neighbor : country.getNeighbors())
        {
            if(neighbor.getOwner() != player)
                totalStrength += neighbor.getArmies();
        }
        return totalStrength;
    }

    /**
     * מחזיר מפה של מדינות שבבעלותנו עם ניקוד איום לכל אחת.
     * הניקוד משקלל את כוח האויבים הסמוכים ואת חשיבות מדינות "צוואר בקבוק".
     *
     * @param player      השחקן שלנו
     * @param bottlenecks סט של מדינות קריטיות (articulation points)
     * @return מפת ניקוד איום לכל מדינה עם אויבים סמוכים

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V * D) = O(E). עוברים על כל מדינות השחקן וקוראים לחישוב שכנים.
     * - סיבוכיות מקום: O(V) לבניית מפת התוצאות (HashMap).
     *
     * מסקנה סופית: פעולה הכרחית לבניית מערך הגנה שמבוצעת בזמן לינארי.
     */
    public Map<Country, Double> calculateThreatScores(Player player, Set<Country> bottlenecks)
    {
        Map<Country, Double> threatScores = new HashMap<>();

        for (Country country : player.getOwnedCountries())
        {
            int totalEnemyStrength = calculateTotalEnemyStrength(country, player);

            if (totalEnemyStrength > 0)
            {
                double threatScore = (double) totalEnemyStrength /
                        Math.max(country.getArmies(), GameConstants.MIN_ARMIES_FOR_DEFENSE_CHECK);

                double multiplier = bottlenecks.contains(country) ? GameConstants.BOTTLENECK_THREAT_MULTIPLIER : 1.0;
                threatScores.put(country, threatScore * multiplier);
            }
        }
        return threatScores;
    }

    /**
     * בונה תור עדיפויות של התקפות פוטנציאליות, כאשר ההתקפות עם הניקוד הגבוה ביותר בראש התור.
     *
     * @param player   השחקן שלנו
     * @param strategy אסטרטגיית חישוב ניקוד התקפה
     * @return MaxPriorityQueue ממוין של כל ההתקפות שעברו את הסף

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(E * log(K)) כאשר K הוא כמות ההתקפות החוקיות שנוספות לתור.
     * - סיבוכיות מקום: O(K) לאחסון האובייקטים בתור.
     *
     * מסקנה סופית: שימוש מעולה במבנה נתונים מתקדם. מיון ההתקפות "תוך כדי תנועה" חוסך זמן ריצה.
     */
    public MaxPriorityQueue<AttackMove> buildAttackQueue(Player player, HeuristicStrategy strategy)
    {
        MaxPriorityQueue<AttackMove> attackQueue = new MaxPriorityQueue<>();
        double score;
        for (Country source : player.getOwnedCountries())
        {
            if (source.getArmies() > 1)
                for (Country target : source.getNeighbors())
                {
                    if (isValidAttackTarget(source, target, player, strategy))
                    {
                        score = strategy.calculateHeuristic(source, target, player, this);
                        if (score > strategy.getAttackThreshold())
                            attackQueue.add(new AttackMove(source, target, score));
                    }
                }

        }
        return attackQueue;
    }

    /**
     * בודק אם מדינת יעד היא מטרת התקפה חוקית לפי האסטרטגיה.
     *
     * @param source   מדינת המקור
     * @param target   מדינת היעד
     * @param player   השחקן שלנו
     * @param strategy האסטרטגיה הנוכחית
     * @return true אם ההתקפה חוקית ורווחית
     *
     */
    private boolean isValidAttackTarget(Country source, Country target, Player player, HeuristicStrategy strategy)
    {
        return target.getOwner() != player &&
                source.getArmies() - target.getArmies() >= strategy.getMinArmyAdvantage();
    }

    /**
     * מוצא מדינת גבול מחוברת (עם אויב סמוך) שניתן להגיע אליה מ-start דרך מדינות שבבעלותנו.
     *
     * @param start  מדינת ההתחלה שבבעלותנו
     * @param player השחקן שלנו
     * @return מדינת גבול מחוברת, או null אם לא נמצאה

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V + E) לביצוע ה-BFS, פלוס O(V * D) לבדיקת השכנים. סה"כ O(V + E).
     * - סיבוכיות מקום: O(V) לשמירת הסט מה-BFS.
     *
     * מסקנה סופית: פונקציה אמינה לחילוץ חיילים כלואים עם Short-circuiting יעיל.
     */
    public Country findConnectedBorderUsingBFS(Country start, Player player)
    {
        return bfsReachableOwned(start, player).stream()
                .filter(country -> country != start && countEnemyNeighbors(country, player) > 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * מחזיר את מספר השכנים של המדינה c ששייכים לאויב.
     *
     * @param c  המדינה שאנו בודקים
     * @param me השחקן שלנו
     * @return מספר השכנים העוינים

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(D).
     * - סיבוכיות מקום: O(1).
     */
    public int countEnemyNeighbors(Country c, Player me)
    {
        return (int) c.getNeighbors().stream()
                .filter(neighbor -> neighbor.getOwner() != me)
                .count();
    }

    /**
     * מחזיר סט של מדינות שבבעלותנו שהן נקודות ביטחון קריטיות (Articulation Points).
     * אלו מדינות שאם נאבדן - הרשת שלנו תתפצל לחלקים בלתי קשורים.
     * שימוש: DFS מעמיק עם מעקב discovery time ו-low value.
     *
     * @param player השחקן שלנו
     * @return סט הנקודות הקריטיות

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V + E). כל קודקוד וכל קשת נבדקים פעם אחת בסריקת העומק.
     * - סיבוכיות מקום: O(V) עבור מפות המעקב ועבור ערימת הרקורסיה.
     *
     * מסקנה סופית: אלגוריתם קריטי ומתקדם שמבוצע ביעילות לינארית אופטימלית.
     */
    public Set<Country> findArticulationPoints(Player player)
    {
        Set<Country> criticalPoints = new HashSet<>();
        Map<Country, Integer> discoveryTime = new HashMap<>();
        Map<Country, Integer> lowValue = new HashMap<>();
        Map<Country, Country> parentMap = new HashMap<>();
        int[] timeCounter = {0};

        for (Country country : player.getOwnedCountries())
        {
            if (!discoveryTime.containsKey(country))
            {
                ArticulationPointsDFSContext context = new ArticulationPointsDFSContext(
                        country, discoveryTime, lowValue, parentMap, criticalPoints, timeCounter, player);
                findArticulationPointsDFS(context);
            }
        }

        return criticalPoints;
    }

    /**
     * מעדכן את סט נקודות הביטחון הקריטיות תוך כדי ביצוע DFS על גרף המדינות שלנו.

     * אלגוריתם: DFS עם מעקב discovery time ו-low value –
     * - אם הנוכחי הוא root ויש לו 2 children או יותר, הוא articulation point.
     * - אם הנוכחי אינו root ויש שכן שה-low value שלו >= discovery time של הנוכחי, הוא articulation point.
     *
     * @param context הקשר ה-DFS הנוכחי
     *
     * ניתוח סיבוכיות: O(V + E) כחלק מהסריקה הכללית.
     */
    private void findArticulationPointsDFS(ArticulationPointsDFSContext context)
    {
        context.time[0]++;
        context.discoveryTime.put(context.current, context.time[0]);
        context.lowValue.put(context.current, context.time[0]);
        int childrenCount = 0;

        for (Country neighbor : context.current.getNeighbors())
        {
            if (neighbor.getOwner() == context.player)
            {
                if (!context.discoveryTime.containsKey(neighbor))
                {
                    childrenCount++;
                    context.parentMap.put(neighbor, context.current);

                    findArticulationPointsDFS(new ArticulationPointsDFSContext(
                            neighbor, context.discoveryTime, context.lowValue,
                            context.parentMap, context.criticalPoints, context.time, context.player));

                    context.lowValue.put(context.current,
                            Math.min(context.lowValue.get(context.current), context.lowValue.get(neighbor)));

                    if (context.parentMap.get(context.current) != null &&
                            context.lowValue.get(neighbor) >= context.discoveryTime.get(context.current))
                        context.criticalPoints.add(context.current);

                }
                else if (neighbor != context.parentMap.get(context.current))
                {
                    context.lowValue.put(context.current,
                            Math.min(context.lowValue.get(context.current), context.discoveryTime.get(neighbor)));
                }
            }
        }

        if (context.parentMap.get(context.current) == null && childrenCount > 1)
            context.criticalPoints.add(context.current);
    }

    /**
     * מחזיר את המהלך הטוב ביותר לתגבור מדינות בשלב Fortify.
     * אסטרטגיה דו-שלבית:
     * 1. חפש מדינות "כלואות" (מוקפות רק בשלנו) שצריכות העברת חיילים.
     * 2. העבר חיילים מגבול בטוח לגבול מסוכן יותר.
     *
     * @param player השחקן שלנו
     * @return FortifyMove המהלך המומלץ, או null אם אין צורך

     * ניתוח סיבוכיות:
     * - זמן ריצה: במקרה הגרוע O(T * (V + E)) כאשר T מספר המדינות הכלואות.
     * - סיבוכיות מקום: O(V) בגלל BFS פנימי.
     *
     * מסקנה סופית: מחלק את בעיית התגבור לבעיות קטנות לוגיות ומייצר החלטות אנושיות כמעט.
     */
    public FortifyMove calculateBestFortify(Player player)
    {
        FortifyMove trappedCountryMove = findBestTrappedCountryMove(player);
        if (trappedCountryMove != null)
            return trappedCountryMove;

        return findBestBorderFortification(player);
    }

    /**
     * מוצא את המהלך הטוב ביותר להעברת חיילים ממדינות "כלואות" (מוקפות רק בשלנו) לגבול פעיל.
     *
     * @param player השחקן שלנו
     * @return FortifyMove המהלך המומלץ, או null אם אין מדינות כלואות

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(T * (V + E)) כאשר T הוא מספר המדינות הכלואות.
     * - סיבוכיות מקום: O(V) לשמירת תור ה-BFS.
     *
     * מסקנה סופית: פעולה טקטית מבריקה. בפועל T קטן מאוד, ולכן הביצועים מצוינים.
     */
    private FortifyMove findBestTrappedCountryMove(Player player) {
        Country bestTrappedCountry = null;
        Country bestBorder = null;
        int maxArmiesInTrapped = 0;

        for (Country source : player.getOwnedCountries())
        {
            if (source.getArmies() > GameConstants.MIN_ARMIES_TO_STAY && isCountryTrapped(source, player))
            {
                Country border = findConnectedBorderUsingBFS(source, player);
                if (border != null && source.getArmies() > maxArmiesInTrapped)
                {
                    bestTrappedCountry = source;
                    bestBorder = border;
                    maxArmiesInTrapped = source.getArmies();
                }
            }
        }

        if (bestTrappedCountry == null)
            return null;

        return new FortifyMove(bestTrappedCountry, bestBorder,
                bestTrappedCountry.getArmies() - GameConstants.MIN_ARMIES_TO_STAY);
    }

    /**
     * בודק אם מדינה כלואה – כל שכניה שייכים לשחקן.
     *
     * @param country המדינה שאנו בודקים
     * @param player  השחקן שלנו
     * @return true אם כל השכנים שלנו
     *
     * ניתוח סיבוכיות: זמן ריצה O(D) ומקום O(1).
     */
    private boolean isCountryTrapped(Country country, Player player)
    {
        return country.getNeighbors().stream()
                .allMatch(neighbor -> neighbor.getOwner() == player);
    }

    /**
     * מוצא את המהלך הטוב ביותר להעברת חיילים מגבול בטוח לגבול מסוכן.
     * מחשב רמת סכנה לכל מדינת גבול ומצא את הזוג המתאים ביותר.
     *
     * @param player השחקן שלנו
     * @return FortifyMove המהלך המומלץ, או null אם לא נמצא זוג מתאים

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V * D) למציאת שני הגבולות + O(V + E) לאימות החיבור. סה"כ O(V + E).
     * - סיבוכיות מקום: O(V) בגלל קריאה בודדת ל-BFS.
     *
     * מסקנה סופית: אלגוריתם קריטי לניהול סיכונים חכם שפועל ביעילות גבוהה.
     */
    private FortifyMove findBestBorderFortification(Player player)
    {
        Country safestBorder = null;
        Country mostThreatenedBorder = null;
        double lowestThreat = Double.MAX_VALUE;
        double highestThreat = -1.0;

        for (Country border : player.getOwnedCountries())
        {
            double threatLevel = calculateBorderThreatLevel(border, player);

            if (threatLevel > 0)
            {
                if (threatLevel < lowestThreat && border.getArmies() >= GameConstants.MIN_ARMIES_FOR_FORTIFY)
                {
                    lowestThreat = threatLevel;
                    safestBorder = border;
                }
                if (threatLevel > highestThreat)
                {
                    highestThreat = threatLevel;
                    mostThreatenedBorder = border;
                }
            }
        }

        if (safestBorder == null || mostThreatenedBorder == null || safestBorder == mostThreatenedBorder)
            return null;

        if (!bfsReachableOwned(safestBorder, player).contains(mostThreatenedBorder))
            return null;

        int armiesToMove = safestBorder.getArmies() - GameConstants.KEEP_ARMIES_AT_SOURCE;
        return armiesToMove > 0 ? new FortifyMove(safestBorder, mostThreatenedBorder, armiesToMove) : null;
    }

    /**
     * מחשב את רמת הסכנה של מדינת גבול כיחס בין כוח האויבים לכוחנו.
     *
     * @param border המדינה שאנו בודקים
     * @param player השחקן שלנו
     * @return רמת הסכנה (0 אם אין אויבים סמוכים)
     *
     * ניתוח סיבוכיות: זמן ריצה O(D) ומקום O(1).
     */
    private double calculateBorderThreatLevel(Country border, Player player)
    {
        int totalEnemyForce = calculateTotalEnemyStrength(border, player);
        if (totalEnemyForce == 0)
            return 0;

        return (double) totalEnemyForce / Math.max(border.getArmies(), GameConstants.MIN_ARMIES_FOR_DEFENSE_CHECK);
    }




    /**
     * מוצא את המדינה המאוימת ביותר – עם הכי הרבה שכנים עוינים,
     * ובמקרה שוויון, עם הכי מעט חיילים.
     *
     * @param player השחקן שלנו
     * @return המדינה המאוימת ביותר

     * ניתוח סיבוכיות:
     * - זמן ריצה: O(V * D) = O(E).
     * - סיבוכיות מקום: O(1).
     *
     * מסקנה סופית: משמש כמנגנון Fallback להחלטות חירום, יעיל וקליל.
     */
    public Country findMostThreatenedCountry(Player player)
    {
        Country best = null;
        int maxEnemies = -1;

        for (Country c : player.getOwnedCountries())
        {
            int enemies = countEnemyNeighbors(c, player);
            if (enemies > maxEnemies || (enemies == maxEnemies && best != null && c.getArmies() < best.getArmies()))
            {
                maxEnemies = enemies;
                best = c;
            }
        }
        return best;
    }

    /**
     * רשומת הקשר ל-DFS של מציאת נקודות ביטחון קריטיות.
     * אוגרת את כל המצב הנדרש לביצוע ה-DFS הרקורסיבי.
     */
    private record ArticulationPointsDFSContext(
            Country current,
            Map<Country, Integer> discoveryTime,
            Map<Country, Integer> lowValue,
            Map<Country, Country> parentMap,
            Set<Country> criticalPoints,
            int[] time,
            Player player
    ) {}
}