package minilang.semantic;

public final class SemanticException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int line;

    public SemanticException(int line, String detail) {
        super("Linea " + line + ": " + detail);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
