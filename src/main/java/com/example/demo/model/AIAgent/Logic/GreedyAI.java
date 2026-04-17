package com.example.demo.model.AIAgent.Logic;

import com.example.demo.config.GameConstants;
import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import com.example.demo.model.manager.RiskGame;
import com.example.demo.model.util.MaxPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import com.example.demo.model.Records.GameRecords.AttackMove;
import com.example.demo.model.Records.GameRecords.FortifyMove;
import com.example.demo.model.Records.GameRecords.BattleResult;

/**
 * GreedyAI - בוט משחק "חמדן" שמוקד על הרווח מידי

 * אסטרטגיה:
 * - בשלב Reinforcement: הצבה חיילים בנקודות קריטיות או הגנה
 * - בשלב Attack: התקפה התוקפנית לנקודות חלשות של אויבים
 * - בשלב Fortify: תגבור וקירוב חיילים לגבולות

 * תפקידיה:
 * - בחירת בוט עבור שחקנים מחשב
 * - ביצוע קבלת החלטות אוטומטית בכל שלב
 * - אימוץ אסטרטגיה היוריסטית (מותאמת אישית)

 * השימוש: משמשת כעיקור של AI במשחק
 */
@Slf4j
public class GreedyAI implements BotStrategy {

    private final AIGraphAnalyzer graphAnalyzer = new AIGraphAnalyzer();
    private final HeuristicStrategy strategy;

    public GreedyAI(HeuristicStrategy strategy) {
        this.strategy = strategy;
    }

    //  נקודת כניסה ראשית
    /**
     * @param game - מופע המשחק הנוכחי
     * @param player - השחקן הממלא את התפקיד של ה-AI
     *               טענת יציאה: הפונקציה אחראית על ביצוע כל התור של שחקן ממוחשב.
     * */
    @Override
    public void executeTurn(Player player, RiskGame game)
    {
        if (player.getOwnedCountries().isEmpty()) {
            return;
        }

        log.info("--- AI Turn Started: {} [{}] ---", player.getName(), strategy.getClass().getSimpleName());

        chooseReinforcement(player, game);
        game.nextPhase();

        chooseAttack(player, game);
        game.nextPhase();

        chooseFortify(player, game);
        game.nextPhase();

        log.info("--- AI Turn Ended ---");
    }

    @Override
    public Country findSetUpCountry(Player player, RiskGame game)
    {
        return graphAnalyzer.findBestSetupCountry(player, strategy);
    }

    //  שלב 1 – DRAFT (מועבר לאסטרטגיה – כל אסטרטגיה מגדירה התנהגות הצבה משלה)
    private void chooseReinforcement(Player player, RiskGame game)
    {
        strategy.executeDraft(player, game, graphAnalyzer);
    }

    //  שלב 2 – ATTACK
    /**
     * @param player - השחקן הממלא את התפקיד של ה-AI
     * @param game - מופע המשחק הנוכחי
     * טענת יציאה: הפונקציה בונה תור קדימויות התקפה מבוסס על הערכות היוריסטיות של כל התקפה אפשרית,
     *             ומבצעת את ההתקפות בסדר יורד של ציון עד שהן לא רלוונטיות יותר או שהן מובילות לכיבוש.
     *            היא מתמקדת בהתקפות עם יתרון צבאי משמעותי ומעדכנת את תור ההתקפה לאחר כל כיבוש כדי לנצל הזדמנויות חדשות שנוצרו.
     * */
     private void chooseAttack(Player player, RiskGame game)
     {
         MaxPriorityQueue<AttackMove> attackQueue = graphAnalyzer.buildAttackQueue(player, strategy);

         while (!attackQueue.isEmpty())
         {
             AttackMove best = attackQueue.poll();

             // Only process valid moves - skip invalid ones
             if (isMoveStillValid(best, player))
             {
                 boolean conquered = false;

                 while (isMoveStillValid(best, player) && !conquered)
                     conquered = performAttack(best, game);


                 if (conquered)
                     attackQueue = graphAnalyzer.buildAttackQueue(player, strategy);
             }
         }
     }
    /**
     * @param game - מופע המשחק הנוכחי
     * @param move - מהלך התקפה שמכיל את המדינה התוקפת, המדינה המותקפת, והציון היוריסטי של המהלך
     * טענת יציאה: הפונקציה אחראית על ביצוע התקפה אחת בין שתי מדינות בהתאם למהלך שנבחר.
     *            היא מדווחת על התוצאה של ההתקפה, כולל האם נכבשה המדינה המותקפת, ומטפלת בכיבוש על ידי העברת חיילים בהתאם לאסטרטגיה שנבחרה.
     *             היא מחזירה אמת אם ההתקפה הובילה לכיבוש, ושקר אחרת.
     * */
    private boolean performAttack(AttackMove move, RiskGame game) {
        log.info("[AI ATTACK] {} ({}) → {} ({}) | Score: {}",
                move.source().getName(), move.source().getArmies(),
                move.target().getName(), move.target().getArmies(),
                String.format("%.2f", move.heuristicScore()));

        BattleResult result = game.attack(move.source(), move.target());
        if (result != null && result.conquered())
        {
            int amountToMove = this.strategy.getTroopsToMoveAfterConquest(
                    move.source(),
                    move.target(),
                    result.minMove(),
                    result.maxMove()
            );
            game.handleConquest(move.source(), move.target(), amountToMove);
        }
        assert result != null;

        log.info("[AI RESULT] {}", result.conquered() ? "Conquered!" : "Failed to conquer.");
        return result.conquered();
    }
    /**
     * @param player - השחקן הממלא את התפקיד של ה-AI
     * @param move - מהלך התקפה שמכיל את המדינה התוקפת, המדינה המותקפת, והציון היוריסטי של המהלך
     * טענת יציאה: הפונקציה בודקת אם מהלך התקפה שנבחר עדיין תקף בהתחשב בשינויים שקרו במפה מאז שהמהלך נבחר.
     *             היא בודקת אם המדינה התוקפת עדיין שייכת לשחקן, אם יש מספיק חיילים לבצע התקפה,
     *             אם המדינה המותקפת עדיין שייכת לאויב, ואם היתרון הצבאי עדיין עומד בדרישות האסטרטגיה. היא מחזירה אמת אם המהלך עדיין תקף, ושקר אחרת.
     * */
    private boolean isMoveStillValid(AttackMove move, Player player)
    {
        if (move.source().getOwner() != player) return false;
        if (move.source().getArmies() <= GameConstants.MIN_ARMIES_TO_STAY) return false;
        if (move.target().getOwner() == player) return false;

        return move.source().getArmies() - move.target().getArmies() >= strategy.getMinArmyAdvantage();
    }

    //  שלב 3 – FORTIFY
    /**
     * @param player - השחקן הממלא את התפקיד של ה-AI
     * @param game - מופע המשחק הנוכחי
     * טענת יציאה: הפונקציה אחראית על חיזוק הגבולות של השחקן על ידי זיהוי חיילים "כלואים" במדינות פנימיות שאין להם דרך להתקדם או לתקוף,
     *             ומעבר שלהם למדינות גבוליות שיכולות לתמוך בהתקפות עתידיות או בהגנה.
     *            היא משתמשת בניתוח גרפי כדי לזהות את החיילים הללו ומבצעת את המעבר בהתאם,
     *            תוך דיווח על הפעולה שבוצעה. אם אין חיילים כלואים, היא מדווחת על כך ומדלגת על שלב החיזוק.
     * */
    private void chooseFortify(Player player, RiskGame game)
    {
        FortifyMove smartMove = graphAnalyzer.calculateBestFortify(player);

        if (smartMove != null)
        {
            game.fortify(smartMove.source(), smartMove.target(), smartMove.armiesToMove());
            log.info("[AI FORTIFY] Moved {} armies from {} (Trapped) to {} (Border)",
                    smartMove.armiesToMove(), smartMove.source().getName(), smartMove.target().getName());
        }
        else
            log.info("[AI FORTIFY] No trapped armies to move. Skipping fortify.");

    }
}