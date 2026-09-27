package minilang.modelo;

import java.math.BigInteger;
import java.util.Objects;

public final class MapInstr extends Instruccion {
    public enum Operador {
        SUMA("+"), RESTA("-"), MULTIPLICACION("*");

        private final String simbolo;

        Operador(String simbolo) {
            this.simbolo = simbolo;
        }

        public String getSimbolo() {
            return simbolo;
        }
    }

    private final Operador operador;
    private final BigInteger numero;

    public MapInstr(int linea, Operador operador, BigInteger numero) {
        super(linea);
        this.operador = Objects.requireNonNull(operador, "El operador es obligatorio");
        this.numero = validarNumero(numero);
    }

    public Operador getOperador() {
        return operador;
    }

    public BigInteger getNumero() {
        return numero;
    }

    @Override
    public String getNombre() {
        return "MAP";
    }
}
