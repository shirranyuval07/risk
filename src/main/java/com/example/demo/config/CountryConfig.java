package com.example.demo.config;

import lombok.Data;
import java.util.List;

@Data
public class CountryConfig {
    private int id;
    private String name;
    private String svgId;
    private String continentName;
    private List<Integer> neighbors;
}