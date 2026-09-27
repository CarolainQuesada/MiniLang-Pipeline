package minilang.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import minilang.lexer.TipoToken;
import minilang.lexer.Token;
import minilang.modelo.DataInstr;
import minilang.modelo.FilterInstr;
import minilang.modelo.Instruccion;
import minilang.modelo.MapInstr;
import minilang.modelo.PrintInstr;
import minilang.modelo.ReduceInstr;

/** Analizador descendente: una funcion por produccion de la gramatica. */
public final class Parser {
    private final List<Token> tokens;
    private int posicion;

    public Parser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        if (this.tokens.isEmpty()
                || this.tokens.get(this.tokens.size() - 1).tipo() != TipoToken.EOF) {
            throw new IllegalArgumentException("La lista de tokens debe terminar en EOF");
        }
        for (int i = 0; i < this.tokens.size() - 1; i++) {
            if (this.tokens.get(i).tipo() == TipoToken.EOF) {
                throw new IllegalArgumentException("EOF solo puede aparecer al final");
            }
        }
    }

    public List<Instruccion> parsear() {
        posicion = 0;
        List<Instruccion> instrucciones = new ArrayList<>();
        instrucciones.add(leerData());
        instrucciones.add(leerOperacion());
        while (esOperacion(actual().tipo())) {
            instrucciones.add(leerOperacion());
        }
        Token imprimir = exigir(TipoToken.PRINT, "PRINT");
        instrucciones.add(new PrintInstr(imprimir.linea()));
        exigir(TipoToken.EOF, "fin del programa despues de PRINT");
        return List.copyOf(instrucciones);
    }

    private DataInstr leerData() {
        Token inicio = exigir(TipoToken.DATA, "DATA al inicio del programa");
        List<BigInteger> numeros = new ArrayList<>();
        numeros.add(leerNumero());
        while (actual().tipo() == TipoToken.NUMERO) {
            numeros.add(leerNumero());
        }
        return new DataInstr(inicio.linea(), numeros);
    }

    private Instruccion leerOperacion() {
        return switch (actual().tipo()) {
            case FILTER -> leerFilter();
            case MAP -> leerMap();
            case REDUCE -> leerReduce();
            default -> throw error("una operacion FILTER, MAP o REDUCE");
        };
    }

    private FilterInstr leerFilter() {
        Token inicio = exigir(TipoToken.FILTER, "FILTER");
        FilterInstr.Comparador comparador = switch (actual().tipo()) {
            case MAYOR -> FilterInstr.Comparador.MAYOR;
            case MENOR -> FilterInstr.Comparador.MENOR;
            case MAYOR_O_IGUAL -> FilterInstr.Comparador.MAYOR_O_IGUAL;
            case MENOR_O_IGUAL -> FilterInstr.Comparador.MENOR_O_IGUAL;
            case IGUAL -> FilterInstr.Comparador.IGUAL;
            default -> throw error("un comparador >, <, >=, <= o ==");
        };
        posicion++;
        return new FilterInstr(inicio.linea(), comparador, leerNumero());
    }

    private MapInstr leerMap() {
        Token inicio = exigir(TipoToken.MAP, "MAP");
        MapInstr.Operador operador = switch (actual().tipo()) {
            case MAS -> MapInstr.Operador.SUMA;
            case MENOS -> MapInstr.Operador.RESTA;
            case ASTERISCO -> MapInstr.Operador.MULTIPLICACION;
            default -> throw error("un operador +, - o *");
        };
        posicion++;
        return new MapInstr(inicio.linea(), operador, leerNumero());
    }

    private ReduceInstr leerReduce() {
        Token inicio = exigir(TipoToken.REDUCE, "REDUCE");
        ReduceInstr.Tipo tipo = switch (actual().tipo()) {
            case SUM -> ReduceInstr.Tipo.SUM;
            case MAX -> ReduceInstr.Tipo.MAX;
            case MIN -> ReduceInstr.Tipo.MIN;
            default -> throw error("SUM, MAX o MIN");
        };
        posicion++;
        return new ReduceInstr(inicio.linea(), tipo);
    }

    private BigInteger leerNumero() {
        return new BigInteger(exigir(TipoToken.NUMERO, "un entero no negativo").lexema());
    }

    private Token exigir(TipoToken tipo, String esperado) {
        if (actual().tipo() != tipo) {
            throw error(esperado);
        }
        Token token = actual();
        if (tipo != TipoToken.EOF) {
            posicion++;
        }
        return token;
    }

    private Token actual() {
        return tokens.get(posicion);
    }

    private ErrorSintactico error(String esperado) {
        Token token = actual();
        String encontrado = token.tipo() == TipoToken.EOF ? "fin del archivo" : "'" + token.lexema() + "'";
        return new ErrorSintactico(token.linea(), "Se esperaba " + esperado + "; se encontro " + encontrado);
    }

    private static boolean esOperacion(TipoToken tipo) {
        return tipo == TipoToken.FILTER || tipo == TipoToken.MAP || tipo == TipoToken.REDUCE;
    }
}
