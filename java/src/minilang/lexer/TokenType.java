package minilang.lexer;

/** Token categories recognized by the MiniLang lexer. */
public enum TokenType {
    DATA, FILTER, MAP, REDUCE, PRINT, SUM, MAX, MIN,
    NUMBER,
    GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, EQUAL_EQUAL,
    PLUS, MINUS, STAR,
    EOF
}
