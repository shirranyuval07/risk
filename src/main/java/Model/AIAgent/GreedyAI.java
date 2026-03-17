package Model.AIAgent;

import Model.*;
import Model.AIAgent.Strategies.DefensiveStrategy;
import Model.AIAgent.Strategies.HeuristicStrategy;
import Model.AIAgent.Strategies.OffensiveStrategy;
import Model.Records.AttackMove;
import Model.Records.BattleResult;
import Model.Records.FortifyMove;
import Model.util.MaxPriorityQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    @Override
    public Country findSetUpCountry(Player player, RiskGame game)
    {
        double stackingWeight = strategy.getSetupStackingWeight(); // for balanced
        Country bestCountry = null;
        double bestScore = -1;
        for(Country country : player.getOwnedCountries())
        {
            int currentEnemyCount = 0;
            for(Country neighbor : country.getNeighbors())
            {
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
    //  שלב 1 – DRAFT
    // ═══════════════════════════════════════════════════════════════════════════

    private void chooseReinforcement(Player player, RiskGame game) {
        // --- תיקון: פיצול התנהגות גיוס לפי אופי הבוט ---
        // נשתמש בסף ההתקפה כדי להבין אם הבוט התקפי (סף נמוך) או הגנתי
        boolean isAggressive = strategy.getAttackThreshold() < 0.5;

        if (isAggressive) {
            // התנהגות התקפית: יצירת "ראש חץ"
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

            // זורקים את כל התגבורת על נקודת הפריצה הזו כדי לדרוס את היבשת!
            if (bestPotentialAttack != null) {
                while (player.getDraftArmies() > 0) {
                    game.placeArmy(bestPotentialAttack.source());
                }
                log.info("[AI DRAFT] Offensive Spearhead: Dumped all armies on {}", bestPotentialAttack.source().getName());
                return; // מסיימים את הגיוס
            }
        }

        // --- ההתנהגות ההגנתית המקורית (פיזור פרופורציונלי לפי איומים) ---
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

            // אם ההתקפה כבר לא חוקית עוד לפני שהתחלנו, דלג עליה
            if (!isMoveStillValid(best, player))
                continue;

            boolean conquered = false;

            //  המשך לתקוף את אותו היעד כל עוד היתרון שלנו נשמר ולא כבשנו!
            while (isMoveStillValid(best, player) && !conquered) {
                conquered = performAttack(best, game);
            }

            // אם בסופו של דבר כבשנו את המדינה, מפת המשחק השתנתה משמעותית, אז נבנה את התור מחדש
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
        log.info("[AI RESULT] {}", result);

        assert result != null;
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
        FortifyMove smartMove = graphAnalyzer.calculateBestFortify(player);

        if (smartMove != null) {
            // 2. מבצעים את המהלך מול לוגיקת המשחק
            game.fortify(smartMove.source(), smartMove.target(), smartMove.armiesToMove());

            log.info("[AI FORTIFY] Moved {} armies from {} (Trapped) to {} (Border)",
                    smartMove.armiesToMove(), smartMove.source().getName(), smartMove.target().getName());
        } else {
            log.info("[AI FORTIFY] No trapped armies to move. Skipping fortify.");
        }
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