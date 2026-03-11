package Model.AIAgent;

import Model.*;
import Model.Records.AttackMove;
import Model.Records.BattleResult;

import java.util.*;


public class GreedyAI implements BotStrategy {
    private final AIGraphAnalyzer graphAnalyzer = new AIGraphAnalyzer();
    private final HeuristicEvaluator evaluator = new HeuristicEvaluator(graphAnalyzer);
    // ─── קבועי משקל לפונקציית ההערכה ──────────────────────────────────────────
    public static final double W_WIN   = 1.5; // חשיבות הסתברות ניצחון בקרב
    public static final double W_BONUS = 2.0; // חשיבות קריטית לשליטה ביבשת
    public static final double W_STRAT = 1.2; // חשיבות ערך אסטרטגי (צוואר בקבוק)
    public static final double W_COST  = 1.0; // חשיבות חיסכון בחיילים

    // סף מינימלי לביצוע התקפה
    private static final double ATTACK_THRESHOLD  = 0.0;
    // יתרון מינימלי בחיילים הנדרש לפני כל תקיפה
    private static final int    MIN_ARMY_ADVANTAGE = 2;

    // ═══════════════════════════════════════════════════════════════════════════
    //  נקודת כניסה ראשית – מנהל את שלושת שלבי התור
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void executeTurn(Player player, RiskGame game)
    {
        System.out.println("--- AI Turn Started: " + player.getName() + " ---");

        chooseReinforcement(player, game); // שלב Draft  – הבוט כבר ב-DraftState
        game.nextPhase();                  // → AttackState

        chooseAttack(player, game);        // שלב Attack
        game.nextPhase();                  // → FortifyState

        chooseFortify(player, game);       // שלב Fortify
        game.nextPhase();                  // → סיום תור, מעבר לשחקן הבא

        System.out.println("--- AI Turn Ended ---");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 1 – DRAFT: גיוס והצבת חיילים
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * חמדן פשוט: מציאת המדינה המאוימת ביותר (הכי הרבה שכנים עוינים)
     * והצבת כל כוחות התגבורת בה.

     * סיבוכיות: O(V·k) כאשר V = מדינות השחקן, k = שכנים ממוצע
     */
    private void chooseReinforcement(Player player, RiskGame game)
    {
        Country mostThreatened = findMostThreatenedCountry(player);

        if (mostThreatened == null) return;

        int startArmies = player.getDraftArmies();
        while (player.getDraftArmies() > 0)
            game.placeArmy(mostThreatened);

        System.out.printf("[AI DRAFT] Placed %d armies on: %s%n",
                startArmies, mostThreatened.getName());
    }

    /**
     * מחזיר את המדינה עם המספר הגבוה ביותר של שכנים עוינים.
     * במקרה של שוויון – מועדפת המדינה עם פחות חיילים (מאוימת יותר).
     */
    private Country findMostThreatenedCountry(Player player)
    {
        Country best = null;
        int maxEnemies = -1;

        for (Country c : player.getOwnedCountries())
        {
            int enemies = graphAnalyzer.countEnemyNeighbors(c, player);
            if (enemies > maxEnemies ||
                    (enemies == maxEnemies && best != null && c.getArmies() < best.getArmies()))
            {
                maxEnemies = enemies;
                best = c;
            }
        }
        return best;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 2 – ATTACK: בחירה וביצוע התקפות
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * בונה תור עדיפויות של מהלכי תקיפה אפשריים, שולף את הטוב ביותר,
     * ומבצע אותו. לאחר כיבוש מוצלח בונה את התור מחדש כי מצב הלוח
     * השתנה ועלולים להופיע מהלכים חדשים עם ציונים שונים.

     * סיבוכיות בנייה: O(E log E) | שליפה: O(log E) לכל תקיפה
     */
    private void chooseAttack(Player player, RiskGame game)
    {
        PriorityQueue<AttackMove> attackQueue = buildAttackQueue(player);

        while (!attackQueue.isEmpty())
        {
            AttackMove best = attackQueue.poll();

            // בדיקת תקפות – המצב עלול להשתנות בין בניית התור לשליפה
            if (!isMoveStillValid(best, player))
                continue;

            boolean conquered = performAttack(best, game);

            // רק כיבוש מצדיק בנייה מחדש – שינוי בעלות משפיע על ציוני השכנים
            if (conquered)
                attackQueue = buildAttackQueue(player);
        }
    }

    /**
     * סורק את כל גבולות השחקן ובונה Max-Heap של מהלכי תקיפה תקפים.
     */
    private PriorityQueue<AttackMove> buildAttackQueue(Player player)
    {
        PriorityQueue<AttackMove> queue = new PriorityQueue<>(
                (m1, m2) -> Double.compare(m2.heuristicScore(), m1.heuristicScore()) // Max-Heap
        );

        for (Country source : player.getOwnedCountries())
        {
            if (source.getArmies() <= 1) continue; // חייב לפחות 2 חיילים לתקיפה

            for (Country target : source.getNeighbors())
            {
                if (target.getOwner() == player) continue; // לא תוקפים מדינות שלנו

                // סינון מוקדם – מונע חישוב היוריסטיקה על מהלכים חלשים
                if (source.getArmies() - target.getArmies() < MIN_ARMY_ADVANTAGE) continue;

                double score = evaluator.calculateHeuristic(source, target, player);
                if (score > ATTACK_THRESHOLD)
                    queue.add(new AttackMove(source, target, score));
            }
        }
        return queue;
    }



    /**
     * מבצע את ההתקפה דרך מנוע המשחק.
     *
     * @return true אם הסתיים בכיבוש, false אחרת
     */
    private boolean performAttack(AttackMove move, RiskGame game)
    {
        System.out.printf("[AI ATTACK] %s (%d) → %s (%d) | Score: %.2f%n",
                move.source().getName(), move.source().getArmies(),
                move.target().getName(), move.target().getArmies(),
                move.heuristicScore());

        BattleResult result = game.attack(move.source(), move.target());
        System.out.println("[AI RESULT] " + result);

        return result.conquered();
    }

    /**
     * בודק שמהלך שנבנה קודם עדיין חוקי וכדאי בנקודת הזמן הנוכחית.
     * הכרחי כי מצב הלוח משתנה בין בניית התור לשליפה.
     */
    private boolean isMoveStillValid(AttackMove move, Player player)
    {
        if (move.source().getOwner() != player)   return false; // מקור כבר לא שלי
        if (move.source().getArmies() <= 1)        return false; // אין מספיק חיילים
        if (move.target().getOwner() == player)    return false; // יעד כבר נכבש
        return move.source().getArmies() - move.target().getArmies() >= MIN_ARMY_ADVANTAGE;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 3 – FORTIFY: העברת כוחות מעורף לחזית
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * מאתר מדינת עורף בטוחה (ללא שכנים עוינים, עם עודף חיילים),
     * ומפעיל BFS למציאת מדינת החזית הקרובה ביותר דרך רצף ידידותי.

     * סיבוכיות BFS: O(V + E) על תת-גרף מדינות השחקן
     */
    private void chooseFortify(Player player, RiskGame game)
    {
        Country safeCountry = findSafeRearCountry(player);
        if (safeCountry == null) return;

        Country borderCountry = graphAnalyzer.findConnectedBorderUsingBFS(safeCountry, player);
        if (borderCountry == null) return;

        int armiesToMove = safeCountry.getArmies() - 1; // חייל אחד נשאר תמיד
        game.fortify(safeCountry, borderCountry, armiesToMove);
        System.out.printf("[AI FORTIFY] Moved %d armies from %s → %s%n",
                armiesToMove, safeCountry.getName(), borderCountry.getName());
    }

    /**
     * מחזיר מדינת עורף: בבעלות השחקן, ללא שכנים עוינים, עם יותר מחייל אחד.
     * מועדפת המדינה עם הכי הרבה חיילים עודפים.
     */
    private Country findSafeRearCountry(Player player)
    {
        Country best = null;
        int maxSurplus = 0;

        for (Country c : player.getOwnedCountries())
        {
            if (c.getArmies() > 1 && graphAnalyzer.countEnemyNeighbors(c, player) == 0)
            {
                int surplus = c.getArmies() - 1;
                if (surplus > maxSurplus)
                {
                    maxSurplus = surplus;
                    best = c;
                }
            }
        }
        return best;
    }


}