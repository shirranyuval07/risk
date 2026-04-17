package com.example.demo.model.AIAgent.Logic;

import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import com.example.demo.model.manager.RiskGame;

public interface BotStrategy {
    void executeTurn(Player player, RiskGame game);
    Country findSetUpCountry(Player player,RiskGame game);
}