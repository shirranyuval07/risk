package com.example.demo;

import com.example.demo.controller.GameController;
import com.example.demo.service.AIEngine;
import com.example.demo.model.AIAgent.Strategies.HeuristicStrategy;
import com.example.demo.model.manager.Player;
import com.example.demo.model.manager.RiskGame;
import com.example.demo.view.GameRoot;
import com.example.demo.view.MainMenu;
import com.example.demo.network.client.RiskWebSocketClient;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
@Slf4j
@SpringBootApplication
public class RiskApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private Stage primaryStage;

    // Whether this instance is running as the host (server mode)
    private boolean isServer = false;


    // JavaFX needs the default empty constructor to launch!

    @Override
    public void init() {
        List<String> params = getParameters().getRaw();
        isServer = params.contains("--server");

        if (isServer) {
            log.info("🖥 Starting in SERVER mode — Spring Boot will start.");
            springContext = SpringApplication.run(RiskApplication.class);
        } else {
            log.info("🎮 Starting in CLIENT mode — no server will be started.");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            Image icon = new Image("map_background.png");
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            log.info("Icon image not found");
        }

        primaryStage.setTitle("⚔ Risk: Global Conquest 2026 ⚔");
        showMainMenu();
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void showMainMenu() {
        MainMenu mainMenu = new MainMenu(this::startGameWithConfig);
        Scene menuScene = new Scene(mainMenu, 1200, 800);
        primaryStage.setScene(menuScene);
    }

    private void startGameWithConfig(List<MainMenu.PlayerSetup> playerSetups, RiskWebSocketClient networkClient) {
        RiskGame game = new RiskGame();

        if (isServer && springContext != null) {

            // FETCH the strategies and factory dynamically from the Spring context!
            HeuristicStrategy balancedStrategy = springContext.getBean("balancedStrategy", HeuristicStrategy.class);
            HeuristicStrategy defensiveStrategy = springContext.getBean("defensiveStrategy", HeuristicStrategy.class);
            HeuristicStrategy offensiveStrategy = springContext.getBean("offensiveStrategy", HeuristicStrategy.class);
            AIEngine.Factory aiFactory = springContext.getBean(AIEngine.Factory.class);

            for (MainMenu.PlayerSetup setup : playerSetups) {
                boolean isAI = !setup.type().equals("Human");
                Player p = new Player(setup.name(), setup.color(), isAI);

                if (isAI) {
                    switch (setup.type()) {
                        case "AI - Balanced"  -> p.setStrategy(aiFactory.createAI(balancedStrategy));
                        case "AI - Defensive" -> p.setStrategy(aiFactory.createAI(defensiveStrategy));
                        case "AI - Offensive" -> p.setStrategy(aiFactory.createAI(offensiveStrategy));
                    }
                }
                game.addPlayer(p);
            }
        } else {
            // Client mode: all players are Human (no AI beans available)
            for (MainMenu.PlayerSetup setup : playerSetups) {
                Player p = new Player(setup.name(), setup.color(), false);
                game.addPlayer(p);
            }
        }

        if (networkClient != null) {
            game.setGameSeed(networkClient.getGameSeed());
        }
        game.startGame();

        GameRoot root = new GameRoot(game);
        new GameController(game, root, networkClient,this::showMainMenu);

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