package com.example.demo.model.AIAgent;

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

import java.util.Map;
import java.util.Set;

/**
 * GreedyAI - בוט משחק "חמדן" שמוקד על הרווח מידי
 * 
 * אסטרטגיה:
 * - בשלב Reinforcement: הצבה חיילים בנקודות קריטיות או הגנה
 * - בשלב Attack: התקפה התוקפנית לנקודות חלשות של אויבים
 * - בשלב Fortify: תגבור וקירוב חיילים לגבולות
 * 
 * תפקידיה:
 * - בחירת בוט עבור שחקנים מחשב
 * - ביצוע קבלת החלטות אוטומטית בכל שלב
 * - אימוץ אסטרטגיה היוריסטית (מותאמת אישית)
 * 
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

    @Override
    public void executeTurn(Player player, RiskGame game) {
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
    public Country findSetUpCountry(Player player, RiskGame game) {
        return graphAnalyzer.findBestSetupCountry(player, strategy.getSetupStackingWeight());
    }

    //  שלב 1 – DRAFT

    private void chooseReinforcement(Player player, RiskGame game) {
        boolean isAggressive = strategy.getAttackThreshold() < 0;

        if (isAggressive) {
            executeAggressiveDraft(player, game);
        } else {
            executeDefensiveDraft(player, game);
        }
    }

    private void executeAggressiveDraft(Player player, RiskGame game) {
        AttackMove bestPotentialAttack = graphAnalyzer.findBestPotentialAttack(player, strategy);

        if (bestPotentialAttack != null) {
            while (player.getDraftArmies() > 0) {
                game.placeArmy(bestPotentialAttack.source());
            }
            log.info("[AI DRAFT] Offensive Spearhead: Dumped all armies on {}", bestPotentialAttack.source().getName());
        }
    }

    private void executeDefensiveDraft(Player player, RiskGame game) {
        Set<Country> myBottlenecks = graphAnalyzer.findArticulationPoints(player);
        Map<Country, Double> threatScores = graphAnalyzer.calculateThreatScores(player, myBottlenecks);

        double totalThreat = threatScores.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalDraftArmies = player.getDraftArmies();

        if (totalThreat == 0) {
            Country fallback = findMostThreatenedCountry(player);
            if (fallback != null) {
                while (player.getDraftArmies() > 0) game.placeArmy(fallback);
            }
            return;
        }

        for (Map.Entry<Country, Double> entry : threatScores.entrySet()) {
            int armiesForThisCountry = (int) Math.floor((entry.getValue() / totalThreat) * totalDraftArmies);
            for (int i = 0; i < armiesForThisCountry; i++) {
                if (player.getDraftArmies() > 0) game.placeArmy(entry.getKey());
            }
        }

        // פיזור שאריות החיילים במדינה המאוימת ביותר
        Country mostThreatened = findMostThreatenedCountry(player);
        while (player.getDraftArmies() > 0 && mostThreatened != null) {
            game.placeArmy(mostThreatened);
        }
    }

    private Country findMostThreatenedCountry(Player player) {
        Country best = null;
        int maxEnemies = -1;

        for (Country c : player.getOwnedCountries()) {
            int enemies = graphAnalyzer.countEnemyNeighbors(c, player);
            if (enemies > maxEnemies ||
                    (enemies == maxEnemies && best != null && c.getArmies() < best.getArmies())) {
                maxEnemies = enemies;
                best = c;
            }
        }
        return best;
    }

    //  שלב 2 – ATTACK

    private void chooseAttack(Player player, RiskGame game) {
        MaxPriorityQueue<AttackMove> attackQueue = graphAnalyzer.buildAttackQueue(player, strategy);

        while (!attackQueue.isEmpty()) {
            AttackMove best = attackQueue.poll();

            if (!isMoveStillValid(best, player))
                continue;

            boolean conquered = false;

            while (isMoveStillValid(best, player) && !conquered) {
                conquered = performAttack(best, game);
            }

            if (conquered) {
                attackQueue = graphAnalyzer.buildAttackQueue(player, strategy);
            }
        }
    }

    private boolean performAttack(AttackMove move, RiskGame game) {
        log.info("[AI ATTACK] {} ({}) → {} ({}) | Score: {}",
                move.source().getName(), move.source().getArmies(),
                move.target().getName(), move.target().getArmies(),
                String.format("%.2f", move.heuristicScore()));

        BattleResult result = game.attack(move.source(), move.target());
        if (result != null && result.conquered()) {
            int amountToMove = this.strategy.getTroopsToMoveAfterConquest(
                    move.source(),
                    move.target(),
                    result.minMove(),
                    result.maxMove()
            );
            game.handleConquest(move.source(), move.target(), amountToMove);
        }
        log.info("[AI RESULT] {}", result);

        assert result != null;
        return result.conquered();
    }

    private boolean isMoveStillValid(AttackMove move, Player player) {
        if (move.source().getOwner() != player) return false;
        if (move.source().getArmies() <= GameConstants.MIN_ARMIES_TO_STAY) return false;
        if (move.target().getOwner() == player) return false;

        return move.source().getArmies() - move.target().getArmies() >= strategy.getMinArmyAdvantage();
    }

    //  שלב 3 – FORTIFY

    private void chooseFortify(Player player, RiskGame game) {
        FortifyMove smartMove = graphAnalyzer.calculateBestFortify(player);

        if (smartMove != null) {
            game.fortify(smartMove.source(), smartMove.target(), smartMove.armiesToMove());
            log.info("[AI FORTIFY] Moved {} armies from {} (Trapped) to {} (Border)",
                    smartMove.armiesToMove(), smartMove.source().getName(), smartMove.target().getName());
        } else {
            log.info("[AI FORTIFY] No trapped armies to move. Skipping fortify.");
        }
    }
}