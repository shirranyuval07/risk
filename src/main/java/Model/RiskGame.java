package Model;

import Model.Records.BattleResult;
import Model.States.DraftState;
import Model.States.GameState;
import Model.States.SetupState;
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
    private final Dice dice;


    @Getter
    private boolean gameOver = false;

    @Getter @Setter
    private long gameSeed = 0;

    public RiskGame() {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.dice = new Dice();
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
        Player p = getCurrentPlayer();
        currentPlayerProperty.set(p); // MAGIC: This tells the UI the player changed!

        int reinforcement = Math.max(3, p.getOwnedCountries().size() / 3);
        reinforcement += board.calculateContinentBonus(p);

        p.setDraftArmies(reinforcement);
        log.info("It's {}'s turn. Reinforcements: {}", p.getName(), reinforcement);

        setCurrentState(new DraftState(this));
    }

    public boolean placeArmy(Country country) {
        return getCurrentState().placeArmy(country);
    }

    public BattleResult attack(Country attacker, Country defender) {
        return getCurrentState().attack(attacker, defender);
    }

    public String fortify(Country from, Country to, int amount) {
        return getCurrentState().fortify(from, to, amount);
    }

    public void nextPhase() {
        getCurrentState().nextPhase();
    }

    public void handleConquest(Country attacker, Country defender, int moveAmount) {
        Player oldOwner = defender.getOwner();
        Player newOwner = attacker.getOwner();

        oldOwner.removeCountry(defender);
        newOwner.addCountry(defender);

        attacker.removeArmies(moveAmount);
        defender.addArmies(moveAmount);
        newOwner.setConqueredThisTurn(true);
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
}