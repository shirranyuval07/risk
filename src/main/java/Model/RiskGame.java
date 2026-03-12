package Model;

import Model.Records.BattleResult;
import Model.States.DraftState;
import Model.States.GameState;
import lombok.Getter;

import java.util.*;

public class RiskGame {
    // --- Getters ---
    @Getter
    private final Board board;
    @Getter
    private final List<Player> players;
    private int currentPlayerIndex;

    // *** שימוש במכונת מצבים במקום ב-Enum ***
    @Getter
    private GameState currentState;

    @Getter
    private final Dice dice;
    private final List<GameObserver> observers; // רשימת מאזינים לעדכון התצוגה

    public RiskGame()
    {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.dice = new Dice();
        this.observers = new ArrayList<>();
    }

    // הגדרת המצב הנוכחי של המשחק
    public void setCurrentState(GameState state)
    {
        this.currentState = state;
        notifyObservers();
    }

    // --- ניהול שחקנים ושלבים ---
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
        // 1. קודם כל: בדיקת ניצחון! נספור כמה שחקנים פעילים נשארו
        long activePlayersCount = players.stream()
                .filter(p -> !p.getOwnedCountries().isEmpty())
                .count();

        // אם נשאר רק שחקן אחד עם מדינות, המשחק נגמר והוא המנצח
        if (activePlayersCount <= 1) {
            System.out.println("Game Over! We have a winner!");
            // TODO: כאן תוכל לקרוא לפונקציה שמציגה את מסך הניצחון ב-JavaFX
            return;
        }

        // 2. קידום התור לשחקן הבא שעדיין בחיים
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            // הלולאה תמשיך לקדם את האינדקס כל עוד השחקן הבא הוא ללא מדינות (מודח)
        } while (players.get(currentPlayerIndex).getOwnedCountries().isEmpty());

        // 3. כעת מובטח שהאינדקס מצביע על שחקן פעיל שיש לו מדינות
        startTurn();
    }

    private void startTurn()
    {
        Player p = getCurrentPlayer();
        int reinforcement = Math.max(3, p.getOwnedCountries().size() / 3);
        reinforcement += board.calculateContinentBonus(p);

        p.setDraftArmies(reinforcement);
        System.out.println("It's " + p.getName() + "'s turn. Reinforcements: " + reinforcement);

        // בתחילת התור, המערכת נכנסת אוטומטית למצב הצבת הכוחות (DRAFT)
        setCurrentState(new DraftState(this));
    }

    // --- האצלת הפעולות (Delegation) למכונת המצבים בסיבוכיות O(1) ---

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

    // --- פונקציות עזר עבור מחלקות המצב (States) ---

    public void handleConquest(Country attacker, Country defender, int moveAmount)
    {
        Player oldOwner = defender.getOwner();
        Player newOwner = attacker.getOwner();

        oldOwner.removeCountry(defender);
        newOwner.addCountry(defender);

        attacker.removeArmies(moveAmount);
        defender.addArmies(moveAmount);
    }

    // --- מנגנון ה-Observer (לעדכון התצוגה, חלק מ-MVC) ---
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