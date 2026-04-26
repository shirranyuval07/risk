package com.example.demo.service;

import com.example.demo.model.util.Card;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import com.example.demo.model.manager.RiskGame;
import com.example.demo.model.States.GameState;
import com.example.demo.model.AIAgent.Logic.BotStrategy;
import com.example.demo.model.AIAgent.Logic.GreedyAI;
import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Integrated AI engine that handles both turn execution and AI creation.
 */
public class AIEngine {

    /**
         * Service to execute turns for AI players, maintaining decoupling from the Player model.
         */
        @Slf4j
        public record Service(Card.Service cardService) {

            public void playTurn(Player aiPlayer, RiskGame game) {
                if (!aiPlayer.isAI() || aiPlayer.getStrategy() == null)
                    return;
                // Handle Setup phase
                if (game.getCurrentState() instanceof GameState.SetupState) {
                    Country c = aiPlayer.getStrategy().findSetUpCountry(aiPlayer, game);
                    game.placeArmy(c);
                    return;
                }
                // Execute regular game phases (Draft, Attack, Fortify)
                aiPlayer.getStrategy().executeTurn(aiPlayer, game);

                // Auto-advance phases to pass turn to the next player
                while (game.getCurrentPlayer() == aiPlayer && !game.isGameOver())
                    game.nextPhase();

            }
        }

    /**
     * Factory to create AI instances with specific strategies.
     * Enhances DIP by abstracting the concrete AI creation logic.
     */
    @Component
    public static class Factory
    {

        /**
         * Creates a standard AI using the GreedyAI implementation and a given heuristic strategy.
         * @param strategy The heuristic strategy to use (Balanced, Offensive, Defensive).
         * @return A concrete BotStrategy implementation.
         */
        public BotStrategy createAI(HeuristicStrategy strategy) {
            return new GreedyAI(strategy);
        }
    }
}
