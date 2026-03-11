package Model.States;

import Model.Country;
import Model.Records.BattleResult;

import java.util.Set;

public interface GameState {
    boolean placeArmy(Country country);
    BattleResult attack(Country attacker, Country defender);
    String fortify(Country from, Country to, int amount);
    void nextPhase();
    Set<Country> getValidTargets(Country source);
    String getPhaseName();
}
