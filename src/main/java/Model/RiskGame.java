package Model;

import lombok.Getter;

import java.util.*;

public class RiskGame {
    // --- Getters ---
    @Getter
    private final Board board;
    @Getter
    private final List<Player> players;
    private int currentPlayerIndex;
    @Getter
    private Phase currentPhase;
    private final Dice dice;
    private final List<GameObserver> observers; // רשימת מאזינים לעדכון התצוגה

    public RiskGame() {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.currentPhase = Phase.DRAFT;
        this.dice = new Dice();
        this.observers = new ArrayList<>();
    }

    // --- ניהול שחקנים ושלבים ---
    public void addPlayer(Player p) {
        players.add(p);
    }

    public void startGame() {
        if (players.isEmpty()) return;
        initializeSetup();
        startTurn();
        notifyObservers();
    }

    public void initializeSetup() {
        // חלוקה אוטומטית של כל המדינות במפה בין השחקנים
        List<Country> allCountries = new ArrayList<>(board.getCountries());
        Collections.shuffle(allCountries);

        for (int i = 0; i < allCountries.size(); i++) {
            Player p = players.get(i % players.size());
            Country c = allCountries.get(i);
            p.addCountry(c);
            c.addArmies(1); // חייל ראשון לכל מדינה כבושה
        }

        // הגדרת חיילי SETUP התחלתיים להצבה (לפי חוקי ריסק)
        int setupArmies = (players.size() == 2) ? 40 : 35; // דוגמה ל-2 או 3 שחקנים
        for (Player p : players) {
            p.setDraftArmies(setupArmies - p.getOwnedCountries().size());
        }
    }

    public void nextPhase() {
        // מניעת מעבר שלב אם לא הוצבו כל החיילים בשלב ה-DRAFT
        if (currentPhase == Phase.DRAFT && getCurrentPlayer().getDraftArmies() > 0) {
            return;
        }

        switch (currentPhase) {
            case DRAFT:
                currentPhase = Phase.ATTACK;
                break;
            case ATTACK:
                currentPhase = Phase.FORTIFY;
                break;
            case FORTIFY:
                currentPhase = Phase.DRAFT;
                nextTurn();
                break;
        }
        notifyObservers();
    }

    private void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        startTurn();
    }

    private void startTurn() {
        Player p = getCurrentPlayer();
        // חישוב תגבורת: כמות מדינות חלקי 3 (מינימום 3) + בונוס יבשות
        int reinforcement = Math.max(3, p.getOwnedCountries().size() / 3);
        reinforcement += board.calculateContinentBonus(p);

        p.setDraftArmies(reinforcement);
        System.out.println("It's " + p.getName() + "'s turn. Reinforcements: " + reinforcement);
    }

    // --- לוגיקת פעולות (Draft, Attack, Fortify) ---

    public boolean placeArmy(Country country) {
        if (currentPhase != Phase.DRAFT || country.getOwner() != getCurrentPlayer()) return false;
        if (getCurrentPlayer().getDraftArmies() <= 0) return false;

        country.addArmies(1);
        getCurrentPlayer().decreaseDraftArmies();
        notifyObservers();
        return true;
    }

    public String attack(Country attacker, Country defender) {
        // ולידציות בסיסיות
        if (currentPhase != Phase.ATTACK) return "Wrong phase!";
        if (attacker.getOwner() != getCurrentPlayer()) return "Not your country!";
        if (defender.getOwner() == getCurrentPlayer()) return "Can't attack yourself!";
        if (!attacker.getNeighbors().contains(defender)) return "Not a neighbor!";
        if (attacker.getArmies() <= 1) return "Need more than 1 army to attack!";

        // הטלת קוביות (עד 3 לתוקף, עד 2 למגן)
        int aDiceCount = Math.min(3, attacker.getArmies() - 1);
        int dDiceCount = Math.min(2, defender.getArmies());

        Integer[] aRolls = dice.roll(aDiceCount);
        Integer[] dRolls = dice.roll(dDiceCount);

        // השוואת קוביות (חוקי ריסק: משווים את התוצאות הכי גבוהות)
        int comparisons = Math.min(aDiceCount, dDiceCount);
        int aLoss = 0, dLoss = 0;

        for (int i = 0; i < comparisons; i++) {
            if (aRolls[i] > dRolls[i]) dLoss++;
            else aLoss++;
        }

        attacker.removeArmies(aLoss);
        defender.removeArmies(dLoss);

        String result = String.format("Attack Result: Attacker lost %d, Defender lost %d", aLoss, dLoss);

        // בדיקת כיבוש
        if (defender.getArmies() == 0) {
            result += " | COUNTRY CONQUERED!";
            handleConquest(attacker, defender, aDiceCount);
        }

        notifyObservers();
        return result;
    }

    private void handleConquest(Country attacker, Country defender, int moveAmount) {
        Player oldOwner = defender.getOwner();
        Player newOwner = attacker.getOwner();

        oldOwner.removeCountry(defender);
        newOwner.addCountry(defender);

        // העברת חיילים מינימלית (לפי כמות הקוביות שהוטלו)
        attacker.removeArmies(moveAmount);
        defender.addArmies(moveAmount);
    }

    public String fortify(Country from, Country to, int amount) {
        if (currentPhase != Phase.FORTIFY) return "Wrong phase!";
        if (from.getOwner() != getCurrentPlayer() || to.getOwner() != getCurrentPlayer()) return "Must own both!";
        if (!from.getNeighbors().contains(to)) return "Countries must be neighbors!";
        if (from.getArmies() - amount < 1) return "Must leave at least 1 army!";

        from.removeArmies(amount);
        to.addArmies(amount);

        notifyObservers();
        return "Moved " + amount + " armies successfully.";
    }

    // --- מנגנון ה-Observer (לעדכון התצוגה) ---
    public interface GameObserver {
        void onGameUpdate();
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (GameObserver obs : observers) {
            obs.onGameUpdate();
        }
    }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
}