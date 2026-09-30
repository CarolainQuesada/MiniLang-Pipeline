package minilang.ir;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import minilang.model.Instruction;

/**
 * Serializes a validated instruction list into the IR text file format.
 *
 * <p>This class only handles formatting. It does not execute any FILTER, MAP or REDUCE;
 * those operations continue to be the responsibility of the Python stage.</p>
 */
public final class IrGenerator {
    /**
     * Converts a list of instructions into the exact contract required by the pipeline.
     *
     * @param instructions validated instructions in execution order
     * @return IR content with one instruction per line and a final newline
     */
    public String generate(List<Instruction> instructions) {
        Objects.requireNonNull(instructions, "instructions are required");
        return instructions.stream()
            .map(instruction -> Objects.requireNonNull(instruction, "instruction is required").toIR())
            .collect(Collectors.joining("\n", "", "\n"));
    }
}
