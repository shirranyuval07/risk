package com.example.demo.model.AIAgent.Rules;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.Country;
import com.example.demo.model.Player;

public interface HeuristicRule {
    /**
     * @return ציון מספרי המייצג את הערך של החוק עבור התקיפה הספציפית
     */
    double evaluate(Country source, Country target, Player currentPlayer, AIGraphAnalyzer analyzer);
}