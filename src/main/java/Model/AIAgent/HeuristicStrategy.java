package Model.AIAgent;

import Model.Country;
import Model.Player;
import Model.RiskGame;

public interface HeuristicStrategy {
    double calculateHeuristic(Country source, Country target, Player player,AIGraphAnalyzer analyzer);
    double getAttackThreshold();
    double getMinArmyAdvantage();

}