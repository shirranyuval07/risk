package com.example.demo.model.Records;
import com.example.demo.model.Country;

public record FortifyMove(Country source, Country target, int armiesToMove) {}