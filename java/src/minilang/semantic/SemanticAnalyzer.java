package minilang.semantic;

import java.util.List;
import java.util.Objects;
import minilang.model.Instruction;

/**
 * Validates the semantic consistency of an already syntactically correct program.
 *
 * <p>The parser accepts the grammar, but a REDUCE instruction reduces the data list to a
 * single numeric value. This analyzer ensures that no operation that still requires a list
 * is executed afterwards.</p>
 */
public final class SemanticAnalyzer {
    /**
     * Checks whether the operation sequence keeps a valid data flow.
     *
     * @param instructions instructions in the order they appear in the source program
     */
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
