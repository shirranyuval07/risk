package Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class Dice {
    private final Random random;

    public Dice() {
        this.random = new Random();
    }

    // מחזיר מערך של תוצאות ממויינות מהגבוה לנמוך (קריטי לחוקי ריסק)
    public Integer[] roll(int numberOfDice) {
        Integer[] results = new Integer[numberOfDice];
        for (int i = 0; i < numberOfDice; i++) {
            results[i] = random.nextInt(6) + 1; // 1 to 6
        }
        // מיון הפוך: 6, 4, 1
        Arrays.sort(results, Collections.reverseOrder());
        return results;
    }
}