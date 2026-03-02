package Model;

public class GreedyAI implements BotStrategy {

    @Override
    public void executeTurn(Player player, RiskGame game) {
        System.out.println("--- AI Turn Started: " + player.getName() + " ---");

        // שלב 1: DRAFT (המשחק כרגע בשלב הזה אוטומטית כשהתור מתחיל)
        chooseReinforcement(player, game);

        // *** התיקון הקריטי: הבוט מקדם את המשחק לשלב ההתקפה ***
        game.nextPhase();

        // שלב 2: ATTACK
        chooseAttack(player, game);

        // *** התיקון הקריטי: הבוט מקדם את המשחק לשלב הביצור ***
        game.nextPhase();

        // שלב 3: FORTIFY
        chooseFortify(player, game);

        System.out.println("--- AI Turn Ended ---");
        // כשהפונקציה תסתיים, מחלקת Player תקדם את התור אוטומטית חזרה אליך
    }

    private void chooseReinforcement(Player player, RiskGame game) {
        Country mostThreatened = null;
        int maxEnemies = -1;

        for (Country c : player.getOwnedCountries()) {
            int enemyCount = countEnemyNeighbors(c, player);
            if (enemyCount > maxEnemies) {
                maxEnemies = enemyCount;
                mostThreatened = c;
            }
        }

        if (mostThreatened != null) {
            // הבוט עכשיו משתמש בפונקציה החוקית של המשחק כדי להציב חיילים
            int startArmies = player.getDraftArmies();
            while (player.getDraftArmies() > 0) {
                game.placeArmy(mostThreatened);
            }
            System.out.println("AI drafted " + startArmies + " armies on: " + mostThreatened.getName());
        }
    }

    private void chooseAttack(Player player, RiskGame game) {
        boolean keepAttacking = true;

        while (keepAttacking) {
            Country bestAttacker = null;
            Country bestTarget = null;
            int bestScore = -999;

            for (Country myCountry : player.getOwnedCountries()) {
                if (myCountry.getArmies() > 1) {
                    for (Country neighbor : myCountry.getNeighbors()) {
                        if (neighbor.getOwner() != player) {
                            int score = myCountry.getArmies() - neighbor.getArmies();
                            // מחפש יתרון של לפחות 2 חיילים
                            if (score > bestScore && score >= 2) {
                                bestScore = score;
                                bestAttacker = myCountry;
                                bestTarget = neighbor;
                            }
                        }
                    }
                }
            }

            if (bestAttacker != null && bestTarget != null) {
                System.out.println("AI attacking " + bestTarget.getName() + " from " + bestAttacker.getName());
                String result = game.attack(bestAttacker, bestTarget);
                System.out.println("AI Attack Result: " + result);
            } else {
                keepAttacking = false;
            }
        }
    }

    private void chooseFortify(Player player, RiskGame game) {
        Country safeCountry = null;
        Country borderCountry = null;

        for (Country c : player.getOwnedCountries()) {
            // מחפש מדינה פנימית שאין לה שכנים עוינים ויש בה יותר מלוחם אחד
            if (c.getArmies() > 1 && countEnemyNeighbors(c, player) == 0) {
                safeCountry = c;
                break;
            }
        }

        if (safeCountry != null) {
            for (Country neighbor : safeCountry.getNeighbors()) {
                // מוצא שכן על הגבול כדי להעביר אליו כוחות
                if (neighbor.getOwner() == player && countEnemyNeighbors(neighbor, player) > 0) {
                    borderCountry = neighbor;
                    break;
                }
            }
        }

        if (safeCountry != null && borderCountry != null) {
            int armiesToMove = safeCountry.getArmies() - 1;
            String result = game.fortify(safeCountry, borderCountry, armiesToMove);
            System.out.println("AI Fortify: " + result);
        }
    }

    private int countEnemyNeighbors(Country c, Player me) {
        int count = 0;
        for (Country neighbor : c.getNeighbors()) {
            if (neighbor.getOwner() != me) {
                count++;
            }
        }
        return count;
    }
}