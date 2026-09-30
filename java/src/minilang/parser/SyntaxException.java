package minilang.parser;

/** Reports a syntax error and the source line where it occurred. */
public final class SyntaxException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int line;

    public SyntaxException(int line, String detail) {
        super("Linea " + line + ": " + detail);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
