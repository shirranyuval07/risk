package Model.AIAgent;

import Model.Country;
import Model.Player;

import java.util.*;

public class AIGraphAnalyzer
{
    /**
     * BFS על תת-גרף מדינות השחקן בלבד.
     * מחזיר את מדינת החזית (שיש לה לפחות שכן עוין אחד) הקרובה ביותר ל-start.
     */
    public Country findConnectedBorderUsingBFS(Country start, Player player)
    {
        Queue<Country> queue    = new LinkedList<>();
        Set<Country>   visited  = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty())
        {
            Country current = queue.poll();

            // חזית: מדינה בשליטתנו עם לפחות שכן עוין אחד
            if (current != start && countEnemyNeighbors(current, player) > 0)
                return current;

            for (Country neighbor : current.getNeighbors())
            {
                if (neighbor.getOwner() == player && !visited.contains(neighbor))
                {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return null; // לא נמצא נתיב רציף לחזית
    }
    /** סופר את מספר השכנים העוינים של מדינה נתונה. */
    public int countEnemyNeighbors(Country c, Player me)
    {
        int count = 0;
        for (Country neighbor : c.getNeighbors())
        {
            if (neighbor.getOwner() != me) count++;
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  אלגוריתם מציאת נקודות חיתוך (Articulation Points) / "צווארי בקבוק"
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * שלב א': מעטפת האלגוריתם (Tarjan/DFS) לאיתור צווארי בקבוק אסטרטגיים בגרף.
     * סיבוכיות מקום: O(V) עבור מילוני העזר וסט התוצאות.
     */

    private void dfsArticulationPoints(Country current, Map<Country, Integer> discoveryTime, Map<Country, Integer> lowValue,
                                       Map<Country, Country> parent, Set<Country> articulationPoints, int[] time, Player player)
    {
        time[0]++;
        discoveryTime.put(current, time[0]);
        lowValue.put(current, time[0]);
        int children = 0;

        for (Country neighbor : current.getNeighbors()) {
            if (neighbor.getOwner() == player) {

                // מצב 1: קודקוד שטרם ביקרנו בו (Tree Edge)
                if (!discoveryTime.containsKey(neighbor)) {
                    children++;
                    parent.put(neighbor, current); // current הוא ההורה של neighbor

                    // קריאה רקורסיבית
                    dfsArticulationPoints(neighbor, discoveryTime, lowValue, parent, articulationPoints, time, player);

                    // עדכון ערך החלחול בחזרה מהרקורסיה
                    lowValue.put(current, Math.min(lowValue.get(current), lowValue.get(neighbor)));

                    // בדיקת צוואר בקבוק (רק אם אנחנו לא שורש העץ!)
                    if (parent.get(current) != null && lowValue.get(neighbor) >= discoveryTime.get(current))
                        articulationPoints.add(current);
                }
                // מצב 2: קודקוד שכבר ביקרנו בו, והוא לא ההורה שלנו (Back Edge - מצאנו מעגל!)
                else if (neighbor != parent.get(current))
                    lowValue.put(current, Math.min(lowValue.get(current), discoveryTime.get(neighbor)));
            }
        }

        if (parent.get(current) == null && children > 1)
            articulationPoints.add(current);
    }
    public Set<Country> findArticulationPoints(Player player)
    {
        Set<Country> articulationPoints = new HashSet<>();

        // מילונים לשמירת מצב הסריקה של כל קודקוד בגרף בזמן O(1)
        Map<Country, Integer> discoveryTime = new HashMap<>();
        Map<Country, Integer> lowValue = new HashMap<>();
        Map<Country, Country> parent = new HashMap<>();

        // טריק ב-Java להעברת מונה לרקורסיה (By Reference)
        int[] time = {0};

        // סריקת כל המדינות של השחקן - כדי לטפל בגרף עם מספר רכיבי קשירות
        for (Country c : player.getOwnedCountries())
        {
            // אם טרם ביקרנו במדינה זו (אין לה זמן גילוי), נתחיל ממנה סריקת DFS
            if (!discoveryTime.containsKey(c))
                dfsArticulationPoints(c, discoveryTime, lowValue, parent, articulationPoints, time, player);
        }

        return articulationPoints;
    }
}
