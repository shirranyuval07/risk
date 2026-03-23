package Model.States;

import Model.Country;
import Model.Player;
import Model.Records.BattleResult;
import Model.RiskGame;

import java.util.HashSet;
import java.util.Set;

public class SetupState implements GameState {

    private final RiskGame game;

    public SetupState(RiskGame game) {
        this.game = game;
    }

    @Override
    public boolean placeArmy(Country country) {
        Player currentPlayer = game.getCurrentPlayer();

        // 1. Validation: Must own the country and have armies left to place
        if (country.getOwner() != currentPlayer) return false;
        if (currentPlayer.getDraftArmies() <= 0) return false;

        // 2. Action: Place exactly 1 army
        country.addArmies(1);
        currentPlayer.decreaseDraftArmies();

        // 3. Check if setup is fully complete for ALL players
        boolean allArmiesPlaced = game.getPlayers().stream()
                .allMatch(p -> p.getDraftArmies() <= 0);

        if (allArmiesPlaced) {
            // Everyone is out of initial armies. Start the real game!
            nextPhase();
        } else {
            // Still setting up: pass the turn to the next player
            game.advanceSetupTurn();
        }
        return true;
    }



    @Override
    public void nextPhase() {
        // Setup is done. nextTurn() will pass the turn to the starting player,
        // calculate their initial reinforcements, and put them in the standard DraftState.
        game.nextTurn();
    }

    @Override
    public Set<Country> getValidTargets(Country source) {
        return new HashSet<>();
    }

    @Override
    public String getPhaseName() {
        return "SETUP";
    }
}