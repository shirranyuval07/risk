package Model.States;

import Model.Country;

public interface GameState {
    boolean placeArmy(Country country);
    String attack(Country attacker, Country defender);
    String fortify(Country from, Country to, int amount);
    void nextPhase();

    String getPhaseName();
}
