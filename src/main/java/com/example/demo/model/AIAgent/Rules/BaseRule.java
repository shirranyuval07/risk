package com.example.demo.model.AIAgent.Rules;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.manager.Player;

/**
 * ממשק בסיסי גנרי לכל כללי ההיוריסטיקה.
 * הפרמטר C מגדיר את סוג ההקשר שהכלל מעריך:
 * - HeuristicRule:      C = AttackContext (source + target) — מעריך התקפה
 * - SetupHeuristicRule: C = Country                        — מעריך הצבה
 *
 * @param <C> סוג ההקשר שמועבר לפונקציית evaluate
 */
@FunctionalInterface
public interface BaseRule<C>
{
    double evaluate(C context, Player player, AIGraphAnalyzer analyzer);
}
