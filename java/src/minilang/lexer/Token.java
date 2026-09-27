package minilang.lexer;

import java.util.Objects;

/** Unidad lexica y su linea de origen, numerada desde 1. */
public record Token(TipoToken tipo, String lexema, int linea) {
    public Token {
        Objects.requireNonNull(tipo, "El tipo es obligatorio");
        Objects.requireNonNull(lexema, "El lexema es obligatorio");
        if (linea < 1) {
            throw new IllegalArgumentException("La linea debe ser positiva");
        }
    }
}
