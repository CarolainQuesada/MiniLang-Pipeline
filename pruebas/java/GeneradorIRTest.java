import minilang.ir.GeneradorIR;
import minilang.lexer.ErrorLexico;
import minilang.lexer.Lexer;
import minilang.parser.ErrorSintactico;
import minilang.parser.Parser;

/** Comprueba el contrato textual de IR desde el analisis hasta la serializacion. */
public final class GeneradorIRTest {
    public static void main(String[] args) {
        comprobar(
            "DATA 3 8 5 10 12\nFILTER > 5\nMAP * 2\nREDUCE SUM\nPRINT",
            "DATA|3,8,5,10,12\nFILTER|>|5\nMAP|*|2\nREDUCE|SUM\nPRINT\n");
        comprobar(
            "DATA 0 FILTER > 1 FILTER < 2 FILTER >= 3 FILTER <= 4 FILTER == 5 PRINT",
            "DATA|0\nFILTER|>|1\nFILTER|<|2\nFILTER|>=|3\nFILTER|<=|4\nFILTER|==|5\nPRINT\n");
        comprobar("DATA 1 MAP + 0 MAP - 2 MAP * 3 PRINT",
            "DATA|1\nMAP|+|0\nMAP|-|2\nMAP|*|3\nPRINT\n");
        comprobar("DATA 1 REDUCE SUM REDUCE MAX REDUCE MIN PRINT",
            "DATA|1\nREDUCE|SUM\nREDUCE|MAX\nREDUCE|MIN\nPRINT\n");
        String grande = "999999999999999999999999999999999999999";
        comprobar("DATA 000 007 " + grande + " FILTER == " + grande + " MAP + 002 PRINT",
            "DATA|0,7," + grande + "\nFILTER|==|" + grande + "\nMAP|+|2\nPRINT\n");
        comprobar("\r\nDATA\t1\r\nMAP\r-\n2\r\nPRINT\r\n", "DATA|1\nMAP|-|2\nPRINT\n");
        comprobarRechazo("DATA 1 MAP / 2 PRINT", ErrorLexico.class);
        comprobarRechazo("DATA 1 MAP + 2", ErrorSintactico.class);
        comprobarRechazo("DATA 1 PRINT", ErrorSintactico.class);
        System.out.println("PASS: contrato IR, operadores, normalizacion y rechazo de fuentes invalidas");
    }

    private static String generar(String fuente) {
        return new GeneradorIR().generar(new Parser(new Lexer(fuente).tokenizar()).parsear());
    }

    private static void comprobar(String fuente, String esperado) {
        String resultado = generar(fuente);
        if (!resultado.equals(esperado)) {
            throw new AssertionError("IR esperado:\n" + esperado + "IR obtenido:\n" + resultado);
        }
    }

    private static void comprobarRechazo(String fuente, Class<? extends RuntimeException> tipo) {
        try {
            generar(fuente);
        } catch (RuntimeException error) {
            if (tipo.isInstance(error)) {
                return;
            }
            throw new AssertionError("Error inesperado", error);
        }
        throw new AssertionError("No debe generar IR para: " + fuente);
    }
}
