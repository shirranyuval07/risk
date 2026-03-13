package Model.AIAgent.Strategies;

import Model.AIAgent.AIGraphAnalyzer;
import Model.Country;
import Model.Player;

public interface HeuristicStrategy {
    double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer);
    double getAttackThreshold();
    double getMinArmyAdvantage();

}