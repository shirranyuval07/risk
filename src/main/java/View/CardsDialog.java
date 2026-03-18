package View;

import Model.Card;
import Model.Player;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Collections;

public class CardsDialog
{

    public static void show(Player player, Runnable onTradeSuccessful) {
        int inf = Collections.frequency(player.getCards(), Card.INFANTRY);
        int cav = Collections.frequency(player.getCards(), Card.CAVALRY);
        int art = Collections.frequency(player.getCards(), Card.ARTILLERY);

        String content = String.format("""
                You currently have:
                %d x Infantry (Soldier)
                %d x Cavalry (Horse)
                %d x Artillery (Cannon)
                
                Trade Values:
                3 Infantry = 4 Armies
                3 Cavalry = 6 Armies
                3 Artillery = 8 Armies
                1 of Each = 10 Armies
                """, inf, cav, art);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Your Cards");
        alert.setHeaderText("Commander " + player.getName() + "'s Hand");
        alert.setContentText(content);

        // אם יש לשחקן סט חוקי, אנחנו מוסיפים כפתור טרייד!
        ButtonType btnTrade = new ButtonType("Trade Best Set");
        if (hasValidSet(inf, cav, art)) {
            alert.getButtonTypes().add(btnTrade);
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == btnTrade) {
                int bonusArmies = player.tradeAnyValidSet();
                player.setDraftArmies(player.getDraftArmies()+bonusArmies); // הוספת החיילים לשחקן

                Alert success = new Alert(Alert.AlertType.INFORMATION, "Success! You received " + bonusArmies + " armies!");
                success.show();

                onTradeSuccessful.run(); // רענון המסך אחרי העסקה
            }
        });
    }

    private static boolean hasValidSet(int inf, int cav, int art) {
        return (inf > 0 && cav > 0 && art > 0) || inf >= 3 || cav >= 3 || art >= 3;
    }
}