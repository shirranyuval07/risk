package com.example.demo.controller;

import com.example.demo.view.dialog.DialogManager;

import com.example.demo.model.manager.Card;
import com.example.demo.service.AIEngine;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.GameUpdateListener;
import com.example.demo.model.manager.Player;
import com.example.demo.view.GameRoot;
import com.example.demo.model.States.GameState.SetupState;
import com.example.demo.model.States.GameState.DraftState;
import com.example.demo.model.States.GameState.AttackState;
import com.example.demo.model.States.GameState.FortifyState;
import com.example.demo.model.Records.GameRecords.BattleResult;

import com.example.demo.model.manager.RiskGame;
import com.example.demo.model.States.*;
import com.example.demo.network.shared.GameAction;
import com.example.demo.network.client.RiskWebSocketClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class GameController {


    private final RiskGame gameModel;
    private final GameRoot gameView;
    private final RiskWebSocketClient networkClient;
    private final boolean isMultiplayer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Class<? extends GameState>, Consumer<Country>> phaseClickHandlers = new HashMap<>();

    // Tracks the first country selected during Attack / Fortify
    private Country sourceCountry = null;

    // Prevents the attacker's client from double-applying conquest army moves
    private boolean pendingConquestHandled = false;

    private final Runnable onReturnToMenu;




    private final AIEngine.Service aiService = new AIEngine.Service(new Card.Service());

    public GameController(RiskGame model, GameRoot view, RiskWebSocketClient networkClient, Runnable onReturnToMenu) {

        this.gameModel = model;
        this.gameView = view;
        this.networkClient = networkClient;
        this.isMultiplayer = (networkClient != null);
        this.onReturnToMenu = onReturnToMenu;

        gameModel.addGameUpdateListener(new GameUpdateListener() {
            @Override
            public void onStatsUpdated() {
                // כשמגיעה קריאה מהמודל - מעדכנים את המסך!
                javafx.application.Platform.runLater(() ->
                        gameView.getPlayerStatsPane().updateStats()
                );
            }

            @Override
            public void onGameMessage(String message) {
                // כשמגיעה הודעה מהמודל - מציגים אותה במסך!
                javafx.application.Platform.runLater(() ->
                        gameView.getControlPane().setMessage(message)
                );
            }
        });

        initializeUIListeners();

        if (isMultiplayer) {
            initializeNetworkListeners();
        }

        checkAndExecuteAITurn();
    }


    /**
     * טענת יציאה: הפונקציה מכניסה למפה של ההנדלרים את השלבי משחק, הפעולות התואמות.
     * מאתחלת מאזינים עבור: לחיצה על המפה, לחיצה על כפתור השלב הבא, לחיצה על כפתור ההדלקת שמות, לחיצה על כפתור הקלפים, לחיצה על כפתור חזרה למסך הראשי.
     * */
    private void initializeUIListeners() {
        //הצבה במפה של הסוג שלב, הפעולה שמטפלת בו.
        phaseClickHandlers.put(SetupState.class, this::handleSetupClick);
        phaseClickHandlers.put(DraftState.class, this::handleDraftClick);
        phaseClickHandlers.put(AttackState.class, this::handleAttackClick);
        phaseClickHandlers.put(FortifyState.class, this::handleFortifyClick);

        // Map click — הקלקה על מדינה במפה תשלח את המדינה ללוגיקה שתואמת את שלב המשחק הנוכחי
        gameView.getMapPane().setOnCountryClick(clickedCountry -> {
            if (isCurrentPlayerAI()) return;
            if (isMultiplayer && !isMyTurn()) {
                gameView.getControlPane().setMessage("It's not your turn!");
                return;
            }
            if (clickedCountry != null) {
                handleCountryClick(clickedCountry);
            }
        });

        // Next Phase button
        gameView.getControlPane().getBtnNextPhase().setOnAction(e -> {
            if (isCurrentPlayerAI()) return;
            if (isMultiplayer && !isMyTurn()) return;
            handleNextPhaseRequest();
        });

        // Toggle country name labels on the map
        gameView.getControlPane().getBtnToggleNames().setOnAction(e -> {
            javafx.scene.control.Button btn = gameView.getControlPane().getBtnToggleNames();
            boolean showNames = btn.getText().contains("Show");
            btn.setText(showNames ? "👁 Hide Names" : "👁 Show Names");
            gameView.getMapPane().toggleNames(showNames);
        });

        // Cards dialog
        gameView.getControlPane().getBtnCards().setOnAction(e ->
                DialogManager.showCardsDialog(gameModel.getCurrentPlayer(), () ->
                        gameView.getPlayerStatsPane().updateStats()
                )
        );
        gameView.getControlPane().getBtnBackToMainMenu().setOnAction(e -> {
            if (networkClient != null) { networkClient.disconnect(); }

            if (onReturnToMenu != null) {
                onReturnToMenu.run(); // זה מפעיל את הפונקציה showMainMenu מ-RiskApplication!
            }
            log.info("Returning to Main Menu...");
        });
    }


    /**
     * טענת יציאה: ברגע שהתקבלה הודעה, מוודאים שלא מפריעים לthread הראשי בכך שקוראים לפעולה Platform.runLater().
     * הודעה מתקבלת בצורת GameMessage, שמכילה סוג הודעה (GameAction) וpayload שהוא מפה של מחרוזות לאובייקטים (כי כל סוג הודעה יכול להביא איתו מידע שונה).
     * בודקים מה סוג ההודעה בעזרת switch - case. על פי סוג ההודעה קוראים לפונקציה המתאימה. אם ההודעה לא נוגעת לפעולת משחק ישירה מתעלמים ממנה.
     * קוראים למאזין הזה רק אם המשחק הוא רב משתתפים.
     * */
    private void initializeNetworkListeners() {
        networkClient.setOnMessageReceived(message ->
                Platform.runLater(() -> {

                    Map<String, Object> payload = message.content();

                    if (payload == null) {
                        payload = new HashMap<>(); // הגנה מפני payload ריק
                    }

                    switch (message.type()) {

                        case NEXT_PHASE -> executeNextPhaseLocal();

                        case SETUP_PLACE -> {
                            String countryId = String.valueOf(payload.get("SetupPlaceID"));
                            executePlacementLocal(getCountry(countryId));
                        }

                        case DRAFT -> {
                            String countryId = String.valueOf(payload.get("targetCountryId"));
                            executePlacementLocal(getCountry(countryId));
                        }

                        case NEXT_TURN -> {
                            String playerName = (String) payload.get("NEXT_TURN");
                            int draftArmies = (Integer) payload.get("DRAFT_ARMIES");
                            executeNextTurnLocal(playerName, draftArmies);
                        }

                        case FORTIFY -> {
                            String srcId = String.valueOf(payload.get("FORTIFY"));
                            String destId = String.valueOf(payload.get("DESTINATION_ID"));
                            int amount = (Integer) payload.get("FORTIFY_AMOUNT");
                            executeFortifyLocal(getCountry(srcId), getCountry(destId), amount);
                        }

                        case CONQUEST_MOVE -> {
                            String srcId = String.valueOf(payload.get("CONQUEST_MOVE"));
                            String destId = String.valueOf(payload.get("CONQUEST_DESTINATION"));
                            int minMove = (Integer) payload.get("MIN_MOVE");
                            int amount = (Integer) payload.get("CONQUEST_AMOUNT");
                            applyConquestMove(getCountry(srcId), getCountry(destId), minMove, amount);
                        }

                        case BATTLE_RESULT -> {
                            try {
                                String attackerId = String.valueOf(payload.get("attackerId"));
                                String defenderId = String.valueOf(payload.get("defenderId"));
                                BattleResult result = objectMapper.convertValue(payload.get("battleResult"), BattleResult.class);
                                applyBattleResult(getCountry(attackerId), getCountry(defenderId), result);
                            } catch (Exception e) {
                                log.error("Failed to parse BattleResult: {}", e.getMessage());
                            }
                        }

                        default -> {
                            // מתעלם מסוגי הודעות אחרות שלא נוגעות למהלך המשחק הישיר כאן (כמו JOIN_ROOM)
                        }
                    }
                })
        );
    }

    /**
     * טענת כניסה: מקבלת מדינה שלחצו עליה
     * טענת יציאה: הפעולה לוקחת את המצב משחק הנוכחי ומוציאה מהמפה של ההנדלרים את הפעולה הנדרשת.
     * אם הפעולה לא null עוברים לפעולה המבוקשת
     *
     * @param clickedCountry המדינה שלחצו עליה
     */

    private void handleCountryClick(Country clickedCountry) {
        Consumer<Country> handler = phaseClickHandlers.get(gameModel.getCurrentState().getClass());
        if (handler != null) {
            handler.accept(clickedCountry);
        }
    }


    /**
     * טענת כניסה: מדינה שהשחקן בחר לשים בה חייל
     * טענת יציאה: הפעולה בודקת אם המשחק הוא רב משתתפים. אם הוא כן תיצור payload שמכיל את המזהה של המדינה ותשלח לכל המשתתפים את ההודעה בשביל שיוכלו להיווצר השינוי אצלם במסך
     * אחרת תקרא לפעולה שמבצעת שינוי לוקלי בשלב הsetup
     * @param country המדינה שהשחקן בחר לשים בה חייל
     * */
    private void handleSetupClick(Country country) {
        if (isMultiplayer) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("SetupPlaceID", country.getId());
            networkClient.sendAction(GameAction.SETUP_PLACE, networkClient.getRoomId(), payload);
        } else {
            executePlacementLocal(country);
        }
    }
    /**
     * טענת כניסה: מדינה שהשחקן בחר לשים בה חייל
     * טענת יציאה: הפעולה בודקת אם המשחק הוא רב משתתפים. אם הוא כן תיצור payload שמכיל את המזהה של המדינה ותשלח לכל המשתתפים את ההודעה בשביל שיוכלו להיווצר השינוי אצלם במסך
     * אחרת תקרא לפעולה שמבצעת שינוי לוקלי בשלב draft
     * @param country המדינה שהשחקן בחר לשים בה חייל
     * */
    private void handleDraftClick(Country country) {
        if (isMultiplayer) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("targetCountryId", country.getId());
            networkClient.sendAction(GameAction.DRAFT, networkClient.getRoomId(), payload);
        } else {
            executePlacementLocal(country);
        }
    }

    /**
     * טענת כניסה:מדינה שהשחקן בחר לשים בה חייל
     * טענת יציאה: המודל מנסה להציב מדינה עבור השלב הזה ומציג הודעה מתאימה. לאחר מכן בודק אם זה תור של AI
     * @param country המדינה שהשחקן בחר לשים בה חייל
     * */
    private void executePlacementLocal(Country country) {
        if (gameModel.placeArmy(country)) {
            gameView.getControlPane().setMessage("Placed army on " + country.getName());
            checkAndExecuteAITurn();
        } else {
            gameView.getControlPane().setMessage("Cannot place army here!");
        }
    }


    /**
     * טענת יציאה: הפונקציה מטפלת בבקשה לעבור לשלב הבא של המשחק.
     * */
    private void handleNextPhaseRequest() {
        if (isMultiplayer) {
            Map<String, Object> payload = new HashMap<>();
            networkClient.sendAction(GameAction.NEXT_PHASE, networkClient.getRoomId(), payload);
        } else {
            executeNextPhaseLocal();
        }
    }
    /**
     * טענת יציאה: הפונקציה מעבירה את המשחק לשלב הבא שלו.
     * */
    private void executeNextPhaseLocal() {
        boolean wasFortify = gameModel.getCurrentState() instanceof FortifyState;

        gameModel.nextPhase();
        clearSelection();

        if (isMultiplayer && gameModel.getCurrentState() instanceof DraftState && !wasFortify)
            broadcastNextTurnIfNeeded();


        if (isCurrentPlayerAI())
            checkAndExecuteAITurn();

        else if (gameModel.getCurrentState() instanceof DraftState
                && gameModel.getCurrentPlayer().getDraftArmies() > 0)
        {
            gameView.getControlPane().setMessage("You have armies left to place!");
        }
    }
    /**
     * טענת יציאה: הפונקציה בודקת מי השחקן הבא בתור.
     * אם השחקן הבא הוא לא אני, שולחת לכל המשתתפים הודעה עם שם השחקן החדש וכמות החיילים שיש לו להניח בשלב הדראפט, כדי שכולם יעודכנו על השינוי בתור.
     * */
    private void broadcastNextTurnIfNeeded() {
        Player newCurrentPlayer = gameModel.getCurrentPlayer();
        if (!newCurrentPlayer.getName().equals(networkClient.getPlayerName())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("NEXT_TURN", newCurrentPlayer.getName());
            payload.put("DRAFT_ARMIES", newCurrentPlayer.getDraftArmies());
            networkClient.sendAction(GameAction.NEXT_TURN, networkClient.getRoomId(), payload);
        }
    }
    /**
     *       טענת יציאה: הפונקציה מבצעת מעבר לתור הבא בלוקאלי, על ידי קריאה לפונקציה של המודל שמעבירה את המשחק לתור הבא.
     *      * לאחר מכן היא מעדכנת את כמות החיילים שיש לשחקן החדש להניח בשלב הדראפט,
     *      *                  כדי שהמסך יציג את המידע הנכון. לבסוף היא מנקה את הבחירות במפה כדי שהשחקן החדש יתחיל עם מסך נקי.
     * @param draftArmies כמות החיילים שיש לשחקן החדש להניח בשלב הדראפט
     * @param playerName שם השחקן החדש בתור, כדי שנוכל לעדכן את כמות החיילים שלו לשים בשלב הדראפט
     * */
    private void executeNextTurnLocal(String playerName, int draftArmies) {
        gameModel.nextTurn();

        for (Player p : gameModel.getPlayers()) {
            if (p.getName().equals(playerName)) {
                p.setDraftArmies(draftArmies);
                break;
            }
        }

        clearSelection();
    }

    /**
     * טענת יציאה: הפונקציה מטפלת בלחיצה על מדינה בשלב הפורטיפיי.
     * אם זו הלחיצה הראשונה, בודקת אם אפשר לחזק מהמדינה הזו (שייכת לשחקן ויש בה יותר מחייל אחד).
     * אם אפשר, שומרת את המדינה כמקור ומדגישה את היעדים האפשריים.
     * אם זו לא הלחיצה הראשונה, בודקת אם הלחיצה היא על אותה מדינה (במקרה הזה מנקה את הבחירה),
     * או על מדינה לא חוקית (מציגה הודעת שגיאה),
     * או על מדינה חוקית (במקרה הזה קוראת לפונקציה שמבקשת מהשחקן כמה חיילים הוא רוצה להזיז ומבצעת את ההעברה).
     * @param clickedCountry המדינה שהשחקן לחץ עליה בשלב הפורטיפיי
     * */
    private void handleFortifyClick(Country clickedCountry) {
        if (sourceCountry == null) {
            if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer())
                    && clickedCountry.getArmies() > 1) {
                setSelection(clickedCountry, "Move from " + clickedCountry.getName() + ". Select target.");
                Set<Country> targets = gameModel.getCurrentState().getValidTargets(clickedCountry);
                if (targets.isEmpty()) {
                    gameView.getControlPane().setMessage("No valid targets to fortify from here!");
                } else {
                    gameView.getMapPane().highlightTargets(targets);
                }
            }
        }
        else
        {
            if (clickedCountry.equals(sourceCountry))
            {
                clearSelection();
            }
            else if(!gameModel.getCurrentState().getValidTargets(sourceCountry).contains(clickedCountry))
            {
                gameView.getControlPane().setMessage("Invalid target!");
            }
            else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer()))
            {
                promptFortifyAmount(clickedCountry);
            }
        }
    }
    /**
     *           טענת יציאה: הפונקציה אחראית להציג דיאלוג עבור כמות המדינות להעביר בשלב הfortify.
     * @param destination המדינה שהשחקן רוצה לחזק
     * */
    private void promptFortifyAmount(Country destination) {
        int maxMove = sourceCountry.getArmies() - 1;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fortify Territory");
        dialog.setHeaderText("Moving armies from " + sourceCountry.getName() + " to " + destination.getName());
        dialog.setContentText("Enter amount (Max: " + maxMove + "):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                if (!input.isEmpty()) {
                    int amount = Integer.parseInt(input);
                    if (isMultiplayer) {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("FORTIFY", sourceCountry.getId());
                        payload.put("DESTINATION_ID", destination.getId());
                        payload.put("FORTIFY_AMOUNT", amount);

                        networkClient.sendAction(GameAction.FORTIFY, networkClient.getRoomId(), payload);
                        clearSelection();
                    } else {
                        executeFortifyLocal(sourceCountry, destination, amount);
                    }
                }
            } catch (NumberFormatException e) {
                gameView.getControlPane().setMessage("Invalid number!");
                clearSelection();
            }
        });
    }
    /**
     * @param amount כמות החיילים שהשחקן רוצה להעביר בין המדינות בשלב הפורטיפיי
     * @param dst המדינה שהשחקן רוצה לחזק
     * @param src המדינה שממנה השחקן רוצה להעביר חיילים
     * טענת יציאה: הפונקציה מבצעת את ההעברה של החיילים בין המדינות בשלב הפורטיפיי.
     * */
    private void executeFortifyLocal(Country src, Country dst, int amount) {
        String resultMsg = gameModel.fortify(src, dst, amount);
        gameView.getControlPane().setMessage(resultMsg);
        clearSelection();
        checkAndExecuteAITurn();
    }

    /**
     * @param clickedCountry המדינה שהשחקן לחץ עליה בשלב האטאק
     *                       טענת יציאה: הפונקציה מטפלת בלחיצה על מדינה בשלב האטאק.
     * אם זו הלחיצה הראשונה, בודקת אם אפשר לתקוף מהמדינה הזו (שייכת לשחקן ויש בה יותר מחייל אחד).
     * אם אפשר, שומרת את המדינה כמקור ומדגישה את היעדים האפשריים.
     *                       אם זו לא לחיצה ראשונה, בודקת אם אפשר לבצע תקיפה ואם כן מבצעת אחרת מנקה את הבחירות.
     * */
    private void handleAttackClick(Country clickedCountry) {
        if (sourceCountry == null)
        {
            if (canAttackFrom(clickedCountry))
            {
                setSelection(clickedCountry, "Select target for " + clickedCountry.getName());
                Set<Country> targets = gameModel.getCurrentState().getValidTargets(clickedCountry);
                if (targets.isEmpty())
                    gameView.getControlPane().setMessage("No valid targets to attack from here!");
                else
                    gameView.getMapPane().highlightTargets(targets);
            }
        }
        else
        {
            if (clickedCountry.equals(sourceCountry))
                clearSelection();
            else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer()))
                clearSelection();
            else
            {
                performAttack(sourceCountry, clickedCountry);
                clearSelection();
            }
        }
    }
    /**
     * @param attacker המדינה שממנה מתבצעת התקיפה
     * @param defender המדינה שמותקפת
     * טענת יציאה: הפונקציה מבצעת את ההתקפה בין המדינות בשלב האטאק.
     * אם המשחק הוא רב משתתפים, הפונקציה יוצרת payload עם המידע על התקיפה ושולחת לכל המשתתפים את ההתקפה
     *                 אחרת רק תבצע את ההתקפה לוקאלית ותציג את התוצאה בדיאלוג.
     * */
    private void performAttack(Country attacker, Country defender) {
        if (isMultiplayer)
        {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ATTACK_REQ", attacker.getId());
            payload.put("DESTINATION_ID", defender.getId());
            payload.put("ATTACKER_ARMIES", attacker.getArmies());
            payload.put("DEFENDER_ARMIES", defender.getArmies());
            networkClient.sendAction(GameAction.ATTACK_REQ, networkClient.getRoomId(), payload);
        }
        else
        {
            BattleResult result = gameModel.attack(attacker, defender);
            if (result != null)
                applyBattleResult(attacker, defender, result);
            else
                gameView.getControlPane().setMessage("Attack failed or invalid.");

        }
    }
    /**
     * @param attacker המדינה שממנה מתבצעת התקיפה
     * @param defender המדינה שמותקפת
     * @param result תוצאת הקרב בין המדינות
     * טענת יציאה: הפונקציה מטפלת בתוצאות הקרב בין המדינות בשלב האטאק.
     * אם המשחק הוא רב משתתפים, הפונקציה תעדכן את כמות החיילים של המדינות בהתאם לתוצאות הקרב, ותציג את התוצאה בדיאלוג רק אם אני התוקף.
     * אם התוקף כבש את המגן, הפונקציה תציג דיאלוג שיבקש מהתוקף כמה חיילים הוא רוצה להזיז לכיבוש,
     *               ותבצע את הכיבוש בהתאם לתוצאה שהתקבלה מהתוקף. אם המשחק הוא לא רב משתתפים, הפונקציה רק תבצע את השינויים לוקאלית ותציג את התוצאה בדיאלוג.
     * */
    private void applyBattleResult(Country attacker, Country defender, BattleResult result) {
        if (isMultiplayer)
        {
            attacker.removeArmies(result.attackerLosses());
            defender.removeArmies(result.defenderLosses());
        }
        boolean iAmTheAttacker = !isMultiplayer || attacker.getOwner().getName().equals(networkClient.getPlayerName());

        if (iAmTheAttacker)
            DialogManager.showBattleResultDialog(result);


        if (result.conquered())
        {
            int minMove = result.minMove();
            int maxMove = result.maxMove();
            if (iAmTheAttacker)
            {
                int chosenAmount = showConquestMoveDialog(defender, minMove, maxMove);
                gameModel.handleConquest(attacker, defender, chosenAmount);
                if (isMultiplayer)
                {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("CONQUEST_MOVE", attacker.getId());
                    payload.put("CONQUEST_DESTINATION", defender.getId());
                    payload.put("CONQUEST_AMOUNT", chosenAmount);
                    payload.put("MIN_MOVE", minMove);

                    pendingConquestHandled = true;
                    networkClient.sendAction(GameAction.CONQUEST_MOVE, networkClient.getRoomId(), payload);
                }
            }
            else
                gameModel.handleConquest(attacker, defender, minMove);

            gameView.getControlPane().setMessage("Territory Conquered!");
        }
        else
            gameView.getControlPane().setMessage("Attack completed.");

    }
    /**
     * @param attacker המדינה שממנה מתבצעת התקיפה
     * @param defender המדינה שמותקפת
     * @param minMove כמות החיילים המינימלית שהשחקן חייב להזיז אם כבש את המגן
     * @param totalMove כמות החיילים שהשחקן בחר להזיז לכיבוש אם כבש את המגן
     * טענת יציאה: הפונקציה מטפלת בתנועת החיילים לאחר שמדינה נכבשה.
     * */
    private void applyConquestMove(Country attacker, Country defender, int minMove, int totalMove) {
        if (pendingConquestHandled)
        {
            pendingConquestHandled = false;
            return;
        }
        int extra = totalMove - minMove;
        if (extra > 0)
        {
            attacker.removeArmies(extra);
            defender.addArmies(extra);
        }
    }
    /**
     * @param minMove כמות החיילים המינימלית שהשחקן חייב להזיז אם כבש את המגן
     * @param maxMove כמות החיילים המקסימלית שהשחקן יכול להזיז אם כבש את המגן (תלויה בכמות החיילים שהיו בתוקף לפני הקרב)
     * @param conquered המדינה שהשחקן כבש
     * טענת יציאה: הפונקציה מציגה דיאלוג שמבקש מהשחקן לבחור כמה חיילים הוא רוצה להזיז לכיבוש המדינה שנכבשה.
     * */
    private static int showConquestMoveDialog(Country conquered, int minMove, int maxMove) {
        List<Integer> choices = new ArrayList<>();
        for (int i = minMove; i <= maxMove; i++) choices.add(i);

        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(maxMove, choices);
        dialog.setTitle("Victory!");
        dialog.setHeaderText("You conquered " + conquered.getName());
        dialog.setContentText("Choose how many armies to move:");

        return dialog.showAndWait().orElse(maxMove);
    }

    /**
     * טענת יציאה: הפונקציה בודקת אם המשחק הגיע למצב שבו שחקן AI צריך לשחק את תורו, ואם כן מפעילה את ה-AI לבצע את המהלך שלו.
     * הפונקציה בונה דיליי בשביל שהשחקן לא יעשה את התור שלו מהר מדי.
     * */
    private void checkAndExecuteAITurn()
    {
        if (gameModel.isGameOver())
        {
            gameView.getControlPane().setMessage("🏆 GAME OVER! Winner: " + gameModel.getCurrentPlayer().getName() + " 🏆");
            gameView.getControlPane().showGameOverState();
            return;
        }

        if (isCurrentPlayerAI())
        {
            PauseTransition pause = buildAIPause();
            pause.play();
        }
    }
    /**
     * טענת יציאה: פונקצית עזר בשביל לבנות דיליי של שנייה בכל שלב במשחק (חוץ מהשלב הראשון שרוצים שה AI יגיב מהר)
     * וברגע שנגמר בדיליי, אם המשחק לא נגמר מבקשים מה AI service לשחק את התור. לאחר מכן מנקים את הבחירות ובודקים אם זה שוב תור של AI.
     * */
    private @NonNull PauseTransition buildAIPause() {
        Duration delay = (gameModel.getCurrentState() instanceof SetupState)
                ? Duration.seconds(0.05)
                : Duration.seconds(1);

        PauseTransition pause = new PauseTransition(delay);
        pause.setOnFinished(e -> {
            if (gameModel.isGameOver() || !isCurrentPlayerAI()) return;
            aiService.playTurn(gameModel.getCurrentPlayer(), gameModel);
            clearSelection();
            checkAndExecuteAITurn();
        });
        return pause;
    }


    //  Helpers

    private boolean isMyTurn() {
        return !isMultiplayer ||
                gameModel.getCurrentPlayer().getName().equals(networkClient.getPlayerName());
    }

    private boolean isCurrentPlayerAI() {
        return gameModel.getCurrentPlayer() != null && gameModel.getCurrentPlayer().isAI();
    }

    private boolean canAttackFrom(Country c) {
        return c.getOwner().equals(gameModel.getCurrentPlayer()) && c.getArmies() > 1;
    }

    private Country getCountry(String id) {
        return gameModel.getBoard().getCountry(Integer.parseInt(id));
    }

    private void setSelection(Country c, String msg) {
        sourceCountry = c;
        gameView.getMapPane().setSelectedCountry(c);
        gameView.getControlPane().setMessage(msg);
    }

    private void clearSelection() {
        sourceCountry = null;
        gameView.getMapPane().setSelectedCountry(null);
        gameView.getMapPane().clearHighlights();
    }
}