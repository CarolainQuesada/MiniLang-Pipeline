package minilang.modelo;

import java.util.Objects;

public final class ReduceInstr extends Instruccion {
    public enum Tipo {
        SUM, MAX, MIN
    }

    private final Tipo tipo;

    public ReduceInstr(int linea, Tipo tipo) {
        super(linea);
        this.tipo = Objects.requireNonNull(tipo, "El tipo de reduccion es obligatorio");
    }

    public Tipo getTipo() {
        return tipo;
    }

    @Override
    public String getNombre() {
        return "REDUCE";
    }
}
