package minilang.model;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public final class DataInstr extends Instruction {
    private final List<BigInteger> numbers;

    public DataInstr(int line, List<BigInteger> numbers) {
        super(line);
        this.numbers = List.copyOf(numbers);
        if (this.numbers.isEmpty()) {
            throw new IllegalArgumentException("DATA requires at least one number");
        }
        this.numbers.forEach(Instruction::requireNonNegative);
    }

    public List<BigInteger> getNumbers() {
        return numbers;
    }

    @Override
    public String getName() {
        return "DATA";
    }

    @Override
    public String toIR() {
        return getName() + "|" + numbers.stream()
            .map(Object::toString).collect(Collectors.joining(","));
    }
}
