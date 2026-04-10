package com.example.demo.model.AIAgent;

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
 */
public class AIGraphAnalyzer {


    /**
     * @param player - השחקן שלנו
     * @param stackingWeight - משקל לחיילים קיימים (ככל שגבוה יותר, כך נעדיף מדינות עם חיילים כבר שם)
     *טענת יציאה: מוצא את המדינה הטובה ביותר להצבת חיילים בשלב ההתאמה
     * אלגוריתם: מחפש מדינה עם הכי הרבה אויבים סמוכים ו/או כבר חיילים שם
     * הניקוד = (כוח אויבים) + (חיילים קיימים × משקל הערימה)
     */
    public Country findBestSetupCountry(Player player, double stackingWeight)
    {
        Country bestCountry = null;
        double bestScore = -1;
        
        // מעבר על כל המדינות שבבעלותנו
        for(Country country : player.getOwnedCountries())
        {
            // חישוב כוח אויבים סמוכים
            int totalEnemyStrength = calculateTotalEnemyStrength(country, player);
            
            if (totalEnemyStrength > 0)
            {
                // נוסחת ניקוד: אויבים + חיילים קיימים (בעיתור משקל)
                double score = totalEnemyStrength + (country.getArmies() * stackingWeight);
                if (score > bestScore)
                {
                    bestCountry = country;
                    bestScore = score;
                }
            }
        }
        
        // חזור על אולטימטום הראשון אם לא נמצא אפשרות טובה
        return bestCountry != null ? bestCountry : player.getOwnedCountries().getFirst();
    }


    /**
     * @param player - השחקן שלנו
     * @param strategy - אסטרטגיית חישוב ניקוד התקפה (יכול להיות שונה לפי סוג הבוט)
     * טענת יציאה: מוצא את ההתקפה הפוטנציאלית הטובה ביותר לפי האסטרטגיה הנתונה
     * אלגוריתם: עבור כל מדינה שבבעלותנו, בדוק את השכנים שלה. אם השכן שייך לאויב, חשב את ניקוד ההתקפה לפי האסטרטגיה. שמור את ההתקפה עם הניקוד הגבוה ביותר.
     * */
    public AttackMove findBestPotentialAttack(Player player, HeuristicStrategy strategy) {
        AttackMove bestPotentialAttack = null;
        for (Country source : player.getOwnedCountries())
        {
            for (Country target : source.getNeighbors())
            {
                if (target.getOwner() != player)
                {
                    double score = strategy.calculateHeuristic(source, target, player, this);
                    if (bestPotentialAttack == null || score > bestPotentialAttack.heuristicScore())
                        bestPotentialAttack = new AttackMove(source, target, score);

                }
            }
        }
        return bestPotentialAttack;
    }

    /**
     * @param player - השחקן שלנו
     * @param country - המדינה שאנו רוצים לחשב את האיום שלה
     * טענת יציאה: מחזיר את סכום כוח האויבים הסמוכים למדינה נתונה
     * אלגוריתם: עבור כל שכני המדינה, אם השכן שייך לאויב, הוסף את כמות החיילים שלו לסכום הכולל. החזר את הסכום בסוף.
     * עוזר: חישוב כוח אויבים סמוכים למדינה
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
     @param player - השחקן שלנו
     @param bottlenecks - סט של מדינות "צוואר בקבוק" קריטיות (articulation points)
     טענת יציאה: מחזיר מפה של מדינות שבבעלותנו עם ניקוד איום לכל אחת, כאשר הניקוד משקלל את כוח האויבים הסמוכים ואת חשיבות המדינות הקריטיות
     אלגוריתם: עבור כל מדינה שבבעלותנו, חשב את סכום כוח האויבים הסמוכים. אם יש אויבים, חשב ניקוד איום בסיסי (כוח אויבים חלקי כוחנו),
     והגבר את הניקוד אם זו מדינה "צוואר בקבוק" קריטית. החזר מפה עם הניקוד לכל מדינה.
     */
    public Map<Country, Double> calculateThreatScores(Player player, Set<Country> bottlenecks)
    {
        Map<Country, Double> threatScores = new HashMap<>();
        
        for (Country country : player.getOwnedCountries())
        {
            // סכום כוח כל אויבי סמוכים
            int totalEnemyStrength = calculateTotalEnemyStrength(country, player);
            
            if (totalEnemyStrength > 0)
            {
                // ניקוד בסיס: כוח אויבים / כוחנו
                double threatScore = (double) totalEnemyStrength / Math.max(country.getArmies(), GameConstants.MIN_ARMIES_FOR_DEFENSE_CHECK);
                
                // הגדלת חשיבות אם זה "צוואר בקבוק" קריטי (articulation point)
                if (bottlenecks.contains(country))
                    threatScore *= GameConstants.BOTTLENECK_THREAT_MULTIPLIER;

                
                threatScores.put(country, threatScore);
            }
        }
        return threatScores;
    }

    /**
     * @param player - השחקן שלנו
     * @param strategy - אסטרטגיית חישוב ניקוד התקפה (יכול להיות שונה לפי סוג הבוט)
     * טענת יציאה: בונה תור עדיפויות של התקפות פוטנציאליות, כאשר ההתקפות עם הניקוד הגבוה ביותר נמצאות בראש התור
     * אלגוריתם: עבור כל מדינה שבבעלותנו, בדוק את השכנים שלה. אם השכן שייך לאויב, חשב את ניקוד ההתקפה לפי האסטרטגיה.
     *                 אם הניקוד גבוה מהסף שהוגדר באסטרטגיה, הוסף את ההתקפה לתור העדיפויות. בסוף, החזר את התור עם כל ההתקפות הממוינות לפי ניקוד.
     * */
     public MaxPriorityQueue<AttackMove> buildAttackQueue(Player player, HeuristicStrategy strategy)
     {
         MaxPriorityQueue<AttackMove> queue = new MaxPriorityQueue<>();

         for (Country source : player.getOwnedCountries())
         {
             boolean hasEnoughArmies = source.getArmies() > 1;
             if (!hasEnoughArmies) continue;

             for (Country target : source.getNeighbors())
             {
                 boolean isEnemyTerritory = target.getOwner() != player;
                 if (!isEnemyTerritory) continue;

                 boolean hasMinAdvantage = source.getArmies() - target.getArmies() >= strategy.getMinArmyAdvantage();
                 if (!hasMinAdvantage) continue;

                 double score = strategy.calculateHeuristic(source, target, player, this);

                 if (score > strategy.getAttackThreshold())
                     queue.add(new AttackMove(source, target, score));
             }
         }
         return queue;
     }

    /**
     * @param start - מדינה התחלה שבבעלותנו
     * @param player - השחקן שלנו
     * טענת יציאה: מוצא מדינה גבול מחוברת (עם אויב סמוך) שניתן להגיע אליה מ-start דרך מדינות שבבעלותנו בלבד
     * */
    public Country findConnectedBorderUsingBFS(Country start, Player player) {
        Queue<Country> queue = new LinkedList<>();
        Set<Country> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Country current = queue.poll();

            if (current != start && countEnemyNeighbors(current, player) > 0)
                return current;

            for (Country neighbor : current.getNeighbors()) {
                if (neighbor.getOwner() == player && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }

    /**
     * @param c - המדינה שאנו רוצים לבדוק
     * @param me - השחקן שלנו
     * טענת יציאה: מחזיר את מספר השכנים של המדינה c ששייכים לאויב (לא לנו)
     * */
    public int countEnemyNeighbors(Country c, Player me)
    {
        int count = 0;
        for (Country neighbor : c.getNeighbors())
            if (neighbor.getOwner() != me) count++;

        return count;
    }

    /**
     * @param player - השחקן שלנו
     * טענת יציאה: מחזיר סט של מדינות שבבעלותנו שהן נקודות ביטחון קריטיות (articulation points) בגרף המדינות שלנו
     * מוצא נקודות ביטחון קריטיות בגרף המדינות שלנו (Articulation Points)
     * אלו מדינות שאם נאבדן - הרשת שלנו תתפצל לחלקים בלתי קשורים
     * שימוש: DFS מעמיק עם מעקב discover time ו-low value
     * זה עוזר להחלטות הגנה/התקפה - מדינות אלו קריטיות!
     */
    public Set<Country> findArticulationPoints(Player player)
    {
        ArticulationPointsDFSContext context;
        Set<Country> criticalPoints = new HashSet<>();
        Map<Country, Integer> discoveryTime = new HashMap<>();
        Map<Country, Integer> lowValue = new HashMap<>();
        Map<Country, Country> parentMap = new HashMap<>();
        int[] timeCounter = {0};


        // בדוק מכל מדינה שעדיין לא ביקרנו בה
        for (Country country : player.getOwnedCountries())
        {

            if (!discoveryTime.containsKey(country))
            {
                context = new ArticulationPointsDFSContext(country, discoveryTime, lowValue, parentMap, criticalPoints, timeCounter, player);
                findArticulationPointsDFS(context);
            }

        }

        return criticalPoints;
    }

    /**
    @param context - הקשר של ה-DFS הנוכחי, כולל המדינה הנוכחית, מפת discovery time, מפת low value, מפת הורים, סט נקודות קריטיות, מונה זמן, והשחקן שלנו
    טענת יציאה: מעדכן את הסט של נקודות ביטחון קריטיות (articulation points) תוך כדי ביצוע DFS על הגרף של המדינות שלנו
    אלגוריתם: DFS רגיל עם מעקב discovery time ו-low value
    - עבור כל שכני המדינה הנוכחית:
        - אם השכן שייך לנו ולא ביקרנו בו עדיין:
            - בצע DFS רקורסיבי על השכן
            - עדכן את ה-low value של הנוכחי לפי ה-low value של השכן
            - בדוק אם הנוכחי הוא articulation point לפי התנאים:
                - אם הנוכחי הוא root (אין לו הורה) ויש לו 2 children או יותר, הוא articulation point
                - אם הנוכחי לא root ויש neighbor שה-low value שלו גדול או שווה ל-discovery time של הנוכחי, אז הנוכחי הוא articulation point
        - אם השכן שייך לנו וכבר ביקרנו בו (ולא דרך ההורה), עדכן את ה-low value של הנוכחי לפי ה-discovery time של השכן
     */
    private void findArticulationPointsDFS(ArticulationPointsDFSContext context) {
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

                    ArticulationPointsDFSContext neighborContext = new ArticulationPointsDFSContext(
                            neighbor, context.discoveryTime, context.lowValue, context.parentMap, context.criticalPoints, context.time, context.player);
                    // קריאה רקורסיבית
                    findArticulationPointsDFS(neighborContext);

                    // עדכן את ה-low value של הנוכחי
                    context.lowValue.put(context.current, Math.min(context.lowValue.get(context.current), context.lowValue.get(neighbor)));

                    // בדוק אם הנוכחי הוא articulation point
                    // תנאי: ההורה לא קיים (root) וה-neighbor לא יכול לחזור לאבא דרך דרך אחרת
                    if (context.parentMap.get(context.current) != null && context.lowValue.get(neighbor) >= context.discoveryTime.get(context.current))
                        context.criticalPoints.add(context.current);

                }
                // אם כבר ביקרנו - עדכן low value (אבל לא דרך ההורה!)
                else if (neighbor != context.parentMap.get(context.current))
                    context.lowValue.put(context.current, Math.min(context.lowValue.get(context.current), context.discoveryTime.get(neighbor)));

            }
        }

        // ה-root הוא articulation point אם יש לו 2 children או יותר
        if (context.parentMap.get(context.current) == null && childrenCount > 1)
            context.criticalPoints.add(context.current);
    }


    /**
     * @param player - השחקן שלנו
     * טענת יציאה: מחזיר את המהלך הטוב ביותר להחזקת מדינות בשלב התגבור, לפי אסטרטגיה דו-שלבית
     * חישוב המהלך הטוב ביותר להחזקת מדינות בשלב התגבור
     * אסטרטגיה דו-שלבית:
     * 1. תחילה - חפש מדינות "תופסות" (מוקפות רק בשלנו) שצריכות העברת חיילים
     * 2. ואז - העבר מחוזקים לנקודות מסוכנות יותר
     */
    public FortifyMove calculateBestFortify(Player player)
    {
        // שלב ראשון: מצא מדינות תופסות שצריכות עזרה
        FortifyMove trappedCountryMove = findBestTrappedCountryMove(player);
        if (trappedCountryMove != null)
            return trappedCountryMove;


        // שלב שני: אם אין תופסות - העבר מחוזק לחלש
        return findBestBorderFortification(player);
    }

    /**
      @param player - השחקן שלנו
      טענת יציאה: מוצא את המהלך הטוב ביותר להחזקת מדינות "תופסות" (מוקפות רק בשלנו) שצריכות העברת חיילים, לפי מספר החיילים שיש במדינה התופסת
      אלגוריתם: עבור כל מדינה שבבעלותנו, בדוק אם היא "תופס" (כל השכנים שלה שייכים לנו).
      אם כן, בדוק כמה חיילים יש שם. שמור את המדינה התופסת עם הכי הרבה חיילים (כי היא הכי קריטית) וודא שיש דרך מחוברת ממנה למדינה גבול עם אויב סמוך.
      אם מצאת מדינה כזו, בנה את המהלך להעברת חיילים מהתופסת לגבול המחובר שלה.
      אם לא מצאת אף מדינה תופסת שצריכה חיזוק, החזר null כדי לעבור לשלב השני של האסטרטגיה.
     */
     private FortifyMove findBestTrappedCountryMove(Player player)
     {
         Country bestTrappedCountry = null;
         int maxArmiesInTrapped = 0;

         for (Country source : player.getOwnedCountries())
         {
             boolean hasEnoughArmies = source.getArmies() > GameConstants.MIN_ARMIES_TO_STAY;
             if (!hasEnoughArmies) continue;

             // בדוק אם זה "תפוס" - כל הסמוכים שלי
             if (isCountryTrapped(source, player))
             {
                 if (source.getArmies() > maxArmiesInTrapped)
                 {
                     Country border = findConnectedBorderUsingBFS(source, player);
                     if (border != null)
                     {
                         bestTrappedCountry = source;
                         maxArmiesInTrapped = source.getArmies();
                     }
                 }
             }
         }

         if (bestTrappedCountry != null)
         {
             Country border = findConnectedBorderUsingBFS(bestTrappedCountry, player);
             return new FortifyMove(bestTrappedCountry, border, bestTrappedCountry.getArmies() - GameConstants.MIN_ARMIES_TO_STAY);
         }

         return null;
     }

    /**
     * @param player - השחקן שלנו
     * @param country - המדינה שאנו רוצים לבדוק
     * טענת יציאה: מחזיר true אם המדינה נתונה היא "תפוסה" (כל השכנים שלה שייכים לנו), אחרת מחזיר false
     * אלגוריתם: עבור כל שכני המדינה, בדוק אם יש שכנים ששייכים לאויב. אם כן, המדינה לא תפוסה. אם כל השכנים שייכים לנו, המדינה היא תפוסה.
     * עוזר: בדוק אם מדינה תפוסה (כל הסמוכים שלה הם שלנו)
     */
    private boolean isCountryTrapped(Country country, Player player)
    {
        for (Country neighbor : country.getNeighbors())
        {
            if (neighbor.getOwner() != player)
                return false; // יש אויב סמוך, לא תפוס
        }
        return true; // כל הסמוכים שלנו
    }

    /**
     * @param player - השחקן שלנו
     * טענת יציאה: מוצא את המהלך הטוב ביותר להעברת חיילים ממדינת גבול בטוחה למדינת גבול מסוכנת, לפי רמת הסכנה של כל מדינה גבול
     * אלגוריתם: עבור כל מדינות הגבול שבבעלותנו, חשב את רמת הסכנה שלהן (כוח האויבים הסמוכים חלקי החיילים שיש שם).
     *              מצא את מדינת הגבול עם רמת הסכנה הנמוכה ביותר (הכי בטוחה) ואת מדינת הגבול עם רמת הסכנה הגבוהה ביותר (הכי מסוכנת).
     * אם מצאת זוג כזה, בדוק אם הם מחוברים דרך מדינות שבבעלותנו בלבד (BFS).
     *              אם כן, בנה את המהלך להעברת חיילים מהגבול הבטוח לגבול המסוכן, תוך שמירה על מינימום חיילים בגבול הבטוח.
     * עוזר: מצא את ההצבעה הטובה ביותר בנתיב הגנה
     * העבר מ"חוזק" (סכנה נמוכה) ל"חלוש" (סכנה גבוהה)
     */
    private FortifyMove findBestBorderFortification(Player player)
    {
        Country safestBorder = null;
        Country mostThreatenedBorder = null;
        double lowestThreat = Double.MAX_VALUE;
        double highestThreat = -1.0;

        // מצא את מדינות הגבול + אמוד את הסכנה שלהן
        for (Country border : player.getOwnedCountries())
        {
            double threatLevel = calculateBorderThreatLevel(border, player);

            if (threatLevel > 0)
            {
                // עדכן את הנמוך ביותר
                if (threatLevel < lowestThreat && border.getArmies() >= GameConstants.MIN_ARMIES_FOR_FORTIFY)
                {
                    lowestThreat = threatLevel;
                    safestBorder = border;
                }
                // עדכן את הגבוה ביותר
                if (threatLevel > highestThreat)
                {
                    highestThreat = threatLevel;
                    mostThreatenedBorder = border;
                }
            }
        }

        // אם מצאנו זוג טוב - ובדוק שהם מחוברים
        if (safestBorder != null && mostThreatenedBorder != null && safestBorder != mostThreatenedBorder)
        {
            if (isConnectedBFS(safestBorder, mostThreatenedBorder, player))
            {
                int armiesToMove = safestBorder.getArmies() - GameConstants.KEEP_ARMIES_AT_SOURCE;
                if (armiesToMove > 0)
                    return new FortifyMove(safestBorder, mostThreatenedBorder, armiesToMove);

            }
        }

        return null;
    }

    /**
     * @param player - השחקן שלנו
     * @param border - מדינת גבול שאנו רוצים לחשב את רמת הסכנה שלה
     * טענת יציאה: מחזיר את רמת הסכנה של מדינת הגבול הנתונה, כאשר רמת הסכנה מחושבת כיחס כוח האויבים הסמוכים חלקי כמות החיילים שיש במדינה.
     *              אם אין אויבים סמוכים, מחזיר 0 (לא מסוכן).
     * אלגוריתם: חשב את סכום כוח האויבים הסמוכים למדינת הגבול. אם סכום זה הוא 0, החזר 0 (לא מסוכן).
     *               אחרת, חשב את רמת הסכנה כיחס בין כוח האויבים הסמוכים לבין כמות החיילים שיש במדינה (עם טיפול למקרה של 0 חיילים כדי למנוע חלוקה באפס). החזר את רמת הסכנה המחושבת.
     * עוזר: חישוב רמת הסכנה של מדינה גבול
     */
    private double calculateBorderThreatLevel(Country border, Player player)
    {
        int totalEnemyForce = calculateTotalEnemyStrength(border, player);
        if (totalEnemyForce == 0) return 0;
        
        return (double) totalEnemyForce / Math.max(border.getArmies(), GameConstants.MIN_ARMIES_FOR_DEFENSE_CHECK);
    }
    /**
     * @param player - השחקן שלנו
     * @param start - מדינה התחלה
     * @param target - מדינה יעד
     * טענת יציאה: מחזיר true אם יש מסלול מחובר בין start ל-target דרך מדינות שבבעלותנו בלבד, אחרת מחזיר false
     * */
    private boolean isConnectedBFS(Country start, Country target, Player player)
    {
        Queue<Country> queue = new LinkedList<>();
        Set<Country> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty())
        {
            Country current = queue.poll();
            if (current == target) return true;

            for (Country neighbor : current.getNeighbors())
            {
                if (neighbor.getOwner() == player && !visited.contains(neighbor))
                {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }
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

