package com.example.demo.model.AIAgent;

import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import com.example.demo.model.Country;
import com.example.demo.model.Player;
import com.example.demo.model.Records.AttackMove;
import com.example.demo.model.Records.BattleResult;
import com.example.demo.model.Records.FortifyMove;
import com.example.demo.model.RiskGame;
import com.example.demo.model.util.MaxPriorityQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class GreedyAI implements BotStrategy {

    private final AIGraphAnalyzer graphAnalyzer = new AIGraphAnalyzer();
    private final HeuristicStrategy strategy;

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

    @Override
    public Country findSetUpCountry(Player player, RiskGame game) {
        double stackingWeight = strategy.getSetupStackingWeight();
        Country bestCountry = null;
        double bestScore = -1;
        for(Country country : player.getOwnedCountries()) {
            int currentEnemyCount = 0;
            for(Country neighbor : country.getNeighbors()) {
                if(neighbor.getOwner() != player)
                    currentEnemyCount += neighbor.getArmies();
            }
            if (currentEnemyCount > 0) {
                double score = currentEnemyCount + (country.getArmies() * stackingWeight);
                if (score > bestScore) {
                    bestCountry = country;
                    bestScore = score;
                }
            }
        }
        if(bestCountry == null)
            return player.getOwnedCountries().getFirst();
        return bestCountry;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 1 – DRAFT (פיצול נקי)
    // ═══════════════════════════════════════════════════════════════════════════

    private void chooseReinforcement(Player player, RiskGame game) {
        boolean isAggressive = strategy.getAttackThreshold() < 0;

        if (isAggressive) {
            executeAggressiveDraft(player, game);
        } else {
            executeDefensiveDraft(player, game);
        }
    }

    private void executeAggressiveDraft(Player player, RiskGame game) {
        AttackMove bestPotentialAttack = null;
        for (Country source : player.getOwnedCountries()) {
            for (Country target : source.getNeighbors()) {
                if (target.getOwner() != player) {
                    double score = strategy.calculateHeuristic(source, target, player, graphAnalyzer);
                    if (bestPotentialAttack == null || score > bestPotentialAttack.heuristicScore()) {
                        bestPotentialAttack = new AttackMove(source, target, score);
                    }
                }
            }
        }

        if (bestPotentialAttack != null) {
            while (player.getDraftArmies() > 0) {
                game.placeArmy(bestPotentialAttack.source());
            }
            log.info("[AI DRAFT] Offensive Spearhead: Dumped all armies on {}", bestPotentialAttack.source().getName());
        }
    }

    private void executeDefensiveDraft(Player player, RiskGame game) {
        Map<Country, Double> threatScores = new HashMap<>();
        double totalThreat = 0.0;
        Set<Country> myBottlenecks = graphAnalyzer.findArticulationPoints(player);

        for (Country country : player.getOwnedCountries()) {
            int enemyStrength = 0;
            for (Country neighbor : country.getNeighbors()) {
                if (neighbor.getOwner() != player) {
                    enemyStrength += neighbor.getArmies();
                }
            }

            if (enemyStrength > 0) {
                double score = (double) enemyStrength / Math.max(country.getArmies(), 1);
                if (myBottlenecks.contains(country)) score *= 2.0;
                threatScores.put(country, score);
                totalThreat += score;
            }
        }

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

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 2 – ATTACK
    // ═══════════════════════════════════════════════════════════════════════════

    private void chooseAttack(Player player, RiskGame game) {
        MaxPriorityQueue<AttackMove> attackQueue = buildAttackQueue(player);

        while (!attackQueue.isEmpty()) {
            AttackMove best = attackQueue.poll();

            if (!isMoveStillValid(best, player))
                continue;

            boolean conquered = false;

            while (isMoveStillValid(best, player) && !conquered) {
                conquered = performAttack(best, game);
            }

            if (conquered) {
                attackQueue = buildAttackQueue(player);
            }
        }
    }

    private MaxPriorityQueue<AttackMove> buildAttackQueue(Player player) {
        MaxPriorityQueue<AttackMove> queue = new MaxPriorityQueue<>();

        for (Country source : player.getOwnedCountries()) {
            if (source.getArmies() <= 1) continue;

            for (Country target : source.getNeighbors()) {
                if (target.getOwner() == player) continue;

                if (source.getArmies() - target.getArmies() < strategy.getMinArmyAdvantage()) continue;

                double score = strategy.calculateHeuristic(source, target, player, graphAnalyzer);

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
        if (move.source().getArmies() <= 1) return false;
        if (move.target().getOwner() == player) return false;

        return move.source().getArmies() - move.target().getArmies() >= strategy.getMinArmyAdvantage();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  שלב 3 – FORTIFY
    // ═══════════════════════════════════════════════════════════════════════════

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