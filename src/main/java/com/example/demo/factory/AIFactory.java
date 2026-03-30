package com.example.demo.factory;

import com.example.demo.model.AIAgent.BotStrategy;
import com.example.demo.model.AIAgent.GreedyAI;
import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import org.springframework.stereotype.Component;

/**
 * Factory to create AI instances with specific strategies.
 * Enhances DIP by abstracting the concrete AI creation logic.
 */
@Component
public class AIFactory {

    /**
     * Creates a standard AI using the GreedyAI implementation and a given heuristic strategy.
     * @param strategy The heuristic strategy to use (Balanced, Offensive, Defensive).
     * @return A concrete BotStrategy implementation.
     */
    public BotStrategy createAI(HeuristicStrategy strategy) {
        return new GreedyAI(strategy);
    }
}
