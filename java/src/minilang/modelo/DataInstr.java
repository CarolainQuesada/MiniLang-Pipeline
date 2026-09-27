package minilang.modelo;

import java.math.BigInteger;
import java.util.List;

public final class DataInstr extends Instruccion {
    private final List<BigInteger> numeros;

    public DataInstr(int linea, List<BigInteger> numeros) {
        super(linea);
        this.numeros = List.copyOf(numeros);
        if (this.numeros.isEmpty()) {
            throw new IllegalArgumentException("DATA requiere al menos un numero");
        }
        this.numeros.forEach(Instruccion::validarNumero);
    }

    public List<BigInteger> getNumeros() {
        return numeros;
    }

    @Override
    public String getNombre() {
        return "DATA";
    }
}
