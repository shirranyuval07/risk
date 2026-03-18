package com.example.demo;

import Controller.GameController;
import Model.AIAgent.Strategies.BalancedStrategy;
import Model.AIAgent.Strategies.DefensiveStrategy;
import Model.AIAgent.GreedyAI;
import Model.AIAgent.Strategies.OffensiveStrategy;
import Model.Player;
import Model.RiskGame;
import View.GameRoot;
import View.MainMenu;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication(scanBasePackages = {"com.example.demo", "Model"})
public class RiskApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private Stage primaryStage;

    @Override
    public void init() {
        // טעינת תשתיות ה-Spring ברקע
        springContext = SpringApplication.run(RiskApplication.class);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            Image icon = new Image("map_background.png");
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Icon image not found");
        }

        primaryStage.setTitle("⚔ Risk: Global Conquest 2026 ⚔");

        // טעינת מסך התפריט הראשי
        showMainMenu();

        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void showMainMenu() {
        // העברת פונקציית הקולבאק: מה קורה כשלוחצים Start?
        MainMenu mainMenu = new MainMenu(this::startGameWithConfig);
        Scene menuScene = new Scene(mainMenu, 1200, 800);
        primaryStage.setScene(menuScene);
    }

    // הפונקציה הזו מופעלת רק אחרי שהמשתמש לחץ על כפתור ההתחלה בתפריט
    private void startGameWithConfig(List<MainMenu.PlayerSetup> playerSetups) {
        RiskWebSocketClient networkClient = new RiskWebSocketClient("Yuval");
        networkClient.connect();
        RiskGame game = new RiskGame();

        // שליפת אסטרטגיות הבוטים מתוך Spring Context
        BalancedStrategy balancedStrategy = springContext.getBean(BalancedStrategy.class);
        DefensiveStrategy defensiveStrategy = springContext.getBean(DefensiveStrategy.class);
        OffensiveStrategy offensiveStrategy = springContext.getBean(OffensiveStrategy.class);

        // תרגום בחירות ה-UI לאובייקטי Player אמיתיים
        for (MainMenu.PlayerSetup setup : playerSetups) {
            boolean isAI = !setup.type().equals("Human");
            Player p = new Player(setup.name(), setup.color(), isAI);

            // אם זה בוט, משדכים לו את המוח הנכון לפי מה שנבחר בקומבובוקס
            if (isAI) {
                switch (setup.type()) {
                    case "AI - Balanced" -> p.setStrategy(new GreedyAI(balancedStrategy));
                    case "AI - Defensive" -> p.setStrategy(new GreedyAI(defensiveStrategy));
                    case "AI - Offensive" -> p.setStrategy(new GreedyAI(offensiveStrategy));
                }
            }
            game.addPlayer(p);
        }

        // התחלת הלוגיקה
        game.startGame();

        // בניית הממשק של המפה והקונטרולר
        GameRoot root = new GameRoot(game);
        new GameController(game, root);


        Scene gameScene = new Scene(root, 1200, 800);
        primaryStage.setScene(gameScene);
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }
}