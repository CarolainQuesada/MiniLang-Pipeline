import java.util.List;
import minilang.lexer.Lexer;
import minilang.lexer.LexicalException;
import minilang.lexer.Token;
import minilang.lexer.TokenType;

/** Lexer-only tests, without external dependencies. */
public final class LexerTest {
    public static void main(String[] args) {
        checkExample();
        checkOperators();
        checkLines();
        checkNumbers();
        checkErrors();
        checkErrorMessages();
        check(new Lexer("").tokenize().equals(List.of(new Token(TokenType.EOF, "", 1))),
            "Empty source");
        System.out.println("PASS: example, operators, lines, numbers and lexical errors");
    }

    private static void checkExample() {
        Lexer lexer = new Lexer("DATA 3 8 5 10 12\nFILTER > 5\nMAP * 2\nREDUCE SUM\nPRINT");
        List<Token> tokens = lexer.tokenize();
        check(tokens.stream().map(Token::type).toList().equals(List.of(
            TokenType.DATA, TokenType.NUMBER, TokenType.NUMBER, TokenType.NUMBER,
            TokenType.NUMBER, TokenType.NUMBER, TokenType.FILTER, TokenType.GREATER,
            TokenType.NUMBER, TokenType.MAP, TokenType.STAR, TokenType.NUMBER,
            TokenType.REDUCE, TokenType.SUM, TokenType.PRINT, TokenType.EOF)), "Example token types");
        check(tokens.get(5).lexeme().equals("12"), "Number lexeme");
        check(tokens.get(6).line() == 2 && tokens.get(14).line() == 5, "Example lines");
        check(lexer.tokenize().equals(tokens), "Repeatable tokenization");
    }

    private static void checkOperators() {
        List<TokenType> types = new Lexer("> < >= <= == + - * SUM MAX MIN").tokenize()
            .stream().map(Token::type).toList();
        check(types.equals(List.of(TokenType.GREATER, TokenType.LESS,
            TokenType.GREATER_EQUAL, TokenType.LESS_EQUAL, TokenType.EQUAL_EQUAL,
            TokenType.PLUS, TokenType.MINUS, TokenType.STAR,
            TokenType.SUM, TokenType.MAX, TokenType.MIN, TokenType.EOF)), "Exact operators");
        check(new Lexer("FILTER>=5").tokenize().get(1)
            .equals(new Token(TokenType.GREATER_EQUAL, ">=", 1)), "Operator without spaces");
    }

    private static void checkLines() {
        List<Token> tokens = new Lexer("\tDATA 0\r\n\rFILTER<1\nPRINT\n").tokenize();
        check(tokens.get(2).line() == 3, "CRLF counts as one line and CR as another");
        check(tokens.get(5).line() == 4, "LF line break");
        check(tokens.get(6).equals(new Token(TokenType.EOF, "", 5)), "EOF line");
    }

    private static void checkNumbers() {
        String big = "999999999999999999999999999999999999999";
        List<Token> tokens = new Lexer("0 007 " + big + " -2").tokenize();
        check(tokens.get(0).lexeme().equals("0"), "Zero");
        check(tokens.get(1).lexeme().equals("007"), "Keep leading zeros");
        check(tokens.get(2).lexeme().equals(big), "No int limit");
        check(tokens.get(3).type() == TokenType.MINUS
            && tokens.get(4).type() == TokenType.NUMBER, "Sign is a separate operator");
    }

    private static void checkErrors() {
        for (String invalid : List.of("=", "!", "!=", "/", ".", "#", "data", "UNKNOWN", "١")) {
            try {
                new Lexer("DATA 1\r\n\n" + invalid).tokenize();
                throw new AssertionError("Should reject: " + invalid);
            } catch (LexicalException error) {
                check(error.getLine() == 3, "Error line: " + invalid);
                check(error.getMessage().contains("Linea 3:"), "Message with line");
            }
        }
    }

    private static void checkErrorMessages() {
        check(lexicalError("DATA 1\nMAP + 1 !").equals("Linea 2: Caracter no reconocido: !"),
            "Visible character as is");
        check(lexicalError("DATA 1\nMAP * 2").contains("Caracter no reconocido: U+00A0"),
            "Invisible character by its code");
        check(lexicalError("DATA 1\nMAP ∗ 2").contains("Caracter no reconocido: U+2217"),
            "Look-alike character by its code");
        check(lexicalError("DATA 1\nFILTÉR > 2").equals("Linea 2: Palabra no reconocida: FILTÉR"),
            "Whole word with accent");
        check(lexicalError("DATA 1\nMAP + 1 año").equals("Linea 2: Palabra no reconocida: año"),
            "Whole word with enie");
    }

    private static String lexicalError(String source) {
        try {
            new Lexer(source).tokenize();
        } catch (LexicalException error) {
            return error.getMessage();
        }
        throw new AssertionError("Should reject: " + source);
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
