package com.example.demo.model.manager;

import com.example.demo.model.States.GameState.SetupState;
import com.example.demo.config.GameConstants;
import com.example.demo.model.States.GameState;
import com.example.demo.model.Records.GameRecords.BattleResult;
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
    @Getter
    private final Board board;
    @Getter
    private final List<Player> players;
    private int currentPlayerIndex;

    private final ObjectProperty<GameState> currentState = new SimpleObjectProperty<>();
    public ObjectProperty<GameState> currentStateProperty() { return currentState; }
    private final ObjectProperty<Player> currentPlayerProperty = new SimpleObjectProperty<>();
    public ObjectProperty<Player> currentPlayerProperty() { return currentPlayerProperty; }
    public GameState getCurrentState() { return currentState.get(); }
    public void setCurrentState(GameState state) {
        this.currentState.set(state);

    }
    @Getter
    private final Dice dice= new Dice();


    @Getter
    private boolean gameOver = false;

    @Getter @Setter
    private long gameSeed = 0;
    @Getter
    private final CombatManager combatManager= new CombatManager();

    private final List<GameUpdateListener> listeners = new ArrayList<>();

    public RiskGame() {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
    }

    public void addPlayer(Player p) {
        players.add(p);
    }
    /**
     * טענת יציאה: הפונקציה אחראית על להתחיל את המשחק. היא מערבבת את הלוח, מגדירה את השחקן המתחיל, מגדירה את המצב משחק.
     * */
    public void startGame() {
        notifyGameMessage("Game started");
        if (players.isEmpty()) return;
        initializeSetup();

        currentPlayerIndex = 0;
        currentPlayerProperty.set(players.getFirst());
        setCurrentState(new SetupState(this));
    }
    /**
     *הפונקציה מערבבת את המדינות לפי סיד מוגדר ורנדומלי במשחק רב משתתפים אחרת רנדומלית לגמרי.
     * לאחר מכן מביאה לכל שחקן את כמות החיילים שאיתו מתחיל.
     * */
    public void initializeSetup() {
        List<Country> allCountries = new ArrayList<>(board.getCountries());
        if (gameSeed != 0)
            Collections.shuffle(allCountries, new Random(gameSeed));
        else
            Collections.shuffle(allCountries);

        for (int i = 0; i < allCountries.size(); i++)
        {
            Player p = players.get(i % players.size());
            Country c = allCountries.get(i);
            p.addCountry(c);
            c.addArmies(1);
        }
        int setupArmies = GameConstants.STARTING_ARMIES - (players.size() * GameConstants.MAX_PLAYERS);

        for (Player p : players)
            p.setDraftArmies(setupArmies - p.getOwnedCountries().size());
    }
    /**
     * טענת יציאה: הפונקציה אחראית לעבור לשלב הבא של התור. אם שחקן השתלט על מדינה הוא מקבל קלף רנדומלי. אם נגמר המשחק אז מעדכנים שמישהו ניצח ושgameover = true.
     * לאחר מכן כל עוד לשחקן הבא עדיין יש מדינות עוברים לתור שלו ומתחילים את התור שלו.
     * */
    public void nextTurn() {
        notifyGameMessage("moved to next turn");

        Player endingPlayer = getCurrentPlayer();
        if (endingPlayer != null && endingPlayer.isConqueredThisTurn())
        {
            endingPlayer.addCard(Card.getRandom()); // חלוקת קלף רנדומלי
            endingPlayer.setConqueredThisTurn(false); // איפוס הדגל לקראת התור הבא שלו
            log.info("{} received a bonus card for conquering a territory!", endingPlayer.getName());
        }

        long activePlayersCount = players.stream()
                .filter(p -> !p.getOwnedCountries().isEmpty())
                .count();

        if (activePlayersCount <= 1)
        {
            this.gameOver = true;
            log.info("Game Over! We have a winner!");
            notifyGameMessage("Game Over! " + (endingPlayer != null ? endingPlayer.getName() : "Somebody") + " wins!");
            notifyStatsUpdated();
            return;
        }

        do
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        while (players.get(currentPlayerIndex).getOwnedCountries().isEmpty());
        startTurn();
    }
    /**
     * טענת יציאה: מעדכנים את השחקן הנוכחי להיות השחקן שהרגע התחיל התור שלו, מחשבים את כמות הdraft armies שיש לו להציב ומציגים שזה תורו.
     * */
    private void startTurn() {
        notifyGameMessage("turn started");
        Player p = getCurrentPlayer();
        currentPlayerProperty.set(p);

        int reinforcement = Math.max(GameConstants.MIN_REINFORCEMENT, p.getOwnedCountries().size() / GameConstants.REINFORCEMENT_DIVISOR);
        reinforcement += board.calculateContinentBonus(p);

        p.setDraftArmies(reinforcement);
        log.info("It's {}'s turn. Reinforcements: {}", p.getName(), reinforcement);

    }
    /**
     * @param country המדינה בה רוצים להציב חייל
     * טענת יציאה: הפונקציה מנסה להציב חייל במדינה שבחרנו. אם זה הצליח (כלומר היה אפשרי לפי החוקים) אז מעדכנים את הסטטיסטיקות בממשק.
     *                הפונקציה מחזירה האם ההצבה הצליחה או לא כדי שהUI יוכל להגיב בהתאם (למשל לאפשר או לא לאפשר לחצן "הצבת חייל" על מדינה מסוימת).
     * */
    public boolean placeArmy(Country country)
    {
        boolean placeArmyComplete = getCurrentState().placeArmy(country);
        if(placeArmyComplete)
            notifyStatsUpdated();

        return placeArmyComplete;
    }
    /**
     * @param attacker המדינה התוקפת
     * @param defender המדינה המגינה
     * טענת יציאה: הפונקציה מנסה לבצע התקפה בין שתי מדינות. אם זה הצליח (כלומר היה אפשרי לפי החוקים)
     *                    אז מעדכנים את הסטטיסטיקות בממשק ומחזירים את תוצאת הקרב כדי שהUI יוכל להציג אותה למשתמש (למשל כמה חיילים נהרגו מכל צד).
     * */
    public BattleResult attack(Country attacker, Country defender)
    {
        BattleResult attackResult = getCurrentState().attack(attacker, defender);
        notifyStatsUpdated();
        return attackResult;
    }
    /**
     * @param amount מספר החיילים שברצוננו להעביר
     * @param from המדינה שממנה רוצים להעביר
     * @param to המדינה אליה רוצים להעביר
     * טענת יציאה: הפונקציה מנסה לבצע פורטיפיקציה בין שתי מדינות. אם זה הצליח (כלומר היה אפשרי לפי החוקים)
     *                    אז מעדכנים את הסטטיסטיקות בממשק ומחזירים הודעה האם הפורטיפיקציה הצליחה או לא כדי שהUI יוכל להציג אותה למשתמש
     *                    (למשל לאפשר או לא לאפשר לחצן "העבר חיילים" על מדינה מסוימת או להציג הודעה שהפורטיפיקציה לא אפשרית).
     * */
    public String fortify(Country from, Country to, int amount) {
        String fortifyResult =  getCurrentState().fortify(from, to, amount);
        notifyStatsUpdated();
        return fortifyResult;
    }
    /**
     * טענת יציאה: הפונקציה אחראית להעביר אותנו לשלב הבא.
     * */
    public void nextPhase() {
        GameState next = getCurrentState().nextPhase();
        if (next != null)
            setCurrentState(next); // מחליף סטייט רק אם באמת קיבלנו אישור לעבור שלב

    }
    /**
     * @param attacker המדינה התוקפת שהרגע זכתה בקרב
     * @param defender המדינה המגינה שהרגע הפסידה בקרב
     * @param moveAmount מספר החיילים שהרחקנו מהמדינה המגינה והעברנו למדינה התוקפת
     * טענת יציאה: הפונקציה אחראית לבצע את שלב הכיבוש לאחר קרב מוצלח.
     *                   היא מעדכנת את הבעלים של המדינות, מעבירה את החיילים מהמדינה המגינה לתוקפת ומעדכנת את הסטטיסטיקות בממשק.
     * */
    public void handleConquest(Country attacker, Country defender, int moveAmount)
    {
        this.combatManager.executeConquest(attacker, defender, moveAmount);
        notifyStatsUpdated();
    }
    //טענת יציאה: מחזירה את השחקן הנוכחי
    public Player getCurrentPlayer()
    {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }
    /**
     * טענת יציאה: הפונקציה אחראית לעבור לשחקן הבא בשלב הSetup עד שנגמר.
     * */
    public void advanceSetupTurn()
    {
        // תעבור לשחקן הבא שעדיין יש לו מדינות לשים בשלב הSetup
        int attempts = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            attempts++;
            if (attempts > players.size())
            {
                log.error("CRITICAL: No valid players found for next turn!");
                this.gameOver = true;
                return;
            }
        }
        while (players.get(currentPlayerIndex).getOwnedCountries().isEmpty());

        currentPlayerProperty.set(players.get(currentPlayerIndex));
    }

    // מאפשר ל-UI להירשם כמאזין
    public void addGameUpdateListener(GameUpdateListener listener) {
        listeners.add(listener);
    }

    // פונקציות עזר שהמודל יפעיל כדי "לצעוק" למאזינים
    private void notifyListeners(Consumer<GameUpdateListener> action) {
        for (GameUpdateListener listener : listeners) {
            action.accept(listener);
        }
    }

    public void notifyStatsUpdated() {
        notifyListeners(GameUpdateListener::onStatsUpdated);
    }

    public void notifyGameMessage(String message) {
        notifyListeners(l -> l.onGameMessage(message));
    }
}