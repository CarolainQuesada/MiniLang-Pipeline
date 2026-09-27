package minilang.semantic;

import java.util.List;
import java.util.Objects;
import minilang.model.Instruction;

/**
 * Checks the meaning of a program the parser already accepted. The grammar allows
 * operations after REDUCE, but at that point the list is already a single number.
 * It does not check concrete types: each instruction reports what it needs and produces.
 */
public final class SemanticAnalyzer {
    public void check(List<Instruction> instructions) {
        Objects.requireNonNull(instructions, "instructions are required");
        Instruction reduction = null;
        for (Instruction instruction : instructions) {
            if (reduction != null && instruction.requiresList()) {
                throw new SemanticException(instruction.getLine(), instruction.getName()
                    + " necesita una lista, pero " + reduction.getName() + " de la linea "
                    + reduction.getLine() + " ya la convirtio en un solo numero; despues de "
                    + reduction.getName() + " solo puede venir PRINT");
            }
            if (instruction.producesNumber()) {
                reduction = instruction;
            }
        }
    }
}
