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

        int aDiceCount = Math.min(3, attacker.getArmies() - 1);
        int dDiceCount = Math.min(2, defender.getArmies());

        Integer[] aRolls = Dice.roll(aDiceCount);
        Integer[] dRolls = Dice.roll(dDiceCount);

        int comparisons = Math.min(aDiceCount, dDiceCount);
        int aLoss = 0, dLoss = 0;

        Arrays.sort(aRolls, Collections.reverseOrder());
        Arrays.sort(dRolls, Collections.reverseOrder());

        for (int i = 0; i < comparisons; i++) {
            if (aRolls[i] > dRolls[i]) dLoss++;
            else aLoss++;
        }

        attacker.removeArmies(aLoss);
        defender.removeArmies(dLoss);

        String result = String.format("Attack Result: Attacker lost %d, Defender lost %d", aLoss, dLoss);
        int minMove = aDiceCount; // המינימום הוא כמות הקוביות שהתוקף הטיל
        int maxMove = attacker.getArmies() - 1; // המקסימום הוא כל החיילים פחות 1
        boolean isConquered = false;
        if (defender.getArmies() == 0) {
            result += " | COUNTRY CONQUERED!";
            isConquered = true;
        }

        log.info(result);
        return new BattleResult(aRolls,dRolls,aLoss,dLoss,isConquered,minMove,maxMove);
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