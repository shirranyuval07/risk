package Model.AIAgent;

import com.example.demo.model.AIAgent.AIGraphAnalyzer;
import com.example.demo.model.manager.Country;
import com.example.demo.model.manager.Player;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class AIGraphAnalyzerTest {

    @Test
    public void testFindArticulationPoints() {
        AIGraphAnalyzer analyzer = new AIGraphAnalyzer();

        // We pass null for Color since this is a headless logic test (no UI required)
        Player player = new Player("TestAI", null, true);

        // Create a sequence of 4 countries
        Country c1 = new Country(1, "Country1", 0, 0);
        Country c2 = new Country(2, "Country2", 0, 0);
        Country c3 = new Country(3, "Country3", 0, 0);
        Country c4 = new Country(4, "Country4", 0, 0);

        // Connect them in a straight line: C1 <-> C2 <-> C3 <-> C4
        // In this topology, C2 and C3 are articulation points (bottlenecks) because
        // losing either of them would split the player's empire in half.
        c1.addNeighbor(c2); c2.addNeighbor(c1);
        c2.addNeighbor(c3); c3.addNeighbor(c2);
        c3.addNeighbor(c4); c4.addNeighbor(c3);

        player.addCountry(c1);
        player.addCountry(c2);
        player.addCountry(c3);
        player.addCountry(c4);

        Set<Country> bottlenecks = analyzer.findArticulationPoints(player);

        // Assertions
        assertTrue(bottlenecks.contains(c2), "C2 should be identified as a bottleneck");
        assertTrue(bottlenecks.contains(c3), "C3 should be identified as a bottleneck");
        assertFalse(bottlenecks.contains(c1), "C1 is an edge node, not a bottleneck");
        assertFalse(bottlenecks.contains(c4), "C4 is an edge node, not a bottleneck");
    }
}