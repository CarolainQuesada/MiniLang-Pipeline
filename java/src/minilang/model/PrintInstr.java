package minilang.model;

public final class PrintInstr extends Instruction {
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
