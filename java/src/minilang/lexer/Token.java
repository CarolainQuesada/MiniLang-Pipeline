package minilang.lexer;

import java.util.Objects;

/** Lexical unit and its source line, numbered from 1. */
public record Token(TokenType type, String lexeme, int line) {
    public Token {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(lexeme, "lexeme is required");
        if (line < 1) {
            throw new IllegalArgumentException("line must be positive");
        }
    }
}
