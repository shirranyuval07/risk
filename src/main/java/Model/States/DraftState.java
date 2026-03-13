package Model.States;

import Model.Country;
import Model.Player;
import Model.Records.BattleResult;
import Model.RiskGame;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class DraftState implements GameState
{
    private final RiskGame game;

    public DraftState(RiskGame game)
    {
        this.game = game;
    }

    @Override
    public boolean placeArmy(Country country) {
        Player currentPlayer = game.getCurrentPlayer();

        if (country.getOwner() != currentPlayer) return false;
        if (currentPlayer.getDraftArmies() <= 0) return false;

        country.addArmies(1);
        currentPlayer.decreaseDraftArmies();

        game.notifyObservers();
        return true;
    }

    @Override
    public BattleResult attack(Country attacker, Country defender) {
        return null;
    }

    @Override
    public String fortify(Country from, Country to, int amount) {
        return "Wrong phase! You are currently in the Draft phase.";
    }

    @Override
    public void nextPhase() {
        if (game.getCurrentPlayer().getDraftArmies() > 0) {
            log.warn("Cannot advance: You must place all draft armies.");
            return;
        }

        game.setCurrentState(new AttackState(game));
        game.notifyObservers();
    }

    @Override
    public Set<Country> getValidTargets(Country source) {
        return new HashSet<>();
    }

    @Override
    public String getPhaseName() {
        return "";
    }
}