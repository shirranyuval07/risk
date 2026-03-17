package Model.AIAgent;

import Model.Country;
import Model.Player;
import Model.RiskGame;

public interface BotStrategy {
    void executeTurn(Player player, RiskGame game);
    Country findSetUpCountry(Player player,RiskGame game);
}