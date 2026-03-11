package Model.Records;


import Model.Country;

public record AttackMove(Country source, Country target, double heuristicScore)
{
}