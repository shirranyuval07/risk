package Model.AIAgent.Rules;

import Model.AIAgent.AIGraphAnalyzer;
import Model.AIAgent.HeuristicRule;
import Model.Country;
import Model.Player;

public class FutureThreatRule implements HeuristicRule {

    @Override
    public double evaluate(Country source, Country target, Player currentPlayer, AIGraphAnalyzer analyzer) {
        int maxThreat = 0;

        // סורקים את כל השכנים של המדינה המותקפת (היעד)
        for (Country neighbor : target.getNeighbors()) {
            // אם השכן לא שלנו, והוא חזק מהאיום שמצאנו עד כה
            if (neighbor.getOwner() != currentPlayer && neighbor.getArmies() > maxThreat) {
                maxThreat = neighbor.getArmies();
            }
        }

        // אנחנו מחזירים את עוצמת האיום.
        // במחלקה שתפעיל את החוק, נכפיל את המספר הזה במשקל שלילי (עונש).
        return (double) maxThreat / Math.max(source.getArmies(), 1);
    }
}