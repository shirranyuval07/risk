package com.example.demo;

import Controller.GameController;
import Model.AIAgent.Strategies.BalancedStrategy;
import Model.AIAgent.Strategies.DefensiveStrategy;
import Model.AIAgent.GreedyAI;
import Model.AIAgent.Strategies.OffensiveStrategy;
import Model.Player;
import Model.RiskGame;
import View.GameRoot;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = {"com.example.demo", "Model"})
public class RiskApplication extends Application {

    private RiskGame game;
    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // 1. Boot up the Spring Framework!
        springContext = SpringApplication.run(RiskApplication.class);

        game = new RiskGame();

        // Player human = new Player("Yuval", Color.rgb(50, 150, 230), false);

        Player aiBot = new Player("Terminator Bot Defense", Color.rgb(225, 60, 60), true);
        Player aiBot2 = new Player("Terminator Bot Offense", Color.rgb(225, 60, 225), true);
        Player aiBot3 = new Player("Terminator Bot Balanced", Color.rgb(225, 143, 60), true);



        BalancedStrategy balancedStrategy = springContext.getBean(BalancedStrategy.class);
        DefensiveStrategy defensiveStrategy = springContext.getBean((DefensiveStrategy.class));
        OffensiveStrategy offensiveStrategy = springContext.getBean(OffensiveStrategy.class);
        aiBot3.setStrategy(new GreedyAI(balancedStrategy));
        aiBot.setStrategy(new GreedyAI(defensiveStrategy));
        aiBot2.setStrategy(new GreedyAI(offensiveStrategy));

        game.addPlayer(aiBot);
        game.addPlayer(aiBot2);
        game.addPlayer(aiBot3);

        game.startGame();
    }

    @Override
    public void start(Stage primaryStage) {
        GameRoot root = new GameRoot(game);
        new GameController(game, root);

        Scene scene = new Scene(root, 1200, 800);
        try {
            Image icon = new Image("map_background.png");
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Icon image not found");
        }

        primaryStage.setTitle("⚔ Risk: Global Conquest 2026 ⚔");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // 3. Gracefully shut down Spring when the game closes
    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }
}