package minilang.modelo;

import java.math.BigInteger;
import java.util.Objects;

public final class FilterInstr extends Instruccion {
    public enum Comparador {
        MAYOR(">"), MENOR("<"), MAYOR_O_IGUAL(">="), MENOR_O_IGUAL("<="), IGUAL("==");

        private final String simbolo;

        Comparador(String simbolo) {
            this.simbolo = simbolo;
        }

        public String getSimbolo() {
            return simbolo;
        }
    }

    private final Comparador comparador;
    private final BigInteger numero;

    public FilterInstr(int linea, Comparador comparador, BigInteger numero) {
        super(linea);
        this.comparador = Objects.requireNonNull(comparador, "El comparador es obligatorio");
        this.numero = validarNumero(numero);
    }

    public Comparador getComparador() {
        return comparador;
    }

    public BigInteger getNumero() {
        return numero;
    }

    @Override
    public String getNombre() {
        return "FILTER";
    }
}
