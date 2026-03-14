package Model;

import Model.Records.BattleResult;
import Model.States.DraftState;
import Model.States.GameState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class RiskGame {
    @Getter
    private final Board board;
    @Getter
    private final List<Player> players;
    private int currentPlayerIndex;

    @Getter
    private GameState currentState;

    @Getter
    private final Dice dice;
    private final List<GameObserver> observers;

    @Getter
    private boolean gameOver = false;


    public RiskGame()
    {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.dice = new Dice();
        this.observers = new ArrayList<>();
    }

    public void setCurrentState(GameState state)
    {
        this.currentState = state;
        notifyObservers();
    }

    public void addPlayer(Player p) {
        players.add(p);
    }

    public void startGame()
    {
        if (players.isEmpty()) return;
        initializeSetup();
        startTurn();
        notifyObservers();
    }

    public void initializeSetup()
    {
        List<Country> allCountries = new ArrayList<>(board.getCountries());
        Collections.shuffle(allCountries);

        for (int i = 0; i < allCountries.size(); i++) {
            Player p = players.get(i % players.size());
            Country c = allCountries.get(i);
            p.addCountry(c);
            c.addArmies(1);
        }

        int setupArmies = (players.size() == 2) ? 40 : 35;
        for (Player p : players) {
            p.setDraftArmies(setupArmies - p.getOwnedCountries().size());
        }
    }

    public void nextTurn() {
        long activePlayersCount = players.stream()
                .filter(p -> !p.getOwnedCountries().isEmpty())
                .count();

        if (activePlayersCount <= 1) {
            this.gameOver = true; // סימון שהמשחק נגמר
            log.info("Game Over! We have a winner!");
            notifyObservers(); // קריטי! זה מה שמעדכן את מפת המסך בפעם האחרונה
            return;
        }

        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).getOwnedCountries().isEmpty());

        startTurn();
    }

    private void startTurn()
    {
        Player p = getCurrentPlayer();
        int reinforcement = Math.max(3, p.getOwnedCountries().size() / 3);
        reinforcement += board.calculateContinentBonus(p);

        p.setDraftArmies(reinforcement);
        log.info("It's {}'s turn. Reinforcements: {}", p.getName(), reinforcement);

        setCurrentState(new DraftState(this));
    }

    public boolean placeArmy(Country country)
    {
        return currentState.placeArmy(country);
    }

    public BattleResult attack(Country attacker, Country defender)
    {
        return currentState.attack(attacker, defender);
    }

    public String fortify(Country from, Country to, int amount) {
        return currentState.fortify(from, to, amount);
    }

    public void nextPhase() {
        currentState.nextPhase();
    }

    public void handleConquest(Country attacker, Country defender, int moveAmount)
    {
        Player oldOwner = defender.getOwner();
        Player newOwner = attacker.getOwner();

        oldOwner.removeCountry(defender);
        newOwner.addCountry(defender);

        attacker.removeArmies(moveAmount);
        defender.addArmies(moveAmount);
    }


    public interface GameObserver
    {
        void onGameUpdate();
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers()
    {
        for (GameObserver obs : observers)
        {
            obs.onGameUpdate();
        }
    }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
}