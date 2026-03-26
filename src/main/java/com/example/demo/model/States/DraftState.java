package com.example.demo.model.States;

import com.example.demo.model.Country;
import com.example.demo.model.Player;
import com.example.demo.model.Records.BattleResult;
import com.example.demo.model.RiskGame;
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

        return true;
    }

    @Override
    public BattleResult attack(Country attacker, Country defender) {
        return null;
    }


    @Override
    public GameState nextPhase() {
        if (game.getCurrentPlayer().getDraftArmies() > 0) {
            log.warn("Cannot advance: You must place all draft armies.");
            return null;
        }

        return new AttackState(game);
    }

    @Override
    public Set<Country> getValidTargets(Country source) {
        return new HashSet<>();
    }

    @Override
    public String getPhaseName() {
        return "DRAFT";
    }
}