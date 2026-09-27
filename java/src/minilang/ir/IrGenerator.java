package minilang.ir;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import minilang.model.Instruction;

/**
 * Joins the lines each instruction produces with toIR(); does not execute operations or write files.
 * It does not know the subclasses: the format of each line is resolved through polymorphism.
 */
public final class IrGenerator {
    public String generate(List<Instruction> instructions) {
        Objects.requireNonNull(instructions, "instructions are required");
        return instructions.stream()
            .map(instruction -> Objects.requireNonNull(instruction, "instruction is required").toIR())
            .collect(Collectors.joining("\n", "", "\n"));
    }
}
