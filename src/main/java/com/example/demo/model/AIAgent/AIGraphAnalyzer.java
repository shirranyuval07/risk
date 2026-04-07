package com.example.demo.model.AIAgent;

import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;



import com.example.demo.model.Records.GameRecords.FortifyMove;

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
    /**
     * פונקציית מעטפת הבודקת האם מדינה ספציפית מהווה צוואר בקבוק (Articulation Point) עבור הבעלים שלה.
     * הפונקציה משתמשת באלגוריתם Tarjan/DFS הקיים במחלקה.
     */
    public FortifyMove calculateBestFortify(Player player) {
        Country bestSource = null;
        Country bestTarget = null;
        int maxArmiesToMove = 0;

        // --- שלב א': איתור מדינות כלואות לחלוטין בעורף (הקוד המקורי שלך) ---
        for (Country source : player.getOwnedCountries()) {
            if (source.getArmies() <= 1) continue;

            boolean isTrapped = true;
            for (Country neighbor : source.getNeighbors()) {
                if (neighbor.getOwner() != player) {
                    isTrapped = false;
                    break;
                }
            }

            if (isTrapped && source.getArmies() > maxArmiesToMove) {
                Country closestBorder = findConnectedBorderUsingBFS(source, player);
                if (closestBorder != null) {
                    bestSource = source;
                    bestTarget = closestBorder;
                    maxArmiesToMove = source.getArmies();
                }
            }
        }

        if (bestSource != null) {
            return new FortifyMove(bestSource, bestTarget, bestSource.getArmies() - 1);
        }

        // --- שלב ב' : ביצור גבולות. העברה מחזית בטוחה לחזית מסוכנת ---
        Country safestBorder = null;
        Country mostDangerousBorder = null;
        double lowestThreatRatio = Double.MAX_VALUE;
        double highestThreatRatio = -1.0;

        for (Country c : player.getOwnedCountries()) {
            int enemyForces = 0;
            for (Country n : c.getNeighbors()) {
                if (n.getOwner() != player) enemyForces += n.getArmies();
            }

            if (enemyForces > 0) { // זו מדינת חזית (גבול)
                double threatRatio = (double) enemyForces / Math.max(c.getArmies(), 1);

                // מחפשים חזית יציבה עם עודף חיילים כדי לקחת ממנה
                if (threatRatio < lowestThreatRatio && c.getArmies() >= 3) {
                    lowestThreatRatio = threatRatio;
                    safestBorder = c;
                }

                // מחפשים את החזית הכי מאוימת שזקוקה לעזרה
                if (threatRatio > highestThreatRatio) {
                    highestThreatRatio = threatRatio;
                    mostDangerousBorder = c;
                }
            }
        }

        // אם מצאנו חזית בטוחה וחזית מסוכנת, נבדוק עם BFS אם הן מחוברות ברצף
        if (safestBorder != null && mostDangerousBorder != null && safestBorder != mostDangerousBorder) {
            if (isConnectedBFS(safestBorder, mostDangerousBorder, player)) {
                // מעבירים חיילים, אבל משאירים 2 חיילים כמשמר מינימלי על הגבול הבטוח
                int armiesToMove = safestBorder.getArmies() - 2;
                if (armiesToMove > 0) {
                    return new FortifyMove(safestBorder, mostDangerousBorder, armiesToMove);
                }
            }
        }

        return null;
    }

    /**
     * פונקציית עזר: סריקת BFS לבדיקה האם קיים נתיב ידידותי בין שתי מדינות
     */
    private boolean isConnectedBFS(Country start, Country target, Player player) {
        Queue<Country> queue = new LinkedList<>();
        Set<Country> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Country current = queue.poll();
            if (current == target) return true; // הגענו ליעד!

            for (Country neighbor : current.getNeighbors()) {
                if (neighbor.getOwner() == player && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false; // אין מסלול בטוח
    }
}

