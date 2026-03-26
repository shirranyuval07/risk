package com.example.demo.model;


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.shape.SVGPath;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Country (Territory) in the Risk game.
 * Uses JavaFX Properties so the UI can bind to data changes automatically.
 */
public class Country {
    @Getter
    private final int id;
    @Getter
    private final String name;

    // --- Data Binding Properties ---
    private final ObjectProperty<Player> owner = new SimpleObjectProperty<>(null);
    private final IntegerProperty armies = new SimpleIntegerProperty(0);

    @Getter
    private final List<Country> neighbors;

    @Getter @Setter
    private int x;
    @Getter @Setter
    private int y;

    @Getter @Setter
    private SVGPath shape;

    @Getter @Setter
    private Continent continent;

    public Country(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.neighbors = new ArrayList<>();
    }

    public void addNeighbor(Country neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
        }
    }

    // --- Property Getters & Setters ---

    public Player getOwner() { return owner.get(); }
    public void setOwner(Player newOwner) { owner.set(newOwner); }
    public ObjectProperty<Player> ownerProperty() { return owner; }

    public int getArmies() { return armies.get(); }
    public void setArmies(int amount) { armies.set(amount); }
    public IntegerProperty armiesProperty() { return armies; }

    // Helper methods for game logic
    public void addArmies(int amount) { this.armies.set(this.armies.get() + amount); }
    public void removeArmies(int amount) { this.armies.set(this.armies.get() - amount); }
}