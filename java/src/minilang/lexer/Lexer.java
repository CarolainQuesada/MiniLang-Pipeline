package minilang.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Splits text into tokens; does not validate program structure or execute instructions. */
public final class Lexer {
    private static final Map<String, TokenType> KEYWORDS = Map.of(
        "DATA", TokenType.DATA, "FILTER", TokenType.FILTER,
        "MAP", TokenType.MAP, "REDUCE", TokenType.REDUCE,
        "PRINT", TokenType.PRINT, "SUM", TokenType.SUM,
        "MAX", TokenType.MAX, "MIN", TokenType.MIN);

    private final String source;
    private int position;
    private int line;

    public Lexer(String source) {
        this.source = Objects.requireNonNull(source, "source is required");
    }

    public List<Token> tokenize() {
        position = 0;
        line = 1;
        List<Token> tokens = new ArrayList<>();
        while (position < source.length()) {
            char current = source.charAt(position);
            if (current == ' ' || current == '\t') {
                position++;
            } else if (current == '\r' || current == '\n') {
                consumeLineBreak();
            } else if (isDigit(current)) {
                tokens.add(readNumber());
            } else if (Character.isLetter(current)) {
                tokens.add(readWord());
            } else {
                tokens.add(readOperator());
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return List.copyOf(tokens);
    }

    private void consumeLineBreak() {
        char lineBreak = source.charAt(position++);
        if (lineBreak == '\r' && position < source.length() && source.charAt(position) == '\n') {
            position++;
        }
        line++;
    }

    private Token readNumber() {
        int start = position;
        while (position < source.length() && isDigit(source.charAt(position))) {
            position++;
        }
        return new Token(TokenType.NUMBER, source.substring(start, position), line);
    }

    /** Reads letters of any language so the error shows the whole word; only KEYWORDS are valid. */
    private Token readWord() {
        int start = position;
        while (position < source.length() && Character.isLetter(source.charAt(position))) {
            position++;
        }
        String word = source.substring(start, position);
        TokenType type = KEYWORDS.get(word);
        if (type == null) {
            throw new LexicalException(line, "Palabra no reconocida: " + word);
        }
        return new Token(type, word, line);
    }

    private Token readOperator() {
        int start = position;
        char symbol = source.charAt(position++);
        TokenType type = switch (symbol) {
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '*' -> TokenType.STAR;
            case '>' -> consumeEquals() ? TokenType.GREATER_EQUAL : TokenType.GREATER;
            case '<' -> consumeEquals() ? TokenType.LESS_EQUAL : TokenType.LESS;
            case '=' -> {
                if (!consumeEquals()) {
                    throw new LexicalException(line, "Operador '=' invalido; se esperaba '=='");
                }
                yield TokenType.EQUAL_EQUAL;
            }
            default -> throw new LexicalException(line,
                "Caracter no reconocido: " + describe(source.codePointAt(start)));
        };
        return new Token(type, source.substring(start, position), line);
    }

    private boolean consumeEquals() {
        if (position < source.length() && source.charAt(position) == '=') {
            position++;
            return true;
        }
        return false;
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    /** Visible ASCII is shown as is; anything else by its Unicode code, since consoles may not print it. */
    private static String describe(int codePoint) {
        if (codePoint > ' ' && codePoint < 127) {
            return String.valueOf((char) codePoint);
        }
        return String.format(
            "U+%04X (invisible o no ASCII; puede venir de copiar y pegar, escribalo de nuevo)", codePoint);
    }
}
