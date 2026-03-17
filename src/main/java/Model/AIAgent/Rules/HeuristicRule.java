package Model.AIAgent.Rules;

import Model.AIAgent.AIGraphAnalyzer;
import Model.Country;
import Model.Player;

public interface HeuristicRule {
    /**
     * @return ציון מספרי המייצג את הערך של החוק עבור התקיפה הספציפית
     */
    double evaluate(Country source, Country target, Player currentPlayer, AIGraphAnalyzer analyzer);
}