package com.example.demo;

import Controller.GameController;
import Model.GreedyAI;
import Model.Player;
import Model.RiskGame;
import View.GameRoot; // שים לב: אנחנו משתמשים ב-GameRoot של JavaFX עכשיו!
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color; // שימוש ב-Color של JavaFX
import javafx.stage.Stage;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RiskApplication extends Application {

    private RiskGame game;

    // שלב 1 במחזור החיים של JavaFX: אתחול הלוגיקה מאחורי הקלעים
    @Override
    public void init() {
        game = new RiskGame();

        // שים לב לשימוש ב-Color.rgb של JavaFX
        Player human = new Player("Yuval", Color.rgb(50, 150, 230), false);
        Player aiBot = new Player("Terminator Bot", Color.rgb(225, 60, 60), true);
        aiBot.setStrategy(new GreedyAI());

        game.addPlayer(human);
        game.addPlayer(aiBot);

        // Setup אוטומטי של כל העולם!
        game.startGame();
    }

    // שלב 2: בניית הממשק הגרפי והצגתו
    @Override
    public void start(Stage primaryStage) {
        // יצירת החלון הראשי (מחליף את GameFrame הישן)
        GameRoot root = new GameRoot(game);

        // יצירת הקונטרולר (נעדכן אותו בהמשך שיתאים ל-JavaFX)
        new GameController(game, root);

        // הגדרת הסצנה (התוכן) בתוך ה-Stage (החלון)
        Scene scene = new Scene(root, 1200, 800);

        primaryStage.setTitle("⚔ Risk: Global Conquest 2026 ⚔");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // תופס את כל המסך
        primaryStage.show();
    }

    public static void main(String[] args) {
        // שורת הקסם של JavaFX: מפעילה את האפליקציה במקום Spring Boot רגיל
        launch(args);
    }
}