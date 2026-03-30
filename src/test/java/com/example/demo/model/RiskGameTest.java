package com.example.demo.model;

import com.example.demo.model.States.GameState;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import com.example.demo.model.manager.RiskGame;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiskGameTest {

    @Test
    void testNextTurnGameOver() {
        RiskGame game = new RiskGame();
        Player p1 = new Player("P1", Color.RED, false);
        Player p2 = new Player("P2", Color.BLUE, false);
        game.addPlayer(p1);
        game.addPlayer(p2);

        // Mock countries
        Country c1 = new Country(1, "C1", 0, 0);
        Country c2 = new Country(2, "C2", 1, 1);
        
        p1.addCountry(c1);
        p2.addCountry(c2);
        
        // p1 is current player
        // game.currentPlayerIndex is 0
        
        // Simulate p1's turn
        game.setCurrentState(new GameState.FortifyState(game));
        
        // p2 loses all countries during p1's turn
        p2.getOwnedCountries().clear();
        c2.setOwner(p1);
        p1.getOwnedCountries().add(c2);

        assertFalse(game.isGameOver());

        // Now p1 ends their turn. 
        // In the original code, nextTurn() checks gameOver BEFORE moving to p2.
        // It sees 1 active player and returns, setting gameOver = true.
        // This is what the user said "doesn't work" - maybe because it ends too early 
        // or they wanted it to move to the next player first?
        // Actually, if only 1 player is left, the game SHOULD end.
        
        game.nextTurn();

        assertTrue(game.isGameOver(), "Game should be over when only one player has countries");
    }

    @Test
    void testNextTurnGameOverFailsWhenCheckedTooEarly() {
        // This test demonstrates the issue where checking too early might be wrong 
        // if we are in a state where we SHOULD move to next turn but someone just got eliminated.
        
        RiskGame game = new RiskGame();
        Player p1 = new Player("P1", Color.RED, false);
        Player p2 = new Player("P2", Color.BLUE, false);
        Player p3 = new Player("P3", Color.GREEN, false);
        game.addPlayer(p1);
        game.addPlayer(p2);
        game.addPlayer(p3);

        Country c1 = new Country(1, "C1", 0, 0);
        Country c2 = new Country(2, "C2", 1, 1);
        Country c3 = new Country(3, "C3", 2, 2);
        
        p1.addCountry(c1);
        p2.addCountry(c2);
        p3.addCountry(c3);

        // p1's turn
        game.setCurrentState(new GameState.FortifyState(game));

        // p2 is eliminated by p1
        p2.getOwnedCountries().clear();
        c2.setOwner(p1);
        p1.getOwnedCountries().add(c2);

        game.nextTurn();

        assertFalse(game.isGameOver());
        assertEquals(p3, game.getCurrentPlayer(), "Should skip p2 and move to p3");
    }
}
