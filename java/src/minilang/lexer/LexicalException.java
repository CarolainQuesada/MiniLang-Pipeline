package minilang.lexer;

/** Reports a lexical error and the source line where it occurred. */
public final class LexicalException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int line;

    public LexicalException(int line, String detail) {
        super("Linea " + line + ": " + detail);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
