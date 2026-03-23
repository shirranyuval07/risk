package Model.States;

import Model.Country;
import Model.Records.BattleResult;

import java.util.Set;

public interface GameState {
    // Default behaviors for invalid phases
    default boolean placeArmy(Country country) { return false; }
    default BattleResult attack(Country attacker, Country defender) { return null; }
    default String fortify(Country from, Country to, int amount) {
        return "Wrong phase! You are currently in the " + getPhaseName() + " phase.";
    }

    void nextPhase();
    Set<Country> getValidTargets(Country source);
    String getPhaseName();
}