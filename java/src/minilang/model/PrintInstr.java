package minilang.model;

/**
 * Represents the terminal {@code PRINT} instruction of the MiniLang program.
 */
public final class PrintInstr extends Instruction {
    /**
     * Creates the final print instruction of the program.
     *
     * @param line source line number where the instruction appears
     */
    public PrintInstr(int line) {
        super(line);
    }

    @Override
    public String getName() {
        return "PRINT";
    }

    @Override
    public String toIR() {
        return getName();
    }
}
