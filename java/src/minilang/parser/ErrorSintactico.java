package minilang.parser;

public final class ErrorSintactico extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int linea;

    public ErrorSintactico(int linea, String detalle) {
        super("Linea " + linea + ": " + detalle);
        this.linea = linea;
    }

    public int getLinea() {
        return linea;
    }
}
