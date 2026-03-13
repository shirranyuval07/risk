package Model.AIAgent;

import Model.*;
import Model.AIAgent.Strategies.HeuristicStrategy;
import Model.Records.AttackMove;
import Model.Records.BattleResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GreedyAI implements BotStrategy {

    private final AIGraphAnalyzer graphAnalyzer = new AIGraphAnalyzer();

    // 1. משתנה האסטרטגיה שמחליף את ה-Evaluator ואת כל הקבועים
    private final HeuristicStrategy strategy;

    // 2. הבנאי שמקבל את "המוח" מבחוץ (Dependency Injection)
    public GreedyAI(HeuristicStrategy strategy) {
        this.strategy = strategy;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  נקודת כניסה ראשית
    // ═══════════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 1 – DRAFT
    // ═══════════════════════════════════════════════════════════════════════════

    private void chooseReinforcement(Player player, RiskGame game) {
        Country mostThreatened = findMostThreatenedCountry(player);
        if (mostThreatened == null) return;

        int startArmies = player.getDraftArmies();
        while (player.getDraftArmies() > 0)
            game.placeArmy(mostThreatened);

        log.debug("[AI DRAFT] Placed {} armies on: {}", startArmies, mostThreatened.getName());
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

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 2 – ATTACK
    // ═══════════════════════════════════════════════════════════════════════════

    private void chooseAttack(Player player, RiskGame game) {
        MaxPriorityQueue<AttackMove> attackQueue = buildAttackQueue(player);

        while (!attackQueue.isEmpty()) {
            AttackMove best = attackQueue.poll();

            if (!isMoveStillValid(best, player))
                continue;

            boolean conquered = performAttack(best, game);

            if (conquered)
                attackQueue = buildAttackQueue(player);
        }
    }

    private MaxPriorityQueue<AttackMove> buildAttackQueue(Player player) {
        MaxPriorityQueue<AttackMove> queue = new MaxPriorityQueue<>();

        for (Country source : player.getOwnedCountries()) {
            if (source.getArmies() <= 1) continue;

            for (Country target : source.getNeighbors()) {
                if (target.getOwner() == player) continue;

                // 3. שימוש באסטרטגיה לבדיקת היתרון המינימלי
                if (source.getArmies() - target.getArmies() < strategy.getMinArmyAdvantage()) continue;

                // 4. שימוש באסטרטגיה לחישוב הציון
                double score = strategy.calculateHeuristic(source, target, player, graphAnalyzer);

                // 5. שימוש באסטרטגיה לבדיקת סף ההתקפה
                if (score > strategy.getAttackThreshold())
                    queue.add(new AttackMove(source, target, score));
            }
        }
        return queue;
    }

    private boolean performAttack(AttackMove move, RiskGame game) {
        log.info("[AI ATTACK] {} ({}) → {} ({}) | Score: {}",
                move.source().getName(), move.source().getArmies(),
                move.target().getName(), move.target().getArmies(),
                String.format("%.2f", move.heuristicScore()));

        BattleResult result = game.attack(move.source(), move.target());
        log.info("[AI RESULT] {}", result);

        return result.conquered();
    }

    private boolean isMoveStillValid(AttackMove move, Player player) {
        if (move.source().getOwner() != player) return false;
        if (move.source().getArmies() <= 1) return false;
        if (move.target().getOwner() == player) return false;

        // 6. שימוש באסטרטגיה לבדיקת חוקיות מעודכנת
        return move.source().getArmies() - move.target().getArmies() >= strategy.getMinArmyAdvantage();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 3 – FORTIFY
    // ═══════════════════════════════════════════════════════════════════════════

    private void chooseFortify(Player player, RiskGame game) {
        Country safeCountry = findSafeRearCountry(player);
        if (safeCountry == null) return;

        Country borderCountry = graphAnalyzer.findConnectedBorderUsingBFS(safeCountry, player);
        if (borderCountry == null) return;

        int armiesToMove = safeCountry.getArmies() - 1;
        game.fortify(safeCountry, borderCountry, armiesToMove);

        log.debug("[AI FORTIFY] Moved {} armies from {} → {}",
                armiesToMove, safeCountry.getName(), borderCountry.getName());
    }

    private Country findSafeRearCountry(Player player) {
        Country best = null;
        int maxSurplus = 0;

        for (Country c : player.getOwnedCountries()) {
            if (c.getArmies() > 1 && graphAnalyzer.countEnemyNeighbors(c, player) == 0) {
                int surplus = c.getArmies() - 1;
                if (surplus > maxSurplus) {
                    maxSurplus = surplus;
                    best = c;
                }
            }
        }
        return best;
    }
}