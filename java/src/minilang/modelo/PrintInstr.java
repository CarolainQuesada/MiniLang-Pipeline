package minilang.modelo;

public final class PrintInstr extends Instruccion {
    public PrintInstr(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "PRINT";
    }
}
