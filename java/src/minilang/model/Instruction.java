package minilang.model;

import java.math.BigInteger;
import java.util.Objects;

/** Program instruction with its source line. */
public abstract class Instruction {
    private final int line;

    protected Instruction(int line) {
        if (line < 1) {
            throw new IllegalArgumentException("line must be positive");
        }
        this.line = line;
    }

    public final int getLine() {
        return line;
    }

    public abstract String getName();

    /** Line of the programa.ir contract; each subclass defines its own format. */
    public abstract String toIR();

    /** Whether the instruction operates on the data list; FILTER, MAP and REDUCE override it. */
    public boolean requiresList() {
        return false;
    }

    /** Whether the instruction turns the list into a single number; REDUCE overrides it. */
    public boolean producesNumber() {
        return false;
    }

    protected static BigInteger requireNonNegative(BigInteger number) {
        Objects.requireNonNull(number, "number is required");
        if (number.signum() < 0) {
            throw new IllegalArgumentException("number must be non-negative");
        }
        return number;
    }
}
