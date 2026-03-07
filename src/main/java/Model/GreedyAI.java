package Model;

import java.util.*;

public class GreedyAI implements BotStrategy {

    @Override
    public void executeTurn(Player player, RiskGame game) {
        System.out.println("--- AI Turn Started: " + player.getName() + " ---");

        // שלב 1: DRAFT (הבוט מתחיל במצב DraftState)
        chooseReinforcement(player, game);

        // מעבר למצב AttackState
        game.nextPhase();

        // שלב 2: ATTACK
        chooseAttack(player, game);

        // מעבר למצב FortifyState
        game.nextPhase();

        // שלב 3: FORTIFY
        chooseFortify(player, game);

        // *** התיקון הקריטי לארכיטקטורה החדשה ***
        // מעבר שלב מתוך FortifyState גורם בפועל לסיום התור והעברתו לשחקן הבא!
        game.nextPhase();

        System.out.println("--- AI Turn Ended ---");
    }

    private void chooseReinforcement(Player player, RiskGame game) {
        Country mostThreatened = null;
        int maxEnemies = -1;

        // חיפוש חמדן פשוט - המדינה המאוימת ביותר
        for (Country c : player.getOwnedCountries()) {
            int enemyCount = countEnemyNeighbors(c, player);
            if (enemyCount > maxEnemies) {
                maxEnemies = enemyCount;
                mostThreatened = c;
            }
        }

        if (mostThreatened != null) {
            int startArmies = player.getDraftArmies();
            while (player.getDraftArmies() > 0) {
                game.placeArmy(mostThreatened);
            }
            System.out.println("AI drafted " + startArmies + " armies on: " + mostThreatened.getName());
        }
    }

    private void chooseAttack(Player player, RiskGame game) {
        boolean keepAttacking = true;

        while (keepAttacking) {
            // שימוש בתור עדיפויות כדי לשלוף את ההתקפה עם התוחלת הגבוהה ביותר בזמן O(log E)
            PriorityQueue<AttackMove> attackQueue = new PriorityQueue<>(
                    (m1, m2) -> Double.compare(m2.heuristicScore, m1.heuristicScore) // Max-Heap
            );

            // בניית מרחב המצבים המיידי (State Space) עבור ההתקפות האפשריות
            for (Country myCountry : player.getOwnedCountries()) {
                if (myCountry.getArmies() > 1) {
                    for (Country neighbor : myCountry.getNeighbors()) {
                        if (neighbor.getOwner() != player) {
                            double score = calculateHeuristic(myCountry, neighbor, player);
                            if (score > 0) { // תנאי סף לביצוע התקפה
                                attackQueue.add(new AttackMove(myCountry, neighbor, score));
                            }
                        }
                    }
                }
            }

            if (!attackQueue.isEmpty()) {
                AttackMove bestMove = attackQueue.poll(); // שליפת ההתקפה הטובה ביותר
                System.out.println("AI attacking " + bestMove.target.getName() + " from " + bestMove.source.getName() + " (Score: " + bestMove.heuristicScore + ")");
                String result = game.attack(bestMove.source, bestMove.target);
                System.out.println("AI Attack Result: " + result);
            } else {
                keepAttacking = false; // אין יותר מהלכים רווחיים
            }
        }
    }

    /**
     * פונקציית תועלת המחשבת את הציון ההיוריסטי של ההתקפה.
     * הערה: יש להתאים את המשקולות (Weights) בהתאם לנוסחה במסמך הפרויקט שלך.
     */
    private double calculateHeuristic(Country attacker, Country defender, Player player) {
        double P_win = attacker.getArmies() - defender.getArmies(); // יתרון מספרי בסיסי
        double V_bonus = 0; // ניתן להוסיף לוגיקה לבדיקה האם המדינה משלימה יבשת
        double S_strategic = countEnemyNeighbors(defender, player) * -0.5; // כמה המדינה מאוימת

        double totalScore = (P_win * 1.5) + (V_bonus * 2.0) + (S_strategic * 1.0);

        // מחזיר ציון רק אם יש יתרון סביר ( לפחות 2 חיילים יותר)
        return (P_win >= 2) ? totalScore : -1;
    }

    private void chooseFortify(Player player, RiskGame game) {
        Country safeCountry = null;

        // מציאת עורף - מדינה בטוחה עם עודף חיילים
        for (Country c : player.getOwnedCountries()) {
            if (c.getArmies() > 1 && countEnemyNeighbors(c, player) == 0) {
                safeCountry = c;
                break;
            }
        }

        if (safeCountry != null) {
            // הפעלת אלגוריתם BFS למציאת מדינת חזית המקושרת לעורף בסיבוכיות O(V+E)
            Country borderCountry = findConnectedBorderUsingBFS(safeCountry, player);

            if (borderCountry != null) {
                int armiesToMove = safeCountry.getArmies() - 1;
                String result = game.fortify(safeCountry, borderCountry, armiesToMove);
                System.out.println("AI Fortify: Moved " + armiesToMove + " armies to " + borderCountry.getName());
            }
        }
    }

    /**
     * אלגוריתם סריקת גרף (BFS) לאיתור מדינת חזית המחוברת ברצף מדינות שבשליטת השחקן.
     */
    private Country findConnectedBorderUsingBFS(Country start, Player player) {
        Queue<Country> queue = new LinkedList<>();
        Set<Country> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Country current = queue.poll();

            // אם מצאנו מדינה עם לפחות אויב אחד, זו חזית, ואפשר לתגבר אותה
            if (countEnemyNeighbors(current, player) > 0) {
                return current;
            }

            for (Country neighbor : current.getNeighbors()) {
                // מתקדמים רק דרך מדינות שהן בבעלות השחקן (נתיב חוקי להעברת כוחות)
                if (neighbor.getOwner() == player && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return null; // לא נמצא נתיב לחזית
    }

    // פונקציית עזר קיימת
    private int countEnemyNeighbors(Country c, Player me) {
        int count = 0;
        for (Country neighbor : c.getNeighbors()) {
            if (neighbor.getOwner() != me) {
                count++;
            }
        }
        return count;
    }

    // מחלקת עזר פנימית לייצוג מהלך התקפה בתוך תור העדיפויות
    private class AttackMove {
        Country source;
        Country target;
        double heuristicScore;

        public AttackMove(Country source, Country target, double heuristicScore) {
            this.source = source;
            this.target = target;
            this.heuristicScore = heuristicScore;
        }
    }
}