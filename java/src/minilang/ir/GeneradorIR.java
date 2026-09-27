package minilang.ir;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import minilang.modelo.DataInstr;
import minilang.modelo.FilterInstr;
import minilang.modelo.Instruccion;
import minilang.modelo.MapInstr;
import minilang.modelo.PrintInstr;
import minilang.modelo.ReduceInstr;

/** Serializa el modelo validado por el parser; no ejecuta operaciones ni escribe archivos. */
public final class GeneradorIR {
    public String generar(List<Instruccion> instrucciones) {
        Objects.requireNonNull(instrucciones, "Las instrucciones son obligatorias");
        return instrucciones.stream()
            .map(this::generarLinea)
            .collect(Collectors.joining("\n", "", "\n"));
    }

    private String generarLinea(Instruccion instruccion) {
        Objects.requireNonNull(instruccion, "La instruccion es obligatoria");
        return switch (instruccion) {
            case DataInstr data -> "DATA|" + data.getNumeros().stream()
                .map(Object::toString).collect(Collectors.joining(","));
            case FilterInstr filtro -> "FILTER|" + filtro.getComparador().getSimbolo()
                + "|" + filtro.getNumero();
            case MapInstr mapa -> "MAP|" + mapa.getOperador().getSimbolo()
                + "|" + mapa.getNumero();
            case ReduceInstr reduccion -> "REDUCE|" + reduccion.getTipo().name();
            case PrintInstr imprimir -> "PRINT";
            default -> throw new IllegalArgumentException(
                "Instruccion no soportada: " + instruccion.getClass().getName());
        };
    }
}
