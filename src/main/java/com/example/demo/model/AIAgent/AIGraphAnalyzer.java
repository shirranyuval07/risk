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
 * 
 * תפקידיה:
 * - חיפוש מדינות אופטימליות להצבה בשלב Setup
 * - חיפוש הזדמנויות התקפה הטובות ביותר
 * - חישוב ניקוד איום לכל מדינה
 * - בניית תור עדיפויות של התקפות
 * - מציאת נקודות ביטחון קריטיות בגרף (Articulation Points)
 * - תכנון הגנה ותגבור (Fortify) אופטימלי
 * 
 * אלגוריתמים:
 * - BFS (Breadth-First Search): למציאת מדינות מחוברות
 * - DFS (Depth-First Search): למציאת נקודות ביטחון
 * 
 * השימוש: משמשת את בוטים (AI players) להחלטות אסטרטגיות
 */
public class AIGraphAnalyzer {


    /**
     * מוצא את המדינה הטובה ביותר להצבת חיילים בשלב ההתאמה
     * אלגוריתם: מחפש מדינה עם הכי הרבה אויבים סמוכים ו/או כבר חיילים שם
     * הניקוד = (כוח אויבים) + (חיילים קיימים × משקל הערימה)
     */
    public Country findBestSetupCountry(Player player, double stackingWeight) {
        Country bestCountry = null;
        double bestScore = -1;
        
        // יתור על כל המדינות שבבעלותנו
        for(Country country : player.getOwnedCountries()) {
            // חישוב כוח אויבים סמוכים
            int totalEnemyStrength = calculateTotalEnemyStrength(country, player);
            
            if (totalEnemyStrength > 0) {
                // נוסחת ניקוד: אויבים + חיילים קיימים (בעיתור משקל)
                double score = totalEnemyStrength + (country.getArmies() * stackingWeight);
                if (score > bestScore) {
                    bestCountry = country;
                    bestScore = score;
                }
            }
        }
        
        // חזור על אולטימטום הראשון אם לא נמצא אפשרות טובה
        return bestCountry != null ? bestCountry : player.getOwnedCountries().getFirst();
    }

    public AttackMove findBestPotentialAttack(Player player, HeuristicStrategy strategy) {
        AttackMove bestPotentialAttack = null;
        for (Country source : player.getOwnedCountries()) {
            for (Country target : source.getNeighbors()) {
                if (target.getOwner() != player) {
                    double score = strategy.calculateHeuristic(source, target, player, this);
                    if (bestPotentialAttack == null || score > bestPotentialAttack.heuristicScore()) {
                        bestPotentialAttack = new AttackMove(source, target, score);
                    }
                }
            }
        }
        return bestPotentialAttack;
    }

    /**
     * עוזר: חישוב כוח אויבים סמוכים למדינה
     */
    private int calculateTotalEnemyStrength(Country country, Player player) {
        int totalStrength = 0;
        for(Country neighbor : country.getNeighbors()) {
            if(neighbor.getOwner() != player) {
                totalStrength += neighbor.getArmies();
            }
        }
        return totalStrength;
    }

    /**
     * חישוב ניקוד איום לכל מדינה בעלותנו
     * משמש להחלטת היכן להצית חיילים בהגנה

     * ניקוד איום = (כוח אויבים) / (חיילים שלנו)
     * אם המדינה היא "צוואר בקבוק" - הכפל × 2
     */
    public Map<Country, Double> calculateThreatScores(Player player, Set<Country> bottlenecks) {
        Map<Country, Double> threatScores = new HashMap<>();
        
        for (Country country : player.getOwnedCountries()) {
            // סכום כוח כל אויבי סמוכים
            int totalEnemyStrength = calculateTotalEnemyStrength(country, player);
            
            if (totalEnemyStrength > 0) {
                // ניקוד בסיס: כוח אויבים / כוחנו
                double threatScore = (double) totalEnemyStrength / Math.max(country.getArmies(), GameConstants.MIN_ARMIES_FOR_DEFENSE_CHECK);
                
                // הגדלת חשיבות אם זה "צוואר בקבוק" קריטי (articulation point)
                if (bottlenecks.contains(country)) {
                    threatScore *= GameConstants.BOTTLENECK_THREAT_MULTIPLIER;
                }
                
                threatScores.put(country, threatScore);
            }
        }
        return threatScores;
    }

    public MaxPriorityQueue<AttackMove> buildAttackQueue(Player player, HeuristicStrategy strategy) {
        MaxPriorityQueue<AttackMove> queue = new MaxPriorityQueue<>();

        for (Country source : player.getOwnedCountries()) {
            if (source.getArmies() <= 1) continue;

            for (Country target : source.getNeighbors()) {
                if (target.getOwner() == player) continue;

                if (source.getArmies() - target.getArmies() < strategy.getMinArmyAdvantage()) continue;

                double score = strategy.calculateHeuristic(source, target, player, this);

                if (score > strategy.getAttackThreshold())
                    queue.add(new AttackMove(source, target, score));
            }
        }
        return queue;
    }


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

    public int countEnemyNeighbors(Country c, Player me) {
        int count = 0;
        for (Country neighbor : c.getNeighbors()) {
            if (neighbor.getOwner() != me) count++;
        }
        return count;
    }

    /**
     * מוצא נקודות ביטחון קריטיות בגרף המדינות שלנו (Articulation Points)
     * אלו מדינות שאם נאבדן - הרשת שלנו תתפצל לחלקים בלתי קשורים
     * שימוש: DFS מעמיק עם מעקב discover time ו-low value
     * זה עוזר להחלטות הגנה/התקפה - מדינות אלו קריטיות!
     */
    public Set<Country> findArticulationPoints(Player player) {
        Set<Country> criticalPoints = new HashSet<>();
        Map<Country, Integer> discoveryTime = new HashMap<>();
        Map<Country, Integer> lowValue = new HashMap<>();
        Map<Country, Country> parentMap = new HashMap<>();
        int[] timeCounter = {0};

        // בדוק מכל מדינה שעדיין לא ביקרנו בה
        for (Country country : player.getOwnedCountries()) {
            if (!discoveryTime.containsKey(country)) {
                findArticulationPointsDFS(country, discoveryTime, lowValue, parentMap, criticalPoints, timeCounter, player);
            }
        }

        return criticalPoints;
    }

    /**
     * DFS עמוק לחיפוש articulation points
     * מחשב: discovery time ו- low value לכל צומת
     * articulation point = אם הסרתו תחתוך קשר בגרף
     */
    private void findArticulationPointsDFS(Country current, Map<Country, Integer> discoveryTime, Map<Country, Integer> lowValue,
                                          Map<Country, Country> parentMap, Set<Country> criticalPoints, int[] time, Player player) {
        time[0]++;
        discoveryTime.put(current, time[0]);
        lowValue.put(current, time[0]);
        int childrenCount = 0;

        for (Country neighbor : current.getNeighbors()) {
            if (neighbor.getOwner() == player) {
                // אם הכבוד הזה עדיין לא ביקרנו בו
                if (!discoveryTime.containsKey(neighbor)) {
                    childrenCount++;
                    parentMap.put(neighbor, current);
                    
                    // קריאה רקורסיבית
                    findArticulationPointsDFS(neighbor, discoveryTime, lowValue, parentMap, criticalPoints, time, player);
                    
                    // עדכן את ה-low value של הנוכחי
                    lowValue.put(current, Math.min(lowValue.get(current), lowValue.get(neighbor)));

                    // בדוק אם הנוכחי הוא articulation point
                    // תנאי: ההורה לא קיים (root) וה-neighbor לא יכול לחזור לאבא דרך דרך אחרת
                    if (parentMap.get(current) != null && lowValue.get(neighbor) >= discoveryTime.get(current)) {
                        criticalPoints.add(current);
                    }
                } 
                // אם כבר ביקרנו - עדכן low value (אבל לא דרך ההורה!)
                else if (neighbor != parentMap.get(current)) {
                    lowValue.put(current, Math.min(lowValue.get(current), discoveryTime.get(neighbor)));
                }
            }
        }

        // ה-root הוא articulation point אם יש לו 2 children או יותר
        if (parentMap.get(current) == null && childrenCount > 1) {
            criticalPoints.add(current);
        }
    }

    /**
     * חישוב המהלך הטוב ביותר להחזקת מדינות בשלב התגבור
     * אסטרטגיה דו-שלבית:
     * 1. תחילה - חפש מדינות "תופסות" (מוקפות רק בשלנו) שצריכות העברת חיילים
     * 2. ואז - העבר מחוזקים לנקודות מסוכנות יותר
     */
    public FortifyMove calculateBestFortify(Player player) {
        // שלב ראשון: מצא מדינות תופסות שצריכות עזרה
        FortifyMove trappedCountryMove = findBestTrappedCountryMove(player);
        if (trappedCountryMove != null) {
            return trappedCountryMove;
        }

        // שלב שני: אם אין תופסות - העבר מחוזק לחלש
        return findBestBorderFortification(player);
    }

    /**
     * עוזר: מצא מדינות תופסות (מוקפות רק בשלנו)
     * אלו צריכות העברת חיילים כדי לא יהיו בלתי מגוננות
     */
    private FortifyMove findBestTrappedCountryMove(Player player) {
        Country bestTrappedCountry = null;
        int maxArmiesInTrapped = 0;

        for (Country source : player.getOwnedCountries()) {
            if (source.getArmies() <= GameConstants.MIN_ARMIES_TO_STAY) continue;

            // בדוק אם זה "תפוס" - כל הסמוכים שלי
            if (isCountryTrapped(source, player)) {
                if (source.getArmies() > maxArmiesInTrapped) {
                    Country border = findConnectedBorderUsingBFS(source, player);
                    if (border != null) {
                        bestTrappedCountry = source;
                        maxArmiesInTrapped = source.getArmies();
                    }
                }
            }
        }

        if (bestTrappedCountry != null) {
            Country border = findConnectedBorderUsingBFS(bestTrappedCountry, player);
            return new FortifyMove(bestTrappedCountry, border, bestTrappedCountry.getArmies() - GameConstants.MIN_ARMIES_TO_STAY);
        }

        return null;
    }

    /**
     * עוזר: בדוק אם מדינה תפוסה (כל הסמוכים שלה הם שלנו)
     */
    private boolean isCountryTrapped(Country country, Player player) {
        for (Country neighbor : country.getNeighbors()) {
            if (neighbor.getOwner() != player) {
                return false; // יש אויב סמוך, לא תפוס
            }
        }
        return true; // כל הסמוכים שלנו
    }

    /**
     * עוזר: מצא את ההצבעה הטובה ביותר בנתיב הגנה
     * העבר מ"חוזק" (סכנה נמוכה) ל"חלוש" (סכנה גבוהה)
     */
    private FortifyMove findBestBorderFortification(Player player) {
        Country safestBorder = null;
        Country mostThreatenedBorder = null;
        double lowestThreat = Double.MAX_VALUE;
        double highestThreat = -1.0;

        // מצא את מדינות הגבול + אמוד את הסכנה שלהן
        for (Country border : player.getOwnedCountries()) {
            double threatLevel = calculateBorderThreatLevel(border, player);

            if (threatLevel > 0) {
                // עדכן את הנמוך ביותר
                if (threatLevel < lowestThreat && border.getArmies() >= GameConstants.MIN_ARMIES_FOR_FORTIFY) {
                    lowestThreat = threatLevel;
                    safestBorder = border;
                }
                // עדכן את הגבוה ביותר
                if (threatLevel > highestThreat) {
                    highestThreat = threatLevel;
                    mostThreatenedBorder = border;
                }
            }
        }

        // אם מצאנו זוג טוב - ובדוק שהם מחוברים
        if (safestBorder != null && mostThreatenedBorder != null && safestBorder != mostThreatenedBorder) {
            if (isConnectedBFS(safestBorder, mostThreatenedBorder, player)) {
                int armiesToMove = safestBorder.getArmies() - GameConstants.KEEP_ARMIES_AT_SOURCE;
                if (armiesToMove > 0) {
                    return new FortifyMove(safestBorder, mostThreatenedBorder, armiesToMove);
                }
            }
        }

        return null;
    }

    /**
     * עוזר: חישוב רמת הסכנה של מדינה גבול
     */
    private double calculateBorderThreatLevel(Country border, Player player) {
        int totalEnemyForce = calculateTotalEnemyStrength(border, player);
        if (totalEnemyForce == 0) return 0;
        
        return (double) totalEnemyForce / Math.max(border.getArmies(), GameConstants.MIN_ARMIES_FOR_DEFENSE_CHECK);
    }

    private boolean isConnectedBFS(Country start, Country target, Player player) {
        Queue<Country> queue = new LinkedList<>();
        Set<Country> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Country current = queue.poll();
            if (current == target) return true;

            for (Country neighbor : current.getNeighbors()) {
                if (neighbor.getOwner() == player && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }
}