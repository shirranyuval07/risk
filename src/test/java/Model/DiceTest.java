package Model;

import com.example.demo.model.manager.Dice;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DiceTest {

    @Test
    public void testRollReturnsCorrectNumberOfDice() {
        Dice dice = new Dice();
        Integer[] result = dice.roll(3);

        assertEquals(3, result.length, "Should return exactly 3 dice results");
    }

    @Test
    public void testRollIsSortedDescending() {
        Dice dice = new Dice();
        // Rolling 10 dice to ensure we get a good spread of numbers to test the sorting
        Integer[] result = dice.roll(10);

        for (int i = 0; i < result.length - 1; i++) {
            assertTrue(result[i] >= result[i + 1],
                    "Dice results must be sorted in descending order (highest to lowest)");
        }
    }

    @Test
    public void testRollValuesAreWithinBounds() {
        Dice dice = new Dice();
        Integer[] result = dice.roll(5);

        for (int val : result) {
            assertTrue(val >= 1 && val <= 6, "A dice roll value must be between 1 and 6");
        }
    }
}