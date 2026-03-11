package Model.Records;

public record BattleResult(Integer[] attackerRolls, // מערך קוביות התוקף (עד 3)
        Integer[] defenderRolls, // מערך קוביות המגן (עד 2)
        int attackerLosses,      // כמות האבדות לתוקף
        int defenderLosses,      // כמות האבדות למגן
        boolean conquered)  // האם הטריטוריה נכבשה?
{}