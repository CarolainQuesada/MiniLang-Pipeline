import java.math.BigInteger;
import java.util.List;
import minilang.lexer.Lexer;
import minilang.modelo.DataInstr;
import minilang.modelo.FilterInstr;
import minilang.modelo.Instruccion;
import minilang.modelo.MapInstr;
import minilang.modelo.ReduceInstr;
import minilang.parser.ErrorSintactico;
import minilang.parser.Parser;

/** Casos de sintaxis y construccion del modelo; no ejecuta el pipeline. */
public final class ParserTest {
    public static void main(String[] args) {
        comprobarEjemplo();
        comprobarVariantes();
        comprobarErrores();
        System.out.println("PASS: modelo, variantes de gramatica y errores sintacticos con linea");
    }

    private static List<Instruccion> parsear(String fuente) {
        return new Parser(new Lexer(fuente).tokenizar()).parsear();
    }

    private static void comprobarEjemplo() {
        Parser parser = new Parser(new Lexer(
            "DATA 3 8 5 10 12\r\nFILTER > 5\r\nMAP * 2\r\nREDUCE SUM\r\nPRINT").tokenizar());
        List<Instruccion> instrucciones = parser.parsear();
        comprobar(instrucciones.stream().map(Instruccion::getNombre).toList()
            .equals(List.of("DATA", "FILTER", "MAP", "REDUCE", "PRINT")), "Orden del modelo");
        comprobar(((DataInstr) instrucciones.get(0)).getNumeros().equals(
            List.of(3, 8, 5, 10, 12).stream().map(BigInteger::valueOf).toList()), "Numeros DATA");
        FilterInstr filtro = (FilterInstr) instrucciones.get(1);
        comprobar(filtro.getComparador() == FilterInstr.Comparador.MAYOR
            && filtro.getNumero().equals(BigInteger.valueOf(5)), "Modelo FILTER");
        MapInstr mapa = (MapInstr) instrucciones.get(2);
        comprobar(mapa.getOperador() == MapInstr.Operador.MULTIPLICACION
            && mapa.getNumero().equals(BigInteger.TWO), "Modelo MAP");
        comprobar(((ReduceInstr) instrucciones.get(3)).getTipo() == ReduceInstr.Tipo.SUM, "Modelo REDUCE");
        for (int i = 0; i < instrucciones.size(); i++) {
            comprobar(instrucciones.get(i).getLinea() == i + 1, "Linea de la instruccion");
        }
        comprobar(parser.parsear().size() == 5, "Parser reutilizable");
        try {
            instrucciones.clear();
            throw new AssertionError("La lista debe ser inmutable");
        } catch (UnsupportedOperationException esperado) {
            // Resultado inmutable confirmado.
        }
    }

    private static void comprobarVariantes() {
        for (FilterInstr.Comparador comparador : FilterInstr.Comparador.values()) {
            FilterInstr filtro = (FilterInstr) parsear("DATA 0 FILTER" + comparador.getSimbolo() + "0 PRINT").get(1);
            comprobar(filtro.getComparador() == comparador, "Comparador " + comparador);
        }
        for (MapInstr.Operador operador : MapInstr.Operador.values()) {
            MapInstr mapa = (MapInstr) parsear("DATA 0 MAP " + operador.getSimbolo() + " 0 PRINT").get(1);
            comprobar(mapa.getOperador() == operador, "Operador " + operador);
        }
        for (ReduceInstr.Tipo tipo : ReduceInstr.Tipo.values()) {
            ReduceInstr reduccion = (ReduceInstr) parsear("DATA 0 REDUCE " + tipo + " PRINT").get(1);
            comprobar(reduccion.getTipo() == tipo, "Reduccion " + tipo);
        }
        String grande = "999999999999999999999999999999999999999";
        List<Instruccion> instrucciones = parsear("\nDATA\n007 " + grande + "\nMAP\n-\n" + grande + " PRINT\n");
        comprobar(((DataInstr) instrucciones.get(0)).getNumeros()
            .equals(List.of(BigInteger.valueOf(7), new BigInteger(grande))), "Numeros sin limite de int");
        comprobar(((MapInstr) instrucciones.get(1)).getNumero().equals(new BigInteger(grande)), "Operando grande");
        comprobar(instrucciones.get(0).getLinea() == 2 && instrucciones.get(1).getLinea() == 4, "Linea de inicio");
        comprobar(parsear("DATA 1 REDUCE SUM REDUCE MAX MAP + 2 FILTER == 3 PRINT").size() == 6,
            "La gramatica admite operaciones repetidas y operaciones despues de REDUCE");
    }

    private static void comprobarErrores() {
        rechazar("", 1, "DATA");
        rechazar("MAP + 1 PRINT", 1, "DATA");
        rechazar("DATA\nPRINT", 2, "entero no negativo");
        rechazar("DATA 1\nPRINT", 2, "una operacion");
        rechazar("DATA -1 MAP + 1 PRINT", 1, "entero no negativo");
        rechazar("DATA 1\nFILTER + 2 PRINT", 2, "comparador");
        rechazar("DATA 1\nFILTER > -2 PRINT", 2, "entero no negativo");
        rechazar("DATA 1\nFILTER >\nPRINT", 3, "entero no negativo");
        rechazar("DATA 1\nMAP > 2 PRINT", 2, "operador");
        rechazar("DATA 1\nMAP + -2 PRINT", 2, "entero no negativo");
        rechazar("DATA 1\nMAP +", 2, "entero no negativo");
        rechazar("DATA 1\nREDUCE PRINT", 2, "SUM, MAX o MIN");
        rechazar("DATA 1\nREDUCE", 2, "SUM, MAX o MIN");
        rechazar("DATA 1\nMAP + 2\n", 3, "PRINT");
        rechazar("DATA 1 MAP + 2 DATA 3 PRINT", 1, "PRINT");
        rechazar("DATA 1 MAP + 2 3 PRINT", 1, "PRINT");
        rechazar("DATA 1 MAP + 2 PRINT\nPRINT", 2, "fin del programa");
        rechazar("DATA 1 MAP + 2 PRINT\nMAP + 1", 2, "fin del programa");
        rechazar("DATA 1\n", 2, "una operacion");
    }

    private static void rechazar(String fuente, int linea, String esperado) {
        try {
            parsear(fuente);
            throw new AssertionError("Debio rechazar: " + fuente);
        } catch (ErrorSintactico error) {
            comprobar(error.getLinea() == linea, "Linea incorrecta para: " + fuente);
            comprobar(error.getMessage().contains("Linea " + linea + ":")
                && error.getMessage().contains(esperado), "Diagnostico: " + error.getMessage());
        }
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
    }
}
