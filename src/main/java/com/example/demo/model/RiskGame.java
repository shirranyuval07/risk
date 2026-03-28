package com.example.demo.model;

import com.example.demo.model.States.GameState.SetupState;




import com.example.demo.model.States.GameState;
import com.example.demo.model.Records.GameRecords.BattleResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class RiskGame {
    @Getter
    private final Board board;
    @Getter
    private final List<Player> players;
    private int currentPlayerIndex;

    // --- DATA BINDING PROPERTIES ---
    private final ObjectProperty<GameState> currentState = new SimpleObjectProperty<>();
    private final ObjectProperty<Player> currentPlayerProperty = new SimpleObjectProperty<>();

    @Getter
    private final Dice dice= new Dice();


    @Getter
    private boolean gameOver = false;

    @Getter @Setter
    private long gameSeed = 0;
    @Getter
    private final CombatManager combatManager= new CombatManager();

    // למעלה יחד עם שאר המשתנים של RiskGame
    private final List<GameUpdateListener> listeners = new ArrayList<>();

    public RiskGame() {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
    }

    // --- PROPERTY GETTERS ---
    public ObjectProperty<GameState> currentStateProperty() { return currentState; }
    public ObjectProperty<Player> currentPlayerProperty() { return currentPlayerProperty; }

    public GameState getCurrentState() { return currentState.get(); }
    public void setCurrentState(GameState state) {
        this.currentState.set(state);

    }

    public void addPlayer(Player p) {
        players.add(p);
    }

    public void startGame() {
        notifyGameMessage("Game started");
        if (players.isEmpty()) return;
        initializeSetup();

        // Start in the Setup Phase instead of a normal turn
        currentPlayerIndex = 0;
        currentPlayerProperty.set(players.getFirst());
        setCurrentState(new SetupState(this));
    }

    public void initializeSetup() {
        List<Country> allCountries = new ArrayList<>(board.getCountries());
        if (gameSeed != 0)
        {
            Collections.shuffle(allCountries, new java.util.Random(gameSeed));
        }
        else
        {
            // אם לא (למשל במשחק מקומי), נערבב כרגיל
            Collections.shuffle(allCountries);
        }

        for (int i = 0; i < allCountries.size(); i++) {
            Player p = players.get(i % players.size());
            Country c = allCountries.get(i);
            p.addCountry(c);
            c.addArmies(1);
        }

        int setupArmies = 50 - (players.size() * 5);

        for (Player p : players)
        {
            p.setDraftArmies(setupArmies - p.getOwnedCountries().size());
        }
    }

    public void nextTurn() {
        notifyGameMessage("moved to next turn");
        long activePlayersCount = players.stream()
                .filter(p -> !p.getOwnedCountries().isEmpty())
                .count();

        if (activePlayersCount <= 1) {
            this.gameOver = true;
            log.info("Game Over! We have a winner!");
            return;
        }

        Player endingPlayer = getCurrentPlayer();
        if (endingPlayer != null && endingPlayer.isConqueredThisTurn()) {
            endingPlayer.addCard(Card.getRandom()); // חלוקת קלף רנדומלי
            endingPlayer.setConqueredThisTurn(false); // איפוס הדגל לקראת התור הבא שלו
            log.info("{} received a bonus card for conquering a territory!", endingPlayer.getName());
        }

        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).getOwnedCountries().isEmpty());

        startTurn();
    }

    private void startTurn() {
        notifyGameMessage("turn started");
        Player p = getCurrentPlayer();
        currentPlayerProperty.set(p);

        int reinforcement = Math.max(3, p.getOwnedCountries().size() / 3);
        reinforcement += board.calculateContinentBonus(p);

        p.setDraftArmies(reinforcement);
        log.info("It's {}'s turn. Reinforcements: {}", p.getName(), reinforcement);

    }

    public boolean placeArmy(Country country) {
        boolean placeArmyComplete = getCurrentState().placeArmy(country);
        if(placeArmyComplete) {
            notifyStatsUpdated();
        }
        return placeArmyComplete;
    }

    public BattleResult attack(Country attacker, Country defender) {
        BattleResult attackResult = getCurrentState().attack(attacker, defender);
        notifyStatsUpdated();
        return attackResult;
    }

    public String fortify(Country from, Country to, int amount) {
        String fortifyResult =  getCurrentState().fortify(from, to, amount);
        notifyStatsUpdated();
        return fortifyResult;
    }

    public void nextPhase() {
        GameState next = getCurrentState().nextPhase();
        if (next != null) {
            setCurrentState(next); // מחליף סטייט רק אם באמת קיבלנו אישור לעבור שלב
        }
    }

    public void handleConquest(Country attacker, Country defender, int moveAmount) {
        this.combatManager.executeConquest(attacker, defender, moveAmount);
        notifyStatsUpdated();
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public void advanceSetupTurn() {
        // Cycle to the next player who still has setup armies left to place
        int attempts = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            attempts++;
            if (attempts > players.size()) {
                log.error("CRITICAL: No valid players found for next turn!");
                this.gameOver = true;
                return;
            }
        } while (players.get(currentPlayerIndex).getOwnedCountries().isEmpty());

        currentPlayerProperty.set(players.get(currentPlayerIndex));
    }

    // מאפשר ל-UI להירשם כמאזין
    public void addGameUpdateListener(GameUpdateListener listener) {
        listeners.add(listener);
    }

    // פונקציות עזר שהמודל יפעיל כדי "לצעוק" למאזינים
    public void notifyStatsUpdated() {
        for (GameUpdateListener listener : listeners) {
            listener.onStatsUpdated();
        }
    }

    public void notifyGameMessage(String message) {
        for (GameUpdateListener listener : listeners) {
            listener.onGameMessage(message);
        }
    }
}