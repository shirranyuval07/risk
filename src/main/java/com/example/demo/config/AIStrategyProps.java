package com.example.demo.config;
import lombok.Data;

@Data
public class AIStrategyProps {
    private double weightWinProbability;
    private double weightContinentBonus;
    private double weightStrategicValue;
    private double weightExpectedCasualties;
    private double articulationPointBonus;
    private double casualtiesMultiplier;
    private double exposurePenaltyMultiplier;
    private double attackThreshold;
    private int minArmyAdvantage;
    private double weightFutureThreat;
    private double continentBreakMultiplier;
    private double bonusFocus;
    private double progressFocus;
    private double resistanceAvoidance;
    private double setupStackingWeight;
}