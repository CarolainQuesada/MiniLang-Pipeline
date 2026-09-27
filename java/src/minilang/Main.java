package minilang;

import java.io.IOException;
import java.nio.file.Path;
import minilang.lexer.ErrorLexico;
import minilang.parser.ErrorSintactico;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Uso: java -cp java/build minilang.Main (sin argumentos)");
            System.exit(2);
            return;
        }
        try {
            new Compilador().compilar(Path.of("programa.mini"), Path.of("programa.ir"));
            System.out.println("Programa valido. Se genero programa.ir.");
        } catch (ErrorLexico | ErrorSintactico error) {
            System.err.println(error.getMessage());
            System.exit(1);
        } catch (IOException error) {
            System.err.println("Error de lectura/escritura: " + error.getMessage());
            System.exit(1);
        }
    }
}
