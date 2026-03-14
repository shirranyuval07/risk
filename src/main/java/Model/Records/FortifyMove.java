package Model.Records;
import Model.Country;

public record FortifyMove(Country source, Country target, int armiesToMove) {}