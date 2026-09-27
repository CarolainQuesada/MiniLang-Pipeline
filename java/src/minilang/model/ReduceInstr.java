package minilang.model;

import java.util.Objects;

public final class ReduceInstr extends Instruction {
    public enum Aggregate {
        SUM, MAX, MIN
    }

    private final Aggregate aggregate;

    public ReduceInstr(int line, Aggregate aggregate) {
        super(line);
        this.aggregate = Objects.requireNonNull(aggregate, "aggregate is required");
    }

    public Aggregate getAggregate() {
        return aggregate;
    }

    @Override
    public String getName() {
        return "REDUCE";
    }

    @Override
    public String toIR() {
        return getName() + "|" + aggregate.name();
    }

    @Override
    public boolean requiresList() {
        return true;
    }

    @Override
    public boolean producesNumber() {
        return true;
    }
}
