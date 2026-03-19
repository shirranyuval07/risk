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

    // משתני רשת
    private final RiskWebSocketClient networkClient;
    private final boolean isMultiplayer;

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
                    }
                    else if ("SETUP_PLACE".equals(action)) {
                        // מקבלים את ה-ID מההודעה, מוצאים את המדינה, ושמים עליה חייל
                        int countryId = Integer.parseInt(parts[1]);
                        Country c = gameModel.getBoard().getCountry(countryId);
                        executeSetupLocal(c);
                    }
                    else if ("DRAFT".equals(action)) {
                        int countryId = Integer.parseInt(parts[1]);
                        Country c = gameModel.getBoard().getCountry(countryId);
                        executeDraftLocal(c);
                    }
                    else if ("FORTIFY".equals(action)) {
                        int srcId = Integer.parseInt(parts[1]);
                        int dstId = Integer.parseInt(parts[2]);
                        int amount = Integer.parseInt(parts[3]);
                        Country src = gameModel.getBoard().getCountry(srcId);
                        Country dst = gameModel.getBoard().getCountry(dstId);
                        executeFortifyLocal(src, dst, amount);
                    }
                    // כאן נוסיף בהמשך את הטיפול בהודעות של ATTACK
                }
            });
        });
    }

    private void initializeListeners() {
        // 1. טיפול בקליקים על מדינות
        gameView.getMapPane().setOnCountryClick(clickedCountry -> {
            if (isCurrentPlayerAI()) return;
            // חסימה: אם זה מולטיפלייר וזה לא התור שלך, אי אפשר ללחוץ על המפה!
            if (isMultiplayer && !gameModel.getCurrentPlayer().getName().equals(networkClient.getPlayerName())) {
                gameView.getControlPane().setMessage("It's not your turn!");
                return;
            }

            if (clickedCountry != null) {
                onCountrySelected(clickedCountry);
            }
            gameView.getPlayerStatsPane().updateStats();
        });

        // 2. טיפול בכפתור מעבר שלב
        gameView.getControlPane().getBtnNextPhase().setOnAction(e -> {
            if (isCurrentPlayerAI()) return;
            if (isMultiplayer && !gameModel.getCurrentPlayer().getName().equals(networkClient.getPlayerName())) return;

            handleNextPhaseRequest();
            gameView.getPlayerStatsPane().updateStats();
        });

        // 3. טיפול בכפתור שמות המדינות
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
            // קוראים לפונקציה החדשה במקום לעשות את הלוגיקה פה
            handleSetupAction(clickedCountry);
        }
        else if (currentState instanceof DraftState) {
            handleDraftAction(clickedCountry);
        } else if (currentState instanceof AttackState) {
            handleAttackAction(clickedCountry);
        } else if (currentState instanceof FortifyState) {
            handleFortifyAction(clickedCountry);
        }
    }
// --- טיפול בהצבת חיילים בתחילת המשחק (SETUP) ---

    private void handleSetupAction(Country country) {
        if (isMultiplayer) {
            // אם אנחנו ברשת, נשלח הודעה לשרת עם ה-ID של המדינה
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
        }
        else
        {
            gameView.getControlPane().setMessage("Cannot place army here!");
        }
    }


    // --- טיפול בהצבת חיילים (DRAFT) ---

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

    // --- טיפול בהעברת שלב (NEXT PHASE) ---

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

    // --- טיפול בתגבורת (FORTIFY) ---

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
            if (clickedCountry.equals(sourceCountry))
                clearSelection();
            else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer())) {

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

    // --- טיפול בהתקפה (ATTACK) ---
    // הערה: כרגע מוגדר לעבוד בצורה מקומית גם במולטיפלייר עד שנפתור את סנכרון הקוביות

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
            if (clickedCountry.equals(sourceCountry))
                clearSelection();
            else {
                // TODO: בעתיד הקרוב נשנה את זה לשלוח בקשת התקפה לשרת
                BattleResult result = gameModel.attack(sourceCountry, clickedCountry);
                if (result != null) {
                    View.BattleResultDialog.show(result);

                    if (result.conquered()) {
                        int amountToMove = getAmountToMove(clickedCountry, result);
                        gameModel.handleConquest(sourceCountry, clickedCountry, amountToMove);
                        gameView.getControlPane().setMessage("Territory Conquered!");
                    } else {
                        gameView.getControlPane().setMessage("Attack completed.");
                    }
                    gameView.getPlayerStatsPane().updateStats();
                } else {
                    gameView.getControlPane().setMessage("Attack failed or invalid.");
                }
                clearSelection();
            }
        }
    }

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

    // --- מערכת התורות והבוטים ---

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
                // במולטיפלייר נצטרך לוודא שרק שחקן אחד (המארח) מריץ את הבוט כדי למנוע כפילויות
                // בינתיים השארתי את זה כפי שזה.
                gameModel.getCurrentPlayer().playTurn(gameModel);
                clearSelection();
                checkAndExecuteAITurn();
            }
        });
        return pause;
    }

    // --- פעולות עזר ---

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