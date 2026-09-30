package minilang.model;

import java.util.Objects;

/**
 * Represents a {@code REDUCE} instruction, which collapses an entire list into one value.
 */
public final class ReduceInstr extends Instruction {
    /**
     * Reduction functions accepted by the language.
     */
    public enum Aggregate {
        SUM, MAX, MIN
    }

    private final Aggregate aggregate;

    /**
     * Creates a REDUCE instruction.
     *
     * @param line source line number
     * @param aggregate aggregation function to be applied
     */
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
