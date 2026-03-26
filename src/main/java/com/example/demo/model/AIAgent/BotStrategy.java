package com.example.demo.model.AIAgent;

import com.example.demo.model.Country;
import com.example.demo.model.Player;
import com.example.demo.model.RiskGame;

public interface BotStrategy {
    void executeTurn(Player player, RiskGame game);
    Country findSetUpCountry(Player player,RiskGame game);
}