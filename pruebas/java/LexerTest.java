import java.util.List;
import minilang.lexer.ErrorLexico;
import minilang.lexer.Lexer;
import minilang.lexer.TipoToken;
import minilang.lexer.Token;

/** Pruebas exclusivas del lexer, sin dependencias externas. */
public final class LexerTest {
    public static void main(String[] args) {
        comprobarEjemplo();
        comprobarOperadores();
        comprobarLineas();
        comprobarNumeros();
        comprobarErrores();
        comprobar(new Lexer("").tokenizar().equals(List.of(new Token(TipoToken.EOF, "", 1))),
            "Fuente vacia");
        System.out.println("PASS: ejemplo, operadores, lineas, numeros y errores lexicos");
    }

    private static void comprobarEjemplo() {
        Lexer lexer = new Lexer("DATA 3 8 5 10 12\nFILTER > 5\nMAP * 2\nREDUCE SUM\nPRINT");
        List<Token> tokens = lexer.tokenizar();
        comprobar(tokens.stream().map(Token::tipo).toList().equals(List.of(
            TipoToken.DATA, TipoToken.NUMERO, TipoToken.NUMERO, TipoToken.NUMERO,
            TipoToken.NUMERO, TipoToken.NUMERO, TipoToken.FILTER, TipoToken.MAYOR,
            TipoToken.NUMERO, TipoToken.MAP, TipoToken.ASTERISCO, TipoToken.NUMERO,
            TipoToken.REDUCE, TipoToken.SUM, TipoToken.PRINT, TipoToken.EOF)), "Tipos del ejemplo");
        comprobar(tokens.get(5).lexema().equals("12"), "Lexema del numero");
        comprobar(tokens.get(6).linea() == 2 && tokens.get(14).linea() == 5, "Lineas del ejemplo");
        comprobar(lexer.tokenizar().equals(tokens), "Tokenizacion repetible");
    }

    private static void comprobarOperadores() {
        List<TipoToken> tipos = new Lexer("> < >= <= == + - * SUM MAX MIN").tokenizar()
            .stream().map(Token::tipo).toList();
        comprobar(tipos.equals(List.of(TipoToken.MAYOR, TipoToken.MENOR,
            TipoToken.MAYOR_O_IGUAL, TipoToken.MENOR_O_IGUAL, TipoToken.IGUAL,
            TipoToken.MAS, TipoToken.MENOS, TipoToken.ASTERISCO,
            TipoToken.SUM, TipoToken.MAX, TipoToken.MIN, TipoToken.EOF)), "Operadores exactos");
        comprobar(new Lexer("FILTER>=5").tokenizar().get(1)
            .equals(new Token(TipoToken.MAYOR_O_IGUAL, ">=", 1)), "Operador sin espacios");
    }

    private static void comprobarLineas() {
        List<Token> tokens = new Lexer("\tDATA 0\r\n\rFILTER<1\nPRINT\n").tokenizar();
        comprobar(tokens.get(2).linea() == 3, "CRLF cuenta una linea y CR otra");
        comprobar(tokens.get(5).linea() == 4, "Salto LF");
        comprobar(tokens.get(6).equals(new Token(TipoToken.EOF, "", 5)), "Linea de EOF");
    }

    private static void comprobarNumeros() {
        String grande = "999999999999999999999999999999999999999";
        List<Token> tokens = new Lexer("0 007 " + grande + " -2").tokenizar();
        comprobar(tokens.get(0).lexema().equals("0"), "Cero");
        comprobar(tokens.get(1).lexema().equals("007"), "Conservar ceros iniciales");
        comprobar(tokens.get(2).lexema().equals(grande), "Sin limite de int");
        comprobar(tokens.get(3).tipo() == TipoToken.MENOS
            && tokens.get(4).tipo() == TipoToken.NUMERO, "El signo es un operador separado");
    }

    private static void comprobarErrores() {
        for (String invalido : List.of("=", "!", "!=", "/", ".", "#", "data", "UNKNOWN", "\u0661")) {
            try {
                new Lexer("DATA 1\r\n\n" + invalido).tokenizar();
                throw new AssertionError("Debio rechazar: " + invalido);
            } catch (ErrorLexico error) {
                comprobar(error.getLinea() == 3, "Linea del error: " + invalido);
                comprobar(error.getMessage().contains("Linea 3:"), "Mensaje con linea");
            }
        }
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
    }
}
