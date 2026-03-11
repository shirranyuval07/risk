package Model.States;

import Model.Country;
import Model.Player;
import Model.Records.BattleResult;
import Model.RiskGame;

import java.util.*;

public class AttackState implements GameState {

    private final RiskGame game;

    public AttackState(RiskGame game) {
        this.game = game;
    }

    @Override
    public boolean placeArmy(Country country) {
        return false; // לא חוקי בשלב זה
    }

    @Override
    public BattleResult attack(Country attacker, Country defender) {
        Player currentPlayer = game.getCurrentPlayer();

        // ולידציות של שלב ההתקפה
        if (attacker.getOwner() != currentPlayer) return null; //"Not your country!"
        if (defender.getOwner() == currentPlayer) return null; //"Can't attack yourself!"

        // שליפת השכנים היא גישה לרשימת השכנויות בגרף בסיבוכיות (O(k
        if (!attacker.getNeighbors().contains(defender)) return null; //"Not a neighbor!"
        if (attacker.getArmies() <= 1) return null; //"Need more than 1 army to attack!"

        // הטלת קוביות (עד 3 לתוקף, עד 2 למגן)
        int aDiceCount = Math.min(3, attacker.getArmies() - 1);
        int dDiceCount = Math.min(2, defender.getArmies());

        Integer[] aRolls = game.getDice().roll(aDiceCount);
        Integer[] dRolls = game.getDice().roll(dDiceCount);

        // השוואת קוביות (חוקי ריסק: משווים את התוצאות הגבוהות ביותר)
        int comparisons = Math.min(aDiceCount, dDiceCount);
        int aLoss = 0, dLoss = 0;

        Arrays.sort(aRolls, Collections.reverseOrder());
        Arrays.sort(dRolls, Collections.reverseOrder());

        for (int i = 0; i < comparisons; i++) {
            if (aRolls[i] > dRolls[i]) dLoss++;
            else aLoss++;
        }

        attacker.removeArmies(aLoss);
        defender.removeArmies(dLoss);

        String result = String.format("Attack Result: Attacker lost %d, Defender lost %d", aLoss, dLoss);

        // בדיקת כיבוש - הפעלת מתודת העזר ממחלקת הניהול
        boolean isConquered = false;
        if (defender.getArmies() == 0) {
            result += " | COUNTRY CONQUERED!";
            game.handleConquest(attacker, defender, aDiceCount);
            isConquered = true;
        }
        System.out.printf(result + "\n");
        game.notifyObservers();

        return new BattleResult(aRolls,dRolls,aLoss,dLoss,isConquered);
    }

    @Override
    public String fortify(Country from, Country to, int amount) {
        return "Wrong phase! You are currently in the Attack phase.";
    }

    @Override
    public void nextPhase() {
        // מעבר לשלב הביצור
        game.setCurrentState(new FortifyState(game));
    }

    @Override
    public Set<Country> getValidTargets(Country source) {
        Set<Country> enemyCountries = new HashSet<Country>();
        for(Country c : source.getNeighbors())
        {
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