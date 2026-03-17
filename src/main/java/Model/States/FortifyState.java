package Model.States;

import Model.Country;
import Model.Player;
import Model.Records.BattleResult;
import Model.RiskGame;

import java.util.*;

public class FortifyState implements GameState {

    private final RiskGame game;

    public FortifyState(RiskGame game) {
        this.game = game;
    }

    @Override
    public boolean placeArmy(Country country) {
        return false;
    }

    @Override
    public BattleResult attack(Country attacker, Country defender) {
        return null; //"Wrong phase! You are currently in the Fortify phase."
    }

    @Override
    public String fortify(Country from, Country to, int amount) {
        Player currentPlayer = game.getCurrentPlayer();

        if (from.getOwner() != currentPlayer || to.getOwner() != currentPlayer) return "Must own both!";
        if (!from.getNeighbors().contains(to)) return "Countries must be neighbors!";
        if (from.getArmies() - amount < 1) return "Must leave at least 1 army behind!";

        from.removeArmies(amount);
        to.addArmies(amount);

        return "Moved " + amount + " armies successfully.";
    }

    @Override
    public void nextPhase() {
        // סיום התור! אנו קוראים לפונקציה של RiskGame שתעביר לשחקן הבא.
        // הפונקציה nextTurn() במחלקת RiskGame כבר דואגת לאתחל את השלב הבא ל-DraftState.
        game.nextTurn();
    }

    @Override
    //gets the valid targets for fortifying using BFS.
    public Set<Country> getValidTargets(Country source) {
        Queue<Country> queue = new LinkedList<>();
        HashSet<Country> visited = new HashSet<>();
        queue.add(source);
        visited.add(source);
        while (!queue.isEmpty())
        {
            Country current = queue.poll();
            for(Country neighbor : current.getNeighbors())
            {
                if(neighbor.getOwner() == game.getCurrentPlayer() && !visited.contains(neighbor))
                {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        visited.remove(source);
        return visited;
    }

    @Override
    public String getPhaseName() {
        return "FORTIFY";
    }
}