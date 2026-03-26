package com.example.demo.config;

import lombok.Data;
import java.util.List;

@Data
public class BoardConfig {
    private List<ContinentConfig> continents;
    private List<CountryConfig> countries;
}