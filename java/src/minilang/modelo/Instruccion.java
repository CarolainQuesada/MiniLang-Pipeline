package minilang.modelo;

import java.math.BigInteger;
import java.util.Objects;

/** Instruccion del programa con su linea de origen. */
public abstract class Instruccion {
    private final int linea;

    protected Instruccion(int linea) {
        if (linea < 1) {
            throw new IllegalArgumentException("La linea debe ser positiva");
        }
        this.linea = linea;
    }

    public final int getLinea() {
        return linea;
    }

    public abstract String getNombre();

    protected static BigInteger validarNumero(BigInteger numero) {
        Objects.requireNonNull(numero, "El numero es obligatorio");
        if (numero.signum() < 0) {
            throw new IllegalArgumentException("El numero debe ser no negativo");
        }
        return numero;
    }
}
