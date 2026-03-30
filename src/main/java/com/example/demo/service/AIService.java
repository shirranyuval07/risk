package com.example.demo.service;

import com.example.demo.model.CardService;
import com.example.demo.model.Country;
import com.example.demo.model.Player;
import com.example.demo.model.RiskGame;
import com.example.demo.model.States.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to execute turns for AI players, maintaining decoupling from the Player model.
 */
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final CardService cardService;

    public void playTurn(Player aiPlayer, RiskGame game) {
        if (!aiPlayer.isAI() || aiPlayer.getStrategy() == null) {
            return;
        }

        // Handle Setup phase
        if (game.getCurrentState() instanceof GameState.SetupState) {
            Country c = aiPlayer.getStrategy().findSetUpCountry(aiPlayer, game);
            game.placeArmy(c);
            return;
        }

        // Handle Card Trading before regular phases
        int tradeResult;
        do {
            tradeResult = cardService.tradeAnyValidSet(aiPlayer);
            if (tradeResult > 0) {
                aiPlayer.setDraftArmies(aiPlayer.getDraftArmies() + tradeResult);
                log.info("🤖 AI " + aiPlayer.getName() + " traded cards for " + tradeResult + " extra armies!");
            }
        } while (tradeResult > 0);

        // Execute regular game phases (Draft, Attack, Fortify)
        aiPlayer.getStrategy().executeTurn(aiPlayer, game);

        // Auto-advance phases to pass turn to next player
        while (game.getCurrentPlayer() == aiPlayer && !game.isGameOver()) {
            game.nextPhase();
        }
    }
}
