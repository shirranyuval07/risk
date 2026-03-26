package com.example.demo.model.AIAgent.Strategies;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.Country;
import com.example.demo.model.Player;

public interface HeuristicStrategy {
    double calculateHeuristic(Country source, Country target, Player player, AIGraphAnalyzer analyzer);
    double getAttackThreshold();
    double getMinArmyAdvantage();
    int getTroopsToMoveAfterConquest(Country source, Country target, int minMove, int maxMove);
    double getSetupStackingWeight();
}