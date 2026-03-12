package Model.Records;


import Model.Country;

public record AttackMove(Country source, Country target, double heuristicScore) implements Comparable<AttackMove> {
    @Override
    public int compareTo(AttackMove o) {
        return Double.compare(this.heuristicScore, o.heuristicScore);
    }
}