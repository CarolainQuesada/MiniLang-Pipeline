package minilang.model;

import java.math.BigInteger;
import java.util.Objects;

public final class MapInstr extends Instruction {
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
