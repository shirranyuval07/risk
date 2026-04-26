package com.example.demo.model.manager;

import com.example.demo.model.States.GameState.SetupState;
import com.example.demo.config.GameConstants;
import com.example.demo.model.States.GameState;
import com.example.demo.model.Records.GameRecords.BattleResult;
import com.example.demo.model.util.Card;
import com.example.demo.model.util.Dice;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class RiskGame
{
    // --- משתני ליבה של המשחק ---
    @Getter private final Board board;
    @Getter private final List<Player> players;
    @Getter private final Dice dice;
    @Getter private final CombatManager combatManager;

    // --- ניהול תורים ומצבים ---
    private int currentPlayerIndex;
    @Getter @Setter private long gameSeed = 0;
    @Getter private boolean gameOver = false;

    // --- Data Binding (משתנים חכמים עבור ה-UI) ---
    private final ObjectProperty<GameState> currentState = new SimpleObjectProperty<>();
    private final ObjectProperty<Player> currentPlayerProperty = new SimpleObjectProperty<>();

    // --- מאזינים ל-UI ---
    private final List<GameUpdateListener> listeners = new ArrayList<>();


    // 1. בנאי (Constructor) והגדרות ראשוניות


    public RiskGame() {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.dice = new Dice();
        this.combatManager = new CombatManager();
        this.currentPlayerIndex = 0;
    }

    public void addPlayer(Player p) {
        players.add(p);
    }


    // 2. ניהול זרימת המשחק (Game Loop)


    public void startGame() {
        notifyGameMessage("Game started");
        if (players.isEmpty()) return;

        initializeSetup();
        currentPlayerIndex = 0;
        currentPlayerProperty.set(players.getFirst());
        setCurrentState(new SetupState(this));
    }

    private void initializeSetup() {
        List<Country> allCountries = new ArrayList<>(board.getCountries());

        // ערבוב המדינות (עם Seed במשחק רשת, או אקראי רגיל במקומי)
        if (gameSeed != 0) Collections.shuffle(allCountries, new Random(gameSeed));
        else               Collections.shuffle(allCountries);

        // חלוקת מדינות ראשונית
        for (int i = 0; i < allCountries.size(); i++) {
            Player p = players.get(i % players.size());
            Country c = allCountries.get(i);
            p.addCountry(c);
            c.addArmies(1);
        }

        // חישוב חיילי בסיס שנותרו להצבה בשלב ההכנה
        int totalStartingArmies = GameConstants.STARTING_ARMIES - (players.size() * GameConstants.MAX_PLAYERS);
        for (Player p : players) {
            p.setDraftArmies(totalStartingArmies - p.getOwnedCountries().size());
        }
    }

    public void advanceSetupTurn() {
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

    public void nextTurn() {
        notifyGameMessage("Moved to next turn");

        Player endingPlayer = getCurrentPlayer();

        // חלוקת קלף בונוס אם השחקן כבש העונה
        if (endingPlayer != null && endingPlayer.isConqueredThisTurn()) {
            endingPlayer.addCard(Card.getRandom());
            endingPlayer.setConqueredThisTurn(false);
            log.info("{} received a bonus card!", endingPlayer.getName());
        }

        // בדיקת ניצחון
        long activePlayersCount = players.stream().filter(p -> !p.getOwnedCountries().isEmpty()).count();
        if (activePlayersCount <= 1) {
            this.gameOver = true;
            String winnerName = (endingPlayer != null) ? endingPlayer.getName() : "Somebody";
            log.info("Game Over! We have a winner!");
            notifyGameMessage("Game Over! " + winnerName + " wins!");
            notifyStatsUpdated();
            return;
        }

        // העברת תור לשחקן החי הבא
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).getOwnedCountries().isEmpty());

        startTurn();
    }

    private void startTurn() {
        notifyGameMessage("Turn started");
        Player p = getCurrentPlayer();
        currentPlayerProperty.set(p);

        // חישוב תגבורת
        int reinforcement = Math.max(GameConstants.MIN_REINFORCEMENT, p.getOwnedCountries().size() / GameConstants.REINFORCEMENT_DIVISOR);
        reinforcement += board.calculateContinentBonus(p);

        p.setDraftArmies(reinforcement);
        log.info("It's {}'s turn. Reinforcements: {}", p.getName(), reinforcement);
    }

    public void nextPhase() {
        GameState next = getCurrentState().nextPhase();
        if (next != null)
            setCurrentState(next);

    }


    // 3. פעולות שחקן (Actions)


    public boolean placeArmy(Country country) {
        boolean success = getCurrentState().placeArmy(country);
        if (success) notifyStatsUpdated();
        return success;
    }

    public BattleResult attack(Country attacker, Country defender) {
        BattleResult result = getCurrentState().attack(attacker, defender);
        notifyStatsUpdated();
        return result;
    }

    public String fortify(Country from, Country to, int amount) {
        String result = getCurrentState().fortify(from, to, amount);
        notifyStatsUpdated();
        return result;
    }

    public void handleConquest(Country attacker, Country defender, int moveAmount) {
        combatManager.executeConquest(attacker, defender, moveAmount);
        notifyStatsUpdated();
    }


    // 4. Getters, Setters & Listeners


    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public GameState getCurrentState() { return currentState.get(); }
    public void setCurrentState(GameState state) { this.currentState.set(state); }
    public ObjectProperty<GameState> currentStateProperty() { return currentState; }
    public ObjectProperty<Player> currentPlayerProperty() { return currentPlayerProperty; }

    public void addGameUpdateListener(GameUpdateListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(Consumer<GameUpdateListener> action) {
        for (GameUpdateListener listener : listeners) action.accept(listener);
    }

    public void notifyStatsUpdated() {
        notifyListeners(GameUpdateListener::onStatsUpdated);
    }

    public void notifyGameMessage(String message) {
        notifyListeners(l -> l.onGameMessage(message));
    }
}