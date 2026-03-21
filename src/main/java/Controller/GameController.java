package Controller;

import Model.*;
import Model.Records.BattleResult;
import Model.States.*;
import View.CardsDialog;
import View.GameRoot;

import com.example.demo.RiskWebSocketClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.PauseTransition;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.Set;

public class GameController {

    // =========================================================================
    //  Fields
    // =========================================================================

    private final RiskGame gameModel;
    private final GameRoot gameView;
    private final RiskWebSocketClient networkClient;
    private final boolean isMultiplayer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Tracks the first country selected during Attack / Fortify
    private Country sourceCountry = null;

    // Prevents the attacker's client from double-applying conquest army moves
    private boolean pendingConquestHandled = false;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public GameController(RiskGame model, GameRoot view, RiskWebSocketClient networkClient) {
        this.gameModel = model;
        this.gameView = view;
        this.networkClient = networkClient;
        this.isMultiplayer = (networkClient != null);

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
            gameView.getPlayerStatsPane().updateStats();
        });

        // Next Phase button
        gameView.getControlPane().getBtnNextPhase().setOnAction(e -> {
            if (isCurrentPlayerAI()) return;
            if (isMultiplayer && !isMyTurn()) return;
            handleNextPhaseRequest();
            gameView.getPlayerStatsPane().updateStats();
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
                    switch (message.type()) {
                        case "GAME_ACTION"   -> handleIncomingGameAction(message.content());
                        case "BATTLE_RESULT" -> handleIncomingBattleResult(message.content());
                    }
                })
        );
    }

    private void handleIncomingGameAction(String content) {
        String[] parts = content.split(":");
        String action = parts[0];

        switch (action) {
            case "NEXT_PHASE"    -> executeNextPhaseLocal();
            case "SETUP_PLACE"   -> executeSetupLocal(getCountry(parts[1]));
            case "DRAFT"         -> executeDraftLocal(getCountry(parts[1]));
            case "NEXT_TURN"     -> executeNextTurnLocal(parts[1], Integer.parseInt(parts[2]));
            case "FORTIFY"       -> executeFortifyLocal(
                    getCountry(parts[1]),
                    getCountry(parts[2]),
                    Integer.parseInt(parts[3]));
            case "CONQUEST_MOVE" -> applyConquestMove(
                    getCountry(parts[1]),
                    getCountry(parts[2]),
                    Integer.parseInt(parts[3]));
        }
    }

    private void handleIncomingBattleResult(String content) {
        // Content format: "<attackerId>:<defenderId>:<BattleResultJson>"
        // We split carefully because the JSON itself contains colons
        int firstColon  = content.indexOf(':');
        int secondColon = content.indexOf(':', firstColon + 1);

        Country attacker  = getCountry(content.substring(0, firstColon));
        Country defender  = getCountry(content.substring(firstColon + 1, secondColon));
        String battleJson = content.substring(secondColon + 1);

        if (attacker == null || defender == null) return;

        try {
            BattleResult result = objectMapper.readValue(battleJson, BattleResult.class);
            applyBattleResult(attacker, defender, result);
        } catch (Exception e) {
            System.out.println("Failed to parse BattleResult: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Phase Routing — decides which handler to call based on current phase
    // =========================================================================

    private void handleCountryClick(Country clickedCountry) {
        if      (gameModel.getCurrentState() instanceof SetupState)   handleSetupClick(clickedCountry);
        else if (gameModel.getCurrentState() instanceof DraftState)   handleDraftClick(clickedCountry);
        else if (gameModel.getCurrentState() instanceof AttackState)  handleAttackClick(clickedCountry);
        else if (gameModel.getCurrentState() instanceof FortifyState) handleFortifyClick(clickedCountry);
    }

    // =========================================================================
    //  SETUP Phase
    // =========================================================================

    private void handleSetupClick(Country country) {
        if (isMultiplayer) {
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                    "SETUP_PLACE:" + country.getId());
        } else {
            executeSetupLocal(country);
        }
    }

    private void executeSetupLocal(Country country) {
        if (gameModel.placeArmy(country)) {
            gameView.getControlPane().setMessage("Placed army on " + country.getName());
            gameView.getPlayerStatsPane().updateStats();
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
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                    "DRAFT:" + country.getId());
        } else {
            executeDraftLocal(country);
        }
    }

    private void executeDraftLocal(Country country) {
        if (gameModel.placeArmy(country)) {
            gameView.getControlPane().setMessage("Deployed 1 army to " + country.getName());
            gameView.getPlayerStatsPane().updateStats();
        } else {
            gameView.getControlPane().setMessage("Cannot deploy here!");
        }
    }

    // =========================================================================
    //  NEXT PHASE / NEXT TURN
    // =========================================================================

    private void handleNextPhaseRequest() {
        if (isMultiplayer) {
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(), "NEXT_PHASE");
        } else {
            executeNextPhaseLocal();
        }
    }

    private void executeNextPhaseLocal() {
        // Capture phase BEFORE advancing, because FortifyState.nextPhase()
        // calls nextTurn() internally — so the state changes inside nextPhase()
        boolean wasFortify = gameModel.getCurrentState() instanceof FortifyState;

        gameModel.nextPhase();
        clearSelection();

        // After fortify ends a turn, broadcast the new player's turn info to all clients
        if (isMultiplayer && gameModel.getCurrentState() instanceof DraftState && !wasFortify) {
            broadcastNextTurnIfNeeded();
        }

        if (isCurrentPlayerAI()) {
            checkAndExecuteAITurn();
        } else if (gameModel.getCurrentState() instanceof DraftState
                && gameModel.getCurrentPlayer().getDraftArmies() > 0) {
            gameView.getControlPane().setMessage("You have armies left to place!");
        }

        gameView.getPlayerStatsPane().updateStats();
    }

    /** Sends NEXT_TURN to all clients if the new current player is not me */
    private void broadcastNextTurnIfNeeded() {
        Player newCurrentPlayer = gameModel.getCurrentPlayer();
        if (!newCurrentPlayer.getName().equals(networkClient.getPlayerName())) {
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                    "NEXT_TURN:" + newCurrentPlayer.getName() + ":" + newCurrentPlayer.getDraftArmies());
        }
    }

    /** Called on all clients when a NEXT_TURN message is received */
    private void executeNextTurnLocal(String playerName, int draftArmies) {
        gameModel.nextTurn();

        // Override the draft army count with the authoritative value from the sender
        for (Player p : gameModel.getPlayers()) {
            if (p.getName().equals(playerName)) {
                p.setDraftArmies(draftArmies);
                break;
            }
        }

        clearSelection();
        gameView.getPlayerStatsPane().updateStats();
    }

    // =========================================================================
    //  FORTIFY Phase
    // =========================================================================

    private void handleFortifyClick(Country clickedCountry) {
        if (sourceCountry == null) {
            // First click — select source territory
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
            // Second click — confirm move or deselect
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
                        networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                                "FORTIFY:" + sourceCountry.getId() + ":" + destination.getId() + ":" + amount);
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
        gameView.getPlayerStatsPane().updateStats();
        clearSelection();
        checkAndExecuteAITurn(); // fortify ends the turn, so trigger AI if it's their turn next
    }

    // =========================================================================
    //  ATTACK Phase
    // =========================================================================

    private void handleAttackClick(Country clickedCountry) {
        if (sourceCountry == null) {
            // First click — select attacker
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
            // Second click — confirm attack or deselect
            if (clickedCountry.equals(sourceCountry)) {
                clearSelection();
            } else {
                performAttack(sourceCountry, clickedCountry);
                clearSelection();
            }
        }
    }

    private void performAttack(Country attacker, Country defender) {
        if (isMultiplayer) {
            // Send to server — server rolls dice and broadcasts result to all clients
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                    "ATTACK_REQ:" + attacker.getId() + "->" + defender.getId()
                            + ":" + attacker.getArmies() + ":" + defender.getArmies());
        } else {
            BattleResult result = gameModel.attack(attacker, defender);
            if (result != null) {
                applyBattleResult(attacker, defender, result);
            } else {
                gameView.getControlPane().setMessage("Attack failed or invalid.");
            }
        }
    }

    /**
     * Applies a BattleResult on this client — used both in local and multiplayer mode.
     * In multiplayer, army losses are applied manually since the local model never rolled.
     * In local mode, AttackState.attack() already applied the losses.
     */
    private void applyBattleResult(Country attacker, Country defender, BattleResult result) {
        if(gameModel.getCurrentPlayer().equals(attacker.getOwner()))
        {
            View.BattleResultDialog.show(result);
        }

        if (result.conquered()) {
            gameModel.handleConquest(attacker, defender, result.minMove());
            gameView.getControlPane().setMessage("Territory Conquered!");
            gameView.getPlayerStatsPane().updateStats();

            // Only the attacker's client asks how many armies to move
            if (isMyTurn() && result.maxMove() > result.minMove()) {
                int chosenAmount = showConquestMoveDialog(defender, result);
                int extraArmies  = chosenAmount - result.minMove();

                if (extraArmies > 0) {
                    if (isMultiplayer) {
                        networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                                "CONQUEST_MOVE:" + attacker.getId() + ":" + defender.getId() + ":" + chosenAmount);
                    } else {
                        attacker.removeArmies(extraArmies);
                        defender.addArmies(extraArmies);
                    }
                }
            }
        } else {
            gameView.getControlPane().setMessage("Attack completed.");
        }

        gameView.getPlayerStatsPane().updateStats();
    }

    /**
     * Called when a CONQUEST_MOVE message arrives.
     * The attacker already applied the move locally, so they skip re-applying it.
     */
    private void applyConquestMove(Country attacker, Country defender, int totalMove) {
        if (pendingConquestHandled) {
            pendingConquestHandled = false;
            return;
        }
        int extra = totalMove - defender.getArmies();
        if (extra > 0) {
            attacker.removeArmies(extra);
            defender.addArmies(extra);
        }
        gameView.getPlayerStatsPane().updateStats();
    }

    private static int showConquestMoveDialog(Country conquered, BattleResult result) {
        java.util.List<Integer> choices = new java.util.ArrayList<>();
        for (int i = result.minMove(); i <= result.maxMove(); i++) choices.add(i);

        javafx.scene.control.ChoiceDialog<Integer> dialog =
                new javafx.scene.control.ChoiceDialog<>(result.maxMove(), choices);
        dialog.setTitle("Victory!");
        dialog.setHeaderText("You conquered " + conquered.getName());
        dialog.setContentText("Choose how many armies to move:");

        return dialog.showAndWait().orElse(result.maxMove());
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
            gameView.getPlayerStatsPane().updateStats();
        }
    }

    private @NonNull PauseTransition buildAIPause() {
        // The setup phase uses a very short delay; normal turns use 1 second so the player can watch
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

    /** Returns true if it's currently this client's player's turn */
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

    /** Convenience method to look up a country by its ID string */
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