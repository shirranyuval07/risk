package com.example.demo.model.AIAgent.Strategies;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.AIAgent.Rules.HeuristicRule;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Interface and implementations for AI heuristic decision-making.
 * Consolidated into a single file to reduce class explosion while maintaining OCP.
 */
public interface HeuristicStrategy {
    double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer);
    double getAttackThreshold();
    double getMinArmyAdvantage();
    int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove);
    double getSetupStackingWeight();

    /**
     * Base implementation with core heuristic logic.
     */
    abstract class Abstract implements HeuristicStrategy {
        protected final double weightWinProb;
        protected final double weightContinentBonus;
        protected final double weightStrategicValue;
        protected final double weightExpectedCasualties;
        protected final double articulationPointBonus;
        protected final double casualtiesMultiplier;
        protected final double exposurePenaltyMultiplier;
        protected final double setupStackingWeight;
        protected final double attackThreshold;
        protected final double minArmyAdvantage;
        private final Map<HeuristicRule, Double> dynamicRules = new HashMap<>();

        public Abstract(double wWin, double wCont, double wStrat, double wCost,
                        double artBonus, double casMult, double exposurePenalty,
                        double threshold, double minAdv, double setupStackingWeight) {
            this.weightWinProb = wWin;
            this.weightContinentBonus = wCont;
            this.weightStrategicValue = wStrat;
            this.weightExpectedCasualties = wCost;
            this.articulationPointBonus = artBonus;
            this.casualtiesMultiplier = casMult;
            this.exposurePenaltyMultiplier = exposurePenalty;
            this.attackThreshold = threshold;
            this.minArmyAdvantage = minAdv;
            this.setupStackingWeight = setupStackingWeight;
        }

        protected void addRule(HeuristicRule rule, double weight) {
            dynamicRules.put(rule, weight);
        }

        @Override
        public double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer) {
            double pWin = estimateWinProbability(source.getArmies(), target.getArmies());
            double sStrat = calculateStrategicValue(target, analyzer);
            double cCost = estimateExpectedLoss(target.getArmies(), source.getArmies());

            if (isSourceExposedAfterAttack(source, target, player)) {
                cCost *= this.exposurePenaltyMultiplier;
            }

            double baseScore = (weightWinProb * pWin) + (weightStrategicValue * sStrat) - (weightExpectedCasualties * cCost);

            double dynamicScore = 0;
            for (Map.Entry<HeuristicRule, Double> entry : dynamicRules.entrySet()) {
                dynamicScore += entry.getKey().evaluate(source, target, player, analyzer) * entry.getValue();
            }
            return baseScore + dynamicScore;
        }

        private double estimateWinProbability(int attackerArmies, int defenderArmies) {
            double ratio = (double) (attackerArmies - 1) / Math.max(defenderArmies, 1);
            double prob = 0.5 + (ratio - 1.0) * 0.2;
            return Math.max(0.1, Math.min(0.95, prob));
        }

        private double calculateStrategicValue(Country target, AIGraphAnalyzer analyzer) {
            double score = 0;
            long enemyNeighbors = target.getNeighbors().stream().filter(n -> n.getOwner() != target.getOwner()).count();
            score += Math.max(0, 3 - enemyNeighbors) * 0.5;

            Player enemy = target.getOwner();
            if (enemy != null) {
                Set<Country> enemyBottlenecks = analyzer.findArticulationPoints(enemy);
                if (enemyBottlenecks.contains(target)) {
                    score += this.articulationPointBonus;
                }
            }
            return score;
        }

        private double estimateExpectedLoss(int defenderArmies, int attackerArmies) {
            return ((double) defenderArmies / Math.max(attackerArmies, 1)) * this.casualtiesMultiplier;
        }

        private boolean isSourceExposedAfterAttack(Country source, Country target, Player player) {
            int remainingArmies = source.getArmies() - 1;
            for (Country neighbor : source.getNeighbors()) {
                if (neighbor == target) continue;
                if (neighbor.getOwner() != player && neighbor.getArmies() >= remainingArmies) return true;
            }
            return false;
        }

        @Override public double getAttackThreshold() { return this.attackThreshold; }
        @Override public double getMinArmyAdvantage() { return this.minArmyAdvantage; }
        @Override public double getSetupStackingWeight() { return this.setupStackingWeight; }

        @Override
        public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
            boolean isSourceNowSafe = true;
            for (Country neighbor : source.getNeighbors()) {
                if (neighbor != target && neighbor.getOwner() != source.getOwner()) {
                    isSourceNowSafe = false;
                    break;
                }
            }
            if (isSourceNowSafe) return maxMove;
            return Math.max(minMove, Math.min(maxMove, source.getArmies() / 2));
        }
    }

    /**
     * Concrete implementation that allows external configuration of weights and behaviors.
     */
    class Configurable extends Abstract {
        private final TroopMovementBehavior movementBehavior;

        public Configurable(double wWin, double wCont, double wStrat, double wCost, double artBonus,
                            double casMult, double expPenalty, double attackThresh, int minAdv,
                            double weightFutureThreat, double continentBreakMultiplier, double bonusFocus,
                            double progressFocus, double resistanceAvoidance, double setupWeight,
                            TroopMovementBehavior movementBehavior) {
            super(wWin, wCont, wStrat, wCost, artBonus, casMult, expPenalty, attackThresh, minAdv, setupWeight);
            this.addRule(HeuristicRule.futureThreatRule(), weightFutureThreat);
            this.addRule(HeuristicRule.continentProgressRule(continentBreakMultiplier, bonusFocus, progressFocus, resistanceAvoidance), wCont);
            this.movementBehavior = movementBehavior;
        }

        @Override
        public int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove) {
            return movementBehavior.calculate(source.getArmies(), minMove, maxMove);
        }

        @FunctionalInterface
        public interface TroopMovementBehavior {
            int calculate(int totalArmies, int minMove, int maxMove);
        }
    }
}