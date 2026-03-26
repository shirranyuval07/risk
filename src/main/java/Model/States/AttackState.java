package Model.States;

import Model.Country;
import Model.Dice;
import Model.Player;
import Model.Records.BattleResult;
import Model.RiskGame;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class AttackState implements GameState {

    private final RiskGame game;

    public AttackState(RiskGame game) {
        this.game = game;
    }

    @Override
    public BattleResult attack(Country attacker, Country defender) {
        Player currentPlayer = game.getCurrentPlayer();

        if (attacker.getOwner() != currentPlayer) return null;
        if (defender.getOwner() == currentPlayer) return null;

        if (!attacker.getNeighbors().contains(defender)) return null;
        if (attacker.getArmies() <= 1) return null;
        return game.getCombatManager().resolveAttack(attacker, defender);
    }

    @Override
    public GameState nextPhase() {
        return new FortifyState(game);
    }

    @Override
    public Set<Country> getValidTargets(Country source) {
        Set<Country> enemyCountries = new HashSet<Country>();
        for(Country c : source.getNeighbors())
        {
            if(c.getOwner() != game.getCurrentPlayer())
                enemyCountries.add(c);
        }
        return enemyCountries;
    }

    @Override
    public String getPhaseName() {
        return "ATTACK";
    }
}