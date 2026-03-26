package com.example.demo.controller;

import com.example.demo.view.dialog.BattleResultDialog;
import com.example.demo.model.Country;
import com.example.demo.model.GameUpdateListener;
import com.example.demo.model.Player;
import com.example.demo.model.Records.BattleResult;
import com.example.demo.view.dialog.CardsDialog;
import com.example.demo.view.GameRoot;

import com.example.demo.model.RiskGame;
import com.example.demo.model.States.*;
import com.example.demo.network.shared.GameAction;
import com.example.demo.network.client.RiskWebSocketClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.PauseTransition;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class GameController {

    // =========================================================================
    //  Fields
    // =========================================================================

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

    private final Logger log = Logger.getLogger(GameController.class.getName());

    // =========================================================================
    //  Constructor
    // =========================================================================

    public GameController(RiskGame model, GameRoot view, RiskWebSocketClient networkClient) {

        this.gameModel = model;
        this.gameView = view;
        this.networkClient = networkClient;
        this.isMultiplayer = (networkClient != null);

        // בתוך הבנאי של GameController:
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

    // =========================================================================
    //  UI Listeners — buttons and map clicks
    // =========================================================================

    private void initializeUIListeners() {

        phaseClickHandlers.put(SetupState.class, this::handleSetupClick);
        phaseClickHandlers.put(DraftState.class, this::handleDraftClick);
        phaseClickHandlers.put(AttackState.class, this::handleAttackClick);
        phaseClickHandlers.put(FortifyState.class, this::handleFortifyClick);

        // Map click — route to the correct phase handler
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
                CardsDialog.show(gameModel.getCurrentPlayer(), () ->
                        gameView.getPlayerStatsPane().updateStats()
                )
        );
    }

    // =========================================================================
    //  Network Listeners — incoming messages from the server
    // =========================================================================

    private void initializeNetworkListeners() {
        networkClient.setOnMessageReceived(message ->
                javafx.application.Platform.runLater(() -> {

                    Map<String, Object> payload = message.content();

                    if (payload == null) {
                        payload = new HashMap<>(); // הגנה מפני payload ריק
                    }

                    switch (message.type()) {

                        case NEXT_PHASE -> executeNextPhaseLocal();

                        case SETUP_PLACE -> {
                            String countryId = String.valueOf(payload.get("SetupPlaceID"));
                            executeSetupLocal(getCountry(countryId));
                        }

                        case DRAFT -> {
                            String countryId = String.valueOf(payload.get("targetCountryId"));
                            executeDraftLocal(getCountry(countryId));
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
                                log.severe("Failed to parse BattleResult: " + e.getMessage());
                            }
                        }

                        default -> {
                            // מתעלם מסוגי הודעות אחרות שלא נוגעות למהלך המשחק הישיר כאן (כמו JOIN_ROOM)
                        }
                    }
                })
        );
    }

    // =========================================================================
    //  Phase Routing — decides which handler to call based on current phase
    // =========================================================================

    private void handleCountryClick(Country clickedCountry) {
        Consumer<Country> handler = phaseClickHandlers.get(gameModel.getCurrentState().getClass());
        if (handler != null) {
            handler.accept(clickedCountry);
        }
    }

    // =========================================================================
    //  SETUP Phase
    // =========================================================================

    private void handleSetupClick(Country country) {
        if (isMultiplayer) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("SetupPlaceID", country.getId());
            networkClient.sendAction(GameAction.SETUP_PLACE, networkClient.getRoomId(), payload);
        } else {
            executeSetupLocal(country);
        }
    }

    private void executeSetupLocal(Country country) {
        if (gameModel.placeArmy(country)) {
            gameView.getControlPane().setMessage("Placed army on " + country.getName());
            checkAndExecuteAITurn();
        } else {
            gameView.getControlPane().setMessage("Cannot place army here!");
        }
    }

    // =========================================================================
    //  DRAFT Phase
    // =========================================================================

    private void handleDraftClick(Country country) {
        if (isMultiplayer) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("targetCountryId", country.getId());
            networkClient.sendAction(GameAction.DRAFT, networkClient.getRoomId(), payload);
        } else {
            executeDraftLocal(country);
        }
    }

    private void executeDraftLocal(Country country) {
        gameModel.placeArmy(country);
    }

    // =========================================================================
    //  NEXT PHASE / NEXT TURN
    // =========================================================================

    private void handleNextPhaseRequest() {
        if (isMultiplayer) {
            Map<String, Object> payload = new HashMap<>();
            networkClient.sendAction(GameAction.NEXT_PHASE, networkClient.getRoomId(), payload);
        } else {
            executeNextPhaseLocal();
        }
    }

    private void executeNextPhaseLocal() {
        boolean wasFortify = gameModel.getCurrentState() instanceof FortifyState;

        gameModel.nextPhase();
        clearSelection();

        if (isMultiplayer && gameModel.getCurrentState() instanceof DraftState && !wasFortify) {
            broadcastNextTurnIfNeeded();
        }

        if (isCurrentPlayerAI()) {
            checkAndExecuteAITurn();
        } else if (gameModel.getCurrentState() instanceof DraftState
                && gameModel.getCurrentPlayer().getDraftArmies() > 0) {
            gameView.getControlPane().setMessage("You have armies left to place!");
        }
    }

    private void broadcastNextTurnIfNeeded() {
        Player newCurrentPlayer = gameModel.getCurrentPlayer();
        if (!newCurrentPlayer.getName().equals(networkClient.getPlayerName())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("NEXT_TURN", newCurrentPlayer.getName());
            payload.put("DRAFT_ARMIES", newCurrentPlayer.getDraftArmies());
            networkClient.sendAction(GameAction.NEXT_TURN, networkClient.getRoomId(), payload);
        }
    }

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

    // =========================================================================
    //  FORTIFY Phase
    // =========================================================================

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
        } else {
            if (clickedCountry.equals(sourceCountry)) {
                clearSelection();
            } else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer())) {
                promptFortifyAmount(clickedCountry);
            }
        }
    }

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

    private void executeFortifyLocal(Country src, Country dst, int amount) {
        String resultMsg = gameModel.fortify(src, dst, amount);
        gameView.getControlPane().setMessage(resultMsg);
        clearSelection();
        checkAndExecuteAITurn();
    }

    // =========================================================================
    //  ATTACK Phase
    // =========================================================================

    private void handleAttackClick(Country clickedCountry) {
        if (sourceCountry == null) {
            if (canAttackFrom(clickedCountry)) {
                setSelection(clickedCountry, "Select target for " + clickedCountry.getName());
                Set<Country> targets = gameModel.getCurrentState().getValidTargets(clickedCountry);
                if (targets.isEmpty()) {
                    gameView.getControlPane().setMessage("No valid targets to attack from here!");
                } else {
                    gameView.getMapPane().highlightTargets(targets);
                }
            }
        } else {
            if (clickedCountry.equals(sourceCountry)) {
                clearSelection();
            } else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer())) {
                clearSelection();
            } else {
                performAttack(sourceCountry, clickedCountry);
                clearSelection();
            }
        }
    }

    private void performAttack(Country attacker, Country defender) {
        if (isMultiplayer) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ATTACK_REQ", attacker.getId());
            payload.put("DESTINATION_ID", defender.getId());
            payload.put("ATTACKER_ARMIES", attacker.getArmies());
            payload.put("DEFENDER_ARMIES", defender.getArmies());

            networkClient.sendAction(GameAction.ATTACK_REQ, networkClient.getRoomId(), payload);
        } else {
            BattleResult result = gameModel.attack(attacker, defender);
            if (result != null) {
                applyBattleResult(attacker, defender, result);
            } else {
                gameView.getControlPane().setMessage("Attack failed or invalid.");
            }
        }
    }

    private void applyBattleResult(Country attacker, Country defender, BattleResult result) {
        if (isMultiplayer) {
            attacker.removeArmies(result.attackerLosses());
            defender.removeArmies(result.defenderLosses());
        }
        boolean iAmTheAttacker = !isMultiplayer ||
                attacker.getOwner().getName().equals(networkClient.getPlayerName());

        if (iAmTheAttacker) {
            BattleResultDialog.show(result);
        }

        if (result.conquered()) {
            int minMove = isMultiplayer ? result.minMove() : 1;
            int maxMove = isMultiplayer ? result.maxMove() : attacker.getArmies() - 1;

            if (iAmTheAttacker) {
                int chosenAmount = showConquestMoveDialog(defender, minMove, maxMove);
                gameModel.handleConquest(attacker, defender, chosenAmount);

                if (isMultiplayer) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("CONQUEST_MOVE", attacker.getId());
                    payload.put("CONQUEST_DESTINATION", defender.getId());
                    payload.put("CONQUEST_AMOUNT", chosenAmount);
                    payload.put("MIN_MOVE", minMove);

                    pendingConquestHandled = true;
                    networkClient.sendAction(GameAction.CONQUEST_MOVE, networkClient.getRoomId(), payload);
                }
            } else {
                gameModel.handleConquest(attacker, defender, minMove);
            }

            gameView.getControlPane().setMessage("Territory Conquered!");

        } else {
            gameView.getControlPane().setMessage("Attack completed.");
        }
    }

    private void applyConquestMove(Country attacker, Country defender, int minMove, int totalMove) {
        if (pendingConquestHandled) {
            pendingConquestHandled = false;
            return;
        }
        int extra = totalMove - minMove;
        if (extra > 0) {
            attacker.removeArmies(extra);
            defender.addArmies(extra);
        }
    }

    private static int showConquestMoveDialog(Country conquered, int minMove, int maxMove) {
        java.util.List<Integer> choices = new java.util.ArrayList<>();
        for (int i = minMove; i <= maxMove; i++) choices.add(i);

        javafx.scene.control.ChoiceDialog<Integer> dialog =
                new javafx.scene.control.ChoiceDialog<>(maxMove, choices);
        dialog.setTitle("Victory!");
        dialog.setHeaderText("You conquered " + conquered.getName());
        dialog.setContentText("Choose how many armies to move:");

        return dialog.showAndWait().orElse(maxMove);
    }

    // =========================================================================
    //  AI Turn Management
    // =========================================================================

    private void checkAndExecuteAITurn() {
        if (gameModel.isGameOver()) {
            gameView.getControlPane().setMessage(
                    "🏆 GAME OVER! Winner: " + gameModel.getCurrentPlayer().getName() + " 🏆");
            return;
        }

        if (isCurrentPlayerAI()) {
            PauseTransition pause = buildAIPause();
            pause.play();
        }
    }

    private @NonNull PauseTransition buildAIPause() {
        Duration delay = (gameModel.getCurrentState() instanceof SetupState)
                ? Duration.seconds(0.05)
                : Duration.seconds(1);

        PauseTransition pause = new PauseTransition(delay);
        pause.setOnFinished(e -> {
            if (gameModel.isGameOver() || !isCurrentPlayerAI()) return;
            gameModel.getCurrentPlayer().playTurn(gameModel);
            clearSelection();
            checkAndExecuteAITurn();
        });
        return pause;
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

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