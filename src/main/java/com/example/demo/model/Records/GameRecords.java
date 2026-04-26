package com.example.demo.model.Records;

import com.example.demo.model.manager.Country;

public interface GameRecords
{

    record AttackMove(Country source, Country target, double heuristicScore) implements Comparable<AttackMove>
    {
        @Override
        public int compareTo(AttackMove o) {
            return Double.compare(this.heuristicScore, o.heuristicScore);
        }
    }
    record BattleResult(
            Integer[] attackerRolls, // מערך קוביות התוקף (עד 3)
            Integer[] defenderRolls, // מערך קוביות המגן (עד 2)
            int attackerLosses,      // כמות האבדות לתוקף
            int defenderLosses,      // כמות האבדות למגן
            boolean conquered,       // האם הטריטוריה נכבשה?
            int minMove,
            int maxMove)
    {}

    record FortifyMove(Country source, Country target, int armiesToMove) {}
}