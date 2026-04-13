package com.example.demo.model.States;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import com.example.demo.model.Records.GameRecords.BattleResult; // שים לב שהנתיב מעודכן לשלב 1
import com.example.demo.model.manager.RiskGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

// Sealed interface - אנחנו מצהירים מראש מי מממש אותו
public sealed interface GameState permits
        GameState.SetupState,
        GameState.DraftState,
        GameState.AttackState,
        GameState.FortifyState {

    // התנהגויות ברירת מחדל
    default boolean placeArmy(Country country) { return false; }
    default BattleResult attack(Country attacker, Country defender) { return null; }
    default String fortify(Country from, Country to, int amount) {
        return "Wrong phase! You are currently in the " + getPhaseName() + " phase.";
    }

    /**
     * עוזר משותף לשלבי Setup ו-Draft: בודק אם המדינה שייכת לשחקן הנוכחי
     * ואם יש לו חיילים להניח, ואם כן מוסיף חייל אחד ומפחית את המכסה.
     * @param country המדינה שבה רוצים להציב חייל
     * @param game מופע המשחק
     * @return true אם ההצבה הצליחה, false אחרת
     */
    default boolean tryPlaceArmy(Country country, RiskGame game) {
        Player currentPlayer = game.getCurrentPlayer();
        if (country.getOwner() != currentPlayer) return false;
        if (currentPlayer.getDraftArmies() <= 0) return false;

        country.addArmies(1);
        currentPlayer.decreaseDraftArmies();
        return true;
    }

    GameState nextPhase();
    Set<Country> getValidTargets(Country source);
    String getPhaseName();

    // ==========================================
    // כאן מתחילים המימושים (Records)
    // ==========================================

    record SetupState(RiskGame game) implements GameState {
        @Override
        public boolean placeArmy(Country country) {
            if (!tryPlaceArmy(country, game)) return false;

            boolean allArmiesPlaced = game.getPlayers().stream()
                    .allMatch(p -> p.getDraftArmies() <= 0);

            if (allArmiesPlaced) {
                game.setCurrentState(nextPhase());
            } else {
                game.advanceSetupTurn();
            }
            return true;
        }

        @Override
        public GameState nextPhase() {
            game.nextTurn();
            return new DraftState(game);
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

    record DraftState(RiskGame game) implements GameState {
        private static final Logger log = LoggerFactory.getLogger(DraftState.class);

        @Override
        public boolean placeArmy(Country country) {
            return tryPlaceArmy(country, game);
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

    record AttackState(RiskGame game) implements GameState {
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
            Set<Country> enemyCountries = new HashSet<>();
            for(Country c : source.getNeighbors()) {
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

    record FortifyState(RiskGame game) implements GameState {
        @Override
        public String fortify(Country from, Country to, int amount) {
            Player currentPlayer = game.getCurrentPlayer();
            if (from.getOwner() != currentPlayer || to.getOwner() != currentPlayer) return "Must own both!";
            if (from.getArmies() - amount < 1) return "Must leave at least 1 army behind!";

            from.removeArmies(amount);
            to.addArmies(amount);
            game.setCurrentState(nextPhase());
            return "Moved " + amount + " armies successfully.";
        }

        @Override
        public GameState nextPhase() {
            game.nextTurn();
            return new DraftState(game);
        }

        @Override
        public Set<Country> getValidTargets(Country source) {
            Set<Country> reachable = AIGraphAnalyzer.bfsReachableOwned(source, game.getCurrentPlayer());
            reachable.remove(source);
            return reachable;
        }

        @Override
        public String getPhaseName() {
            return "FORTIFY";
        }
    }
}