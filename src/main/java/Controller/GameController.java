package Controller;

import Model.*; // ייבוא כל מחלקות המודל כולל המצבים (DraftState, AttackState וכו')
import Model.Records.BattleResult;
import Model.States.DraftState;
import Model.States.GameState;
import Model.States.AttackState;
import Model.States.FortifyState;
import View.GameRoot;

import javafx.animation.PauseTransition;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;

import java.util.Optional;
import java.util.Set;

import Model.States.SetupState;
import org.jspecify.annotations.NonNull;

public class GameController {
    private final RiskGame gameModel;
    private final GameRoot gameView;
    private Country sourceCountry = null;

    public GameController(RiskGame model, GameRoot view) {
        this.gameModel = model;
        this.gameView = view;

        initializeListeners();
        checkAndExecuteAITurn();
    }

    private void initializeListeners() {
        // 1. טיפול בקליקים על מדינות
        gameView.getMapPane().setOnCountryClick(clickedCountry -> {
            if (isCurrentPlayerAI()) return;
            if (clickedCountry != null) {
                onCountrySelected(clickedCountry);
            }
        });

        // 2. טיפול בכפתור מעבר שלב
        gameView.getControlPane().getBtnNextPhase().setOnAction(e -> {
            if (isCurrentPlayerAI()) return;
            handleNextPhaseRequest();
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
    }

    private void onCountrySelected(Country clickedCountry) {
        // שליפת אובייקט הסטייט הנוכחי מהמודל
        GameState currentState = gameModel.getCurrentState();

        // זיהוי פולימורפי של השלב הנוכחי וניתוב לפעולה המתאימה ב-Controller
        if (currentState instanceof SetupState) {

            if (gameModel.placeArmy(clickedCountry)) {
                gameView.getControlPane().setMessage("Placed army on " + clickedCountry.getName());
                checkAndExecuteAITurn(); // Kickstart the next player if it's an AI
            } else {
                gameView.getControlPane().setMessage("Cannot place army here!");
            }
        } else if (currentState instanceof DraftState) {
            handleDraftAction(clickedCountry);
        } else if (currentState instanceof AttackState) {
            handleAttackAction(clickedCountry);
        } else if (currentState instanceof FortifyState) {
            handleFortifyAction(clickedCountry);
        }
    }

    private void handleDraftAction(Country country) {
        // ה-Controller קורא למודל, והמודל מעביר את הבקשה ל-DraftState
        if (gameModel.placeArmy(country)) {
            gameView.getControlPane().setMessage("Deployed 1 army to " + country.getName());
        } else {
            gameView.getControlPane().setMessage("Cannot deploy here!");
        }
    }

    private void handleAttackAction(Country clickedCountry) {
        if (sourceCountry == null)
        {

            if (canAttackFrom(clickedCountry))
            {
                setSelection(clickedCountry, "Select target for " + clickedCountry.getName());
                Set<Country> targets = gameModel.getCurrentState().getValidTargets(clickedCountry);
                if(!targets.isEmpty())
                    gameView.getMapPane().highlightTargets(targets);
                else
                    gameView.getControlPane().setMessage("No valid targets to attack from here!");
            }
        }
        else
        {
            if (clickedCountry.equals(sourceCountry))
                clearSelection();
            else
            {
                // המודל יעביר את הבקשה ל-AttackState
                BattleResult result = gameModel.attack(sourceCountry, clickedCountry);
                if (result != null)
                {
                    View.BattleResultDialog.show(result);

                    if (result.conquered())
                    {
                        int amountToMove = getAmountToMove(clickedCountry, result);
                        gameModel.handleConquest(sourceCountry, clickedCountry, amountToMove);

                        gameView.getControlPane().setMessage("Territory Conquered!");
                    } else
                    {
                        gameView.getControlPane().setMessage("Attack completed.");
                    }
                }
                else
                    gameView.getControlPane().setMessage("Attack failed or invalid.");

                clearSelection();
            }
        }
    }

    private static int getAmountToMove(Country clickedCountry, BattleResult result) {
        java.util.List<Integer> choices = new java.util.ArrayList<>();
        for (int i = result.minMove(); i <= result.maxMove(); i++) {
            choices.add(i);
        }

        // הצגת חלון בחירה למשתמש
        javafx.scene.control.ChoiceDialog<Integer> dialog =
                new javafx.scene.control.ChoiceDialog<>(result.maxMove(), choices);
        dialog.setTitle("Victory!");
        dialog.setHeaderText("You conquered " + clickedCountry.getName());
        dialog.setContentText("Choose how many armies to move:");

        Optional<Integer> chosenAmount = dialog.showAndWait();

        // ביצוע ההעברה בפועל דרך המודל עם הכמות שנבחרה (או המקסימום אם השחקן סגר את החלון)
        return chosenAmount.orElse(result.maxMove());
    }

    private void handleFortifyAction(Country clickedCountry) {
        if (sourceCountry == null)
        {
            if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer()) && clickedCountry.getArmies() > 1)
            {
                setSelection(clickedCountry, "Move from " + clickedCountry.getName() + ". Select target.");
                Set<Country> targets = gameModel.getCurrentState().getValidTargets(clickedCountry);
                if(!targets.isEmpty())
                    gameView.getMapPane().highlightTargets(targets);
                else
                    gameView.getControlPane().setMessage("No valid targets to attack from here!");
            }
        }
        else
        {
            if (clickedCountry.equals(sourceCountry))
                clearSelection();
            else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer()))
                executeFortifyMove(clickedCountry);
        }
    }

    private void executeFortifyMove(Country target) {
        int maxMove = sourceCountry.getArmies() - 1;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fortify Territory");
        dialog.setHeaderText("Moving armies from " + sourceCountry.getName() + " to " + target.getName());
        dialog.setContentText("Enter amount (Max: " + maxMove + "):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                if (!input.isEmpty()) {
                    int amount = Integer.parseInt(input);
                    // ה-Controller שולח למודל, שמטופל על ידי FortifyState
                    String resMsg = gameModel.fortify(sourceCountry, target, amount);
                    gameView.getControlPane().setMessage(resMsg);
                }
            } catch (NumberFormatException e) {
                gameView.getControlPane().setMessage("Invalid number!");
            }
        });
        clearSelection();
    }

    private void handleNextPhaseRequest() {
        gameModel.nextPhase(); // מחליף את הסטייט הפנימי במודל
        clearSelection();

        if (isCurrentPlayerAI()) {
            checkAndExecuteAITurn();
        } else if (gameModel.getCurrentState() instanceof DraftState && gameModel.getCurrentPlayer().getDraftArmies() > 0) {
            gameView.getControlPane().setMessage("You have armies left to place!");
        }
    }

    // הפעלת הבוט
    private void checkAndExecuteAITurn() {
        // 1. קודם כל, אם המשחק נגמר - תעצור הכל ותכתוב מי ניצח!
        if (gameModel.isGameOver()) {
            Player winner = gameModel.getCurrentPlayer(); // השחקן האחרון שנשאר
            gameView.getControlPane().setMessage("🏆 GAME OVER! Winner: " + winner.getName() + " 🏆");
            return; // השורה הזו שוברת את הלולאה האינסופית
        }

        if (isCurrentPlayerAI()) {
            PauseTransition pause = getPauseTransition();
            pause.play();
        }
    }

    private @NonNull PauseTransition getPauseTransition() {
        PauseTransition pause = (gameModel.getCurrentState() instanceof SetupState) ? new PauseTransition(Duration.seconds(0.05)):new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {

            // 2. הגנת ביטחון במקרה שהמשחק נגמר תוך כדי ההשהייה
            if (gameModel.isGameOver()) return;

            if (isCurrentPlayerAI()) {
                gameModel.getCurrentPlayer().playTurn(gameModel);
                clearSelection();
                checkAndExecuteAITurn();
            }
        });
        return pause;
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

    private boolean canAttackFrom(Country c) {
        return c.getOwner().equals(gameModel.getCurrentPlayer()) && c.getArmies() > 1;
    }

    private boolean isCurrentPlayerAI() {
        return gameModel.getCurrentPlayer() != null && gameModel.getCurrentPlayer().isAI();
    }

}