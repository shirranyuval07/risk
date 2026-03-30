package com.example.demo.model;

import java.util.Collections;
import java.util.List;

/**
 * Service to handle card trading logic according to Risk rules.
 */
public class CardService {

    /**
     * Checks if a player has a valid set of cards and trades them for armies.
     * @param player The player attempting to trade cards.
     * @return The number of armies rewarded for the set, or 0 if no valid set exists.
     */
    public int tradeAnyValidSet(Player player) {
        List<Card> cards = player.getCards();
        int inf = Collections.frequency(cards, Card.INFANTRY);
        int cav = Collections.frequency(cards, Card.CAVALRY);
        int art = Collections.frequency(cards, Card.ARTILLERY);

        // One of each
        if (inf > 0 && cav > 0 && art > 0) {
            cards.remove(Card.INFANTRY);
            cards.remove(Card.CAVALRY);
            cards.remove(Card.ARTILLERY);
            return 10;
        }

        // Three of the same
        if (art >= 3) return tradeMatchingCards(cards, Card.ARTILLERY, 8);
        if (cav >= 3) return tradeMatchingCards(cards, Card.CAVALRY, 6);
        if (inf >= 3) return tradeMatchingCards(cards, Card.INFANTRY, 4);

        return 0; // No valid set
    }

    private int tradeMatchingCards(List<Card> cards, Card type, int reward) {
        for (int i = 0; i < 3; i++) {
            cards.remove(type);
        }
        return reward;
    }
}
