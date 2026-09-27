package minilang.model;

import java.math.BigInteger;
import java.util.Objects;

public final class FilterInstr extends Instruction {
    public enum Comparison {
        GREATER(">"), LESS("<"), GREATER_OR_EQUAL(">="), LESS_OR_EQUAL("<="), EQUAL("==");

        private final String symbol;

        Comparison(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private final Comparison comparison;
    private final BigInteger operand;

    public FilterInstr(int line, Comparison comparison, BigInteger operand) {
        super(line);
        this.comparison = Objects.requireNonNull(comparison, "comparison is required");
        this.operand = requireNonNegative(operand);
    }

    public Comparison getComparison() {
        return comparison;
    }

    public BigInteger getOperand() {
        return operand;
    }

    @Override
    public String getName() {
        return "FILTER";
    }

    @Override
    public String toIR() {
        return getName() + "|" + comparison.getSymbol() + "|" + operand;
    }

    @Override
    public boolean requiresList() {
        return true;
    }
}
