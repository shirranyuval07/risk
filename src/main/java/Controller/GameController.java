package Controller;

import Model.Country;
import Model.Player;
import Model.RiskGame;
import Model.Phase;
import View.GameRoot;

import javafx.animation.PauseTransition;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;

import java.util.Optional;

public class GameController {
    private final RiskGame gameModel;
    private final GameRoot gameView;
    private Country sourceCountry = null;

    public GameController(RiskGame model, GameRoot view) {
        this.gameModel = model;
        this.gameView = view;

        // רישום הקונטרולר כמאזין למודל
        this.gameModel.addObserver(this::updateGameView);

        initializeListeners();
        checkAndExecuteAITurn();
        updateGameView();
    }

    private void initializeListeners() {
        // 1. טיפול בקליקים על מדינות (JavaFX Style!)
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
    }

    private void onCountrySelected(Country clickedCountry) {
        Phase currentPhase = gameModel.getCurrentPhase();
        switch (currentPhase) {
            case DRAFT: handleDraftAction(clickedCountry); break;
            case ATTACK: handleAttackAction(clickedCountry); break;
            case FORTIFY: handleFortifyAction(clickedCountry); break;
        }
    }

    private void handleDraftAction(Country country) {
        if (gameModel.placeArmy(country)) {
            gameView.getControlPane().setMessage("Deployed 1 army to " + country.getName());
        } else {
            gameView.getControlPane().setMessage("Cannot deploy here!");
        }
    }

    private void handleAttackAction(Country clickedCountry) {
        if (sourceCountry == null) {
            if (canAttackFrom(clickedCountry)) {
                setSelection(clickedCountry, "Select target for " + clickedCountry.getName());
            }
        } else {
            if (clickedCountry.equals(sourceCountry)) {
                clearSelection();
            } else {
                String result = gameModel.attack(sourceCountry, clickedCountry);
                gameView.getControlPane().setMessage(result);
                clearSelection();
            }
        }
    }

    private void handleFortifyAction(Country clickedCountry) {
        if (sourceCountry == null) {
            if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer()) && clickedCountry.getArmies() > 1) {
                setSelection(clickedCountry, "Move from " + clickedCountry.getName() + ". Select target.");
            }
        } else {
            if (clickedCountry.equals(sourceCountry)) {
                clearSelection();
            } else if (clickedCountry.getOwner().equals(gameModel.getCurrentPlayer())) {
                executeFortifyMove(clickedCountry);
            }
        }
    }

    private void executeFortifyMove(Country target) {
        int maxMove = sourceCountry.getArmies() - 1;

        // חלון קלט מותאם ל-JavaFX במקום JOptionPane
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fortify Territory");
        dialog.setHeaderText("Moving armies from " + sourceCountry.getName() + " to " + target.getName());
        dialog.setContentText("Enter amount (Max: " + maxMove + "):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                if (!input.isEmpty()) {
                    int amount = Integer.parseInt(input);
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
        gameModel.nextPhase();
        clearSelection();

        if (isCurrentPlayerAI()) {
            checkAndExecuteAITurn();
        } else if (gameModel.getCurrentPhase() == Phase.DRAFT && gameModel.getCurrentPlayer().getDraftArmies() > 0) {
            gameView.getControlPane().setMessage("You have armies left to place!");
        }
    }

    // הפעלת הבוט עם טיימר מודרני (PauseTransition)
    private void checkAndExecuteAITurn() {
        if (isCurrentPlayerAI()) {
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                if (isCurrentPlayerAI()) {
                    gameModel.getCurrentPlayer().playTurn(gameModel);
                    clearSelection();
                    checkAndExecuteAITurn();
                }
            });
            pause.play();
        }
    }

    private void setSelection(Country c, String msg) {
        sourceCountry = c;
        gameView.getMapPane().setSelectedCountry(c);
        gameView.getControlPane().setMessage(msg);
    }

    private void clearSelection() {
        sourceCountry = null;
        gameView.getMapPane().setSelectedCountry(null);
    }

    private boolean canAttackFrom(Country c) {
        return c.getOwner().equals(gameModel.getCurrentPlayer()) && c.getArmies() > 1;
    }

    private boolean isCurrentPlayerAI() {
        return gameModel.getCurrentPlayer() != null && gameModel.getCurrentPlayer().isAI();
    }

    private void updateGameView() {
        Player p = gameModel.getCurrentPlayer();
        if (p == null) return;

        gameView.getControlPane().updateView(
                p.getName(),
                gameModel.getCurrentPhase().toString(),
                p.getDraftArmies());
        gameView.getMapPane().refreshMap();
    }
}