package minilang.lexer;

public final class ErrorLexico extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int linea;

    public ErrorLexico(int linea, String detalle) {
        super("Linea " + linea + ": " + detalle);
        this.linea = linea;
    }

    public int getLinea() {
        return linea;
    }
}
