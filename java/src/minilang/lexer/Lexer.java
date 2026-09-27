package minilang.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Tokeniza texto; no valida la estructura del programa ni ejecuta instrucciones. */
public final class Lexer {
    private static final Map<String, TipoToken> PALABRAS = Map.of(
        "DATA", TipoToken.DATA, "FILTER", TipoToken.FILTER,
        "MAP", TipoToken.MAP, "REDUCE", TipoToken.REDUCE,
        "PRINT", TipoToken.PRINT, "SUM", TipoToken.SUM,
        "MAX", TipoToken.MAX, "MIN", TipoToken.MIN);

    private final String fuente;
    private int posicion;
    private int linea;

    public Lexer(String fuente) {
        this.fuente = Objects.requireNonNull(fuente, "La fuente es obligatoria");
    }

    public List<Token> tokenizar() {
        posicion = 0;
        linea = 1;
        List<Token> tokens = new ArrayList<>();
        while (posicion < fuente.length()) {
            char actual = fuente.charAt(posicion);
            if (actual == ' ' || actual == '\t') {
                posicion++;
            } else if (actual == '\r' || actual == '\n') {
                consumirSalto();
            } else if (esDigito(actual)) {
                tokens.add(leerNumero());
            } else if (esLetra(actual)) {
                tokens.add(leerPalabra());
            } else {
                tokens.add(leerOperador());
            }
        }
        tokens.add(new Token(TipoToken.EOF, "", linea));
        return List.copyOf(tokens);
    }

    private void consumirSalto() {
        char salto = fuente.charAt(posicion++);
        if (salto == '\r' && posicion < fuente.length() && fuente.charAt(posicion) == '\n') {
            posicion++;
        }
        linea++;
    }

    private Token leerNumero() {
        int inicio = posicion;
        while (posicion < fuente.length() && esDigito(fuente.charAt(posicion))) {
            posicion++;
        }
        return new Token(TipoToken.NUMERO, fuente.substring(inicio, posicion), linea);
    }

    private Token leerPalabra() {
        int inicio = posicion;
        while (posicion < fuente.length() && esLetra(fuente.charAt(posicion))) {
            posicion++;
        }
        String palabra = fuente.substring(inicio, posicion);
        TipoToken tipo = PALABRAS.get(palabra);
        if (tipo == null) {
            throw new ErrorLexico(linea, "Palabra no reconocida: " + palabra);
        }
        return new Token(tipo, palabra, linea);
    }

    private Token leerOperador() {
        int inicio = posicion;
        char simbolo = fuente.charAt(posicion++);
        TipoToken tipo = switch (simbolo) {
            case '+' -> TipoToken.MAS;
            case '-' -> TipoToken.MENOS;
            case '*' -> TipoToken.ASTERISCO;
            case '>' -> consumirIgual() ? TipoToken.MAYOR_O_IGUAL : TipoToken.MAYOR;
            case '<' -> consumirIgual() ? TipoToken.MENOR_O_IGUAL : TipoToken.MENOR;
            case '=' -> {
                if (!consumirIgual()) {
                    throw new ErrorLexico(linea, "Operador '=' invalido; se esperaba '=='");
                }
                yield TipoToken.IGUAL;
            }
            default -> throw new ErrorLexico(linea, "Caracter no reconocido: " + simbolo);
        };
        return new Token(tipo, fuente.substring(inicio, posicion), linea);
    }

    private boolean consumirIgual() {
        if (posicion < fuente.length() && fuente.charAt(posicion) == '=') {
            posicion++;
            return true;
        }
        return false;
    }

    private static boolean esDigito(char caracter) {
        return caracter >= '0' && caracter <= '9';
    }

    private static boolean esLetra(char caracter) {
        return (caracter >= 'A' && caracter <= 'Z') || (caracter >= 'a' && caracter <= 'z');
    }
}
