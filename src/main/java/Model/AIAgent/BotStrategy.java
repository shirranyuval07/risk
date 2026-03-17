package Model.AIAgent;

import Model.Player;
import Model.RiskGame;

public interface BotStrategy {
    void executeTurn(Player player, RiskGame game);
}