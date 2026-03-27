package com.example.demo;

import com.example.demo.config.AIProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(args = "--server")
class RiskApplicationTests {

    @Autowired
    private AIProperties aiProperties;

	@Test
	void contextLoads() {
        assertNotNull(aiProperties);
        assertNotNull(aiProperties.getBalanced());
        assertEquals(1.5, aiProperties.getBalanced().getWeightWinProbability());
	}

}
