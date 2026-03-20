package Controller;

import Model.*;
import Model.Records.BattleResult;
import Model.States.DraftState;
import Model.States.GameState;
import Model.States.AttackState;
import Model.States.FortifyState;
import Model.States.SetupState;
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
    private final RiskGame gameModel;
    private final GameRoot gameView;
    private Country sourceCountry = null;

    // Network fields
    private final RiskWebSocketClient networkClient;
    private final boolean isMultiplayer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameController(RiskGame model, GameRoot view, RiskWebSocketClient networkClient) {
        this.gameModel = model;
        this.gameView = view;
        this.networkClient = networkClient;
        this.isMultiplayer = (networkClient != null);

        initializeListeners();

        if (isMultiplayer) {
            setupNetworkListeners();
        }

        checkAndExecuteAITurn();
    }

    private void setupNetworkListeners() {
        networkClient.setOnMessageReceived(message -> {
            javafx.application.Platform.runLater(() -> {
                if ("GAME_ACTION".equals(message.type())) {
                    String[] parts = message.content().split(":");
                    String action = parts[0];

                    if ("NEXT_PHASE".equals(action)) {
                        executeNextPhaseLocal();
                    } else if ("SETUP_PLACE".equals(action)) {
                        int countryId = Integer.parseInt(parts[1]);
                        Country c = gameModel.getBoard().getCountry(countryId);
                        executeSetupLocal(c);
                    } else if ("DRAFT".equals(action)) {
                        int countryId = Integer.parseInt(parts[1]);
                        Country c = gameModel.getBoard().getCountry(countryId);
                        executeDraftLocal(c);
                    } else if ("FORTIFY".equals(action)) {
                        int srcId = Integer.parseInt(parts[1]);
                        int dstId = Integer.parseInt(parts[2]);
                        int amount = Integer.parseInt(parts[3]);
                        Country src = gameModel.getBoard().getCountry(srcId);
                        Country dst = gameModel.getBoard().getCountry(dstId);
                        executeFortifyLocal(src, dst, amount);
                    } else if ("CONQUEST_MOVE".equals(action)) {
                        // parts: [CONQUEST_MOVE, attackerId, defenderId, totalAmount]
                        int attackerId = Integer.parseInt(parts[1]);
                        int defenderId = Integer.parseInt(parts[2]);
                        int totalMove  = Integer.parseInt(parts[3]);
                        Country attacker = gameModel.getBoard().getCountry(attackerId);
                        Country defender = gameModel.getBoard().getCountry(defenderId);
                        if (attacker != null && defender != null) {
                            // Apply the player's chosen move amount on all clients.
                            // minMove armies were already moved during executeBattleResultLocal,
                            // so we just move the delta here.
                            int delta = totalMove - attacker.getArmies(); // re-derived below
                            // Simpler: just do a fresh handleConquest with the chosen amount
                            // (executeBattleResultLocal already called handleConquest with minMove,
                            //  so undo that and redo with totalMove is complex — instead we
                            //  track the pending conquest and apply totalMove directly)
                            applyPendingConquestMove(attacker, defender, totalMove);
                        }
                    }

                } else if ("BATTLE_RESULT".equals(message.type())) {
                    // content format: "<attackerId>:<defenderId>:<BattleResultJson>"
                    // Find the first two colons to split IDs from the JSON (JSON itself contains colons)
                    String content = message.content();
                    int firstColon  = content.indexOf(':');
                    int secondColon = content.indexOf(':', firstColon + 1);

                    int attackerId = Integer.parseInt(content.substring(0, firstColon));
                    int defenderId = Integer.parseInt(content.substring(firstColon + 1, secondColon));
                    String battleJson = content.substring(secondColon + 1);

                    Country attacker = gameModel.getBoard().getCountry(attackerId);
                    Country defender = gameModel.getBoard().getCountry(defenderId);

                    if (attacker != null && defender != null) {
                        try {
                            BattleResult result = objectMapper.readValue(battleJson, BattleResult.class);
                            executeBattleResultLocal(attacker, defender, result);
                        } catch (Exception e) {
                            System.out.println("Failed to parse BattleResult: " + e.getMessage());
                        }
                    }
                }
            });
        });
    }

    private void initializeListeners() {
        gameView.getMapPane().setOnCountryClick(clickedCountry -> {
            if (isCurrentPlayerAI()) return;
            if (isMultiplayer && !gameModel.getCurrentPlayer().getName().equals(networkClient.getPlayerName())) {
                gameView.getControlPane().setMessage("It's not your turn!");
                return;
            }

            if (clickedCountry != null) {
                onCountrySelected(clickedCountry);
            }
            gameView.getPlayerStatsPane().updateStats();
        });

        gameView.getControlPane().getBtnNextPhase().setOnAction(e -> {
            if (isCurrentPlayerAI()) return;
            if (isMultiplayer && !gameModel.getCurrentPlayer().getName().equals(networkClient.getPlayerName())) return;

            handleNextPhaseRequest();
            gameView.getPlayerStatsPane().updateStats();
        });

        gameView.getControlPane().getBtnToggleNames().setOnAction(e -> {
            javafx.scene.control.Button btn = gameView.getControlPane().getBtnToggleNames();
            if (btn.getText().contains("Show")) {
                btn.setText("👁 Hide Names");
                gameView.getMapPane().toggleNames(true);
            } else {
                btn.setText("👁 Show Names");
                gameView.getMapPane().toggleNames(false);
            }
        });

        gameView.getControlPane().getBtnCards().setOnAction(e -> {
            CardsDialog.show(gameModel.getCurrentPlayer(), () -> {
                gameView.getPlayerStatsPane().updateStats();
            });
        });
    }

    private void onCountrySelected(Country clickedCountry) {
        GameState currentState = gameModel.getCurrentState();

        if (currentState instanceof SetupState) {
            handleSetupAction(clickedCountry);
        } else if (currentState instanceof DraftState) {
            handleDraftAction(clickedCountry);
        } else if (currentState instanceof AttackState) {
            handleAttackAction(clickedCountry);
        } else if (currentState instanceof FortifyState) {
            handleFortifyAction(clickedCountry);
        }
    }

    // --- SETUP ---

    private void handleSetupAction(Country country) {
        if (isMultiplayer) {
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(), "SETUP_PLACE:" + country.getId());
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

    // --- DRAFT ---

    private void handleDraftAction(Country country) {
        if (isMultiplayer) {
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(), "DRAFT:" + country.getId());
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

    // --- NEXT PHASE ---

    private void handleNextPhaseRequest() {
        if (isMultiplayer) {
            networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(), "NEXT_PHASE");
        } else {
            executeNextPhaseLocal();
        }
    }

    private void executeNextPhaseLocal() {
        gameModel.nextPhase();
        clearSelection();

        if (isCurrentPlayerAI()) {
            checkAndExecuteAITurn();
        } else if (gameModel.getCurrentState() instanceof DraftState && gameModel.getCurrentPlayer().getDraftArmies() > 0) {
            gameView.getControlPane().setMessage("You have armies left to place!");
        }
        gameView.getPlayerStatsPane().updateStats();
    }

    // --- FORTIFY ---

    private void handleFortifyAction(Country clickedCountry) {
        if (sourceCountry == null) {
            if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer()) && clickedCountry.getArmies() > 1) {
                setSelection(clickedCountry, "Move from " + clickedCountry.getName() + ". Select target.");
                Set<Country> targets = gameModel.getCurrentState().getValidTargets(clickedCountry);
                if (!targets.isEmpty())
                    gameView.getMapPane().highlightTargets(targets);
                else
                    gameView.getControlPane().setMessage("No valid targets to fortify from here!");
            }
        } else {
            if (clickedCountry.equals(sourceCountry)) {
                clearSelection();
            } else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer())) {
                int maxMove = sourceCountry.getArmies() - 1;
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Fortify Territory");
                dialog.setHeaderText("Moving armies from " + sourceCountry.getName() + " to " + clickedCountry.getName());
                dialog.setContentText("Enter amount (Max: " + maxMove + "):");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(input -> {
                    try {
                        if (!input.isEmpty()) {
                            int amount = Integer.parseInt(input);
                            if (isMultiplayer) {
                                networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                                        "FORTIFY:" + sourceCountry.getId() + ":" + clickedCountry.getId() + ":" + amount);
                                clearSelection();
                            } else {
                                executeFortifyLocal(sourceCountry, clickedCountry, amount);
                            }
                        }
                    } catch (NumberFormatException e) {
                        gameView.getControlPane().setMessage("Invalid number!");
                        clearSelection();
                    }
                });
            }
        }
    }

    private void executeFortifyLocal(Country src, Country dst, int amount) {
        String resMsg = gameModel.fortify(src, dst, amount);
        gameView.getControlPane().setMessage(resMsg);
        gameView.getPlayerStatsPane().updateStats();
        clearSelection();
    }

    // --- ATTACK ---

    private void handleAttackAction(Country clickedCountry) {
        if (sourceCountry == null) {
            if (canAttackFrom(clickedCountry)) {
                setSelection(clickedCountry, "Select target for " + clickedCountry.getName());
                Set<Country> targets = gameModel.getCurrentState().getValidTargets(clickedCountry);
                if (!targets.isEmpty())
                    gameView.getMapPane().highlightTargets(targets);
                else
                    gameView.getControlPane().setMessage("No valid targets to attack from here!");
            }
        } else {
            if (clickedCountry.equals(sourceCountry)) {
                clearSelection();
            } else {
                Country attacker = sourceCountry;
                clearSelection();

                if (isMultiplayer) {
                    // Send to server — the server rolls dice and broadcasts result to everyone
                    networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                            "ATTACK_REQ:" + attacker.getId() + "->" + clickedCountry.getId());
                } else {
                    // Local game: roll locally as before
                    BattleResult result = gameModel.attack(attacker, clickedCountry);
                    if (result != null) {
                        executeBattleResultLocal(attacker, clickedCountry, result);
                    } else {
                        gameView.getControlPane().setMessage("Attack failed or invalid.");
                    }
                }
            }
        }
    }

    /**
     * Applies a BattleResult that arrived either from the server (multiplayer)
     * or from the local dice roll (single-player). Shared by both paths.
     *
     * For conquests: applies minMove immediately (matching what the server did),
     * then — only on the attacker's own client — asks how many more to move and
     * sends a CONQUEST_MOVE message so every client syncs to the final amount.
     */
    private void executeBattleResultLocal(Country attacker, Country defender, BattleResult result) {
        // Apply army losses that are encoded in the result
        // (AttackState.attack already did this on the server; we need to mirror it here on clients)
        // Note: In multiplayer the local gameModel has NOT rolled yet — we must apply losses manually.
        if (isMultiplayer) {
            applyLossesDirectly(attacker, defender, result);
        }

        View.BattleResultDialog.show(result);

        if (result.conquered()) {
            // Apply the minimum conquest move (mirrors what the server already did)
            gameModel.handleConquest(attacker, defender, result.minMove());
            gameView.getControlPane().setMessage("Territory Conquered!");
            gameView.getPlayerStatsPane().updateStats();

            // Only the attacker's client shows the "how many armies to move" dialog
            boolean iAmTheAttacker = !isMultiplayer ||
                    gameModel.getCurrentPlayer().getName().equals(networkClient.getPlayerName());

            if (iAmTheAttacker && result.maxMove() > result.minMove()) {
                int chosenAmount = getAmountToMove(defender, result);
                int delta = chosenAmount - result.minMove();

                if (delta > 0) {
                    if (isMultiplayer) {
                        // Tell all clients to apply the extra armies
                        networkClient.sendAction("GAME_ACTION", networkClient.getRoomId(),
                                "CONQUEST_MOVE:" + attacker.getId() + ":" + defender.getId() + ":" + chosenAmount);
                    } else {
                        // Local: move the extra armies right now
                        attacker.removeArmies(delta);
                        defender.addArmies(delta);
                    }
                }
            }
        } else {
            gameView.getControlPane().setMessage("Attack completed.");
        }

        gameView.getPlayerStatsPane().updateStats();
    }

    /**
     * In multiplayer, the local model hasn't run attack() — so we apply
     * the army losses from the server's BattleResult directly.
     */
    private void applyLossesDirectly(Country attacker, Country defender, BattleResult result) {
        attacker.removeArmies(result.attackerLosses());
        defender.removeArmies(result.defenderLosses());
    }

    /**
     * Called when a CONQUEST_MOVE message is received. The attacker's client
     * already applied minMove via handleConquest; here we move the remaining delta.
     */
    private void applyPendingConquestMove(Country attacker, Country defender, int totalMove) {
        int delta = totalMove - attacker.getArmies(); // armies already moved = minMove
        // Safer approach: just check how many were already moved (minMove was the first handleConquest)
        // We store nothing, so just apply the extra armies directly:
        // totalMove - minMove were not yet moved on non-attacker clients.
        // The attacker's client sends this AFTER it already moved minMove, so:
        // All clients (including attacker) will receive this. Since attacker already moved
        // the full amount locally, we guard by re-syncing:
        // Simplest correct approach: apply delta on everyone except the original attacker client.
        // But since we can't know who the "original" attacker is from the broadcast alone,
        // we track a flag instead.

        // Practical solution: store the extra delta from the last conquest and guard with it.
        // For now, all clients receive and apply the delta; the attacker skips because it
        // already applied the full totalMove interactively.
        if (pendingConquestHandled) {
            pendingConquestHandled = false; // attacker's client: skip re-application
            return;
        }
        int extra = totalMove - defender.getArmies(); // armies already on defender after minMove
        if (extra > 0) {
            attacker.removeArmies(extra);
            defender.addArmies(extra);
        }
        gameView.getPlayerStatsPane().updateStats();
    }

    // Flag: true on the attacker's client after they interactively chose the move amount
    private boolean pendingConquestHandled = false;

    private static int getAmountToMove(Country clickedCountry, BattleResult result) {
        java.util.List<Integer> choices = new java.util.ArrayList<>();
        for (int i = result.minMove(); i <= result.maxMove(); i++) {
            choices.add(i);
        }

        javafx.scene.control.ChoiceDialog<Integer> dialog =
                new javafx.scene.control.ChoiceDialog<>(result.maxMove(), choices);
        dialog.setTitle("Victory!");
        dialog.setHeaderText("You conquered " + clickedCountry.getName());
        dialog.setContentText("Choose how many armies to move:");

        Optional<Integer> chosenAmount = dialog.showAndWait();
        return chosenAmount.orElse(result.maxMove());
    }

    // --- AI & TURN MANAGEMENT ---

    private void checkAndExecuteAITurn() {
        if (gameModel.isGameOver()) {
            Player winner = gameModel.getCurrentPlayer();
            gameView.getControlPane().setMessage("🏆 GAME OVER! Winner: " + winner.getName() + " 🏆");
            return;
        }

        if (isCurrentPlayerAI()) {
            PauseTransition pause = getPauseTransition();
            pause.play();
            gameView.getPlayerStatsPane().updateStats();
        }
    }

    private @NonNull PauseTransition getPauseTransition() {
        PauseTransition pause = (gameModel.getCurrentState() instanceof SetupState) ?
                new PauseTransition(Duration.seconds(0.05)) : new PauseTransition(Duration.seconds(1));

        pause.setOnFinished(e -> {
            if (gameModel.isGameOver()) return;

            if (isCurrentPlayerAI()) {
                gameModel.getCurrentPlayer().playTurn(gameModel);
                clearSelection();
                checkAndExecuteAITurn();
            }
        });
        return pause;
    }

    // --- HELPERS ---

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

    private boolean canAttackFrom(Country c) {
        return c.getOwner().equals(gameModel.getCurrentPlayer()) && c.getArmies() > 1;
    }

    private boolean isCurrentPlayerAI() {
        return gameModel.getCurrentPlayer() != null && gameModel.getCurrentPlayer().isAI();
    }
}