package minilang.model;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Represents a {@code MAP} instruction, which transforms each element using one operator.
 */
public final class MapInstr extends Instruction {
    /**
     * Arithmetic operators supported by the MAP instruction.
     */
    public enum Operator {
        ADD("+"), SUBTRACT("-"), MULTIPLY("*");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private final Operator operator;
    private final BigInteger operand;

    /**
     * Creates a MAP instruction.
     *
     * @param line source line number
     * @param operator arithmetic symbol to apply to each value
     * @param operand numeric constant used in the operation
     */
    public MapInstr(int line, Operator operator, BigInteger operand) {
        super(line);
        this.operator = Objects.requireNonNull(operator, "operator is required");
        this.operand = requireNonNegative(operand);
    }

    public Operator getOperator() {
        return operator;
    }

    public BigInteger getOperand() {
        return operand;
    }

    @Override
    public String getName() {
        return "MAP";
    }

    @Override
    public String toIR() {
        return getName() + "|" + operator.getSymbol() + "|" + operand;
    }

    @Override
    public boolean requiresList() {
        return true;
    }
}
