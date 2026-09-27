package minilang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import minilang.ir.GeneradorIR;
import minilang.lexer.Lexer;
import minilang.parser.Parser;

/** Conecta lectura, analisis y escritura de la etapa Java. */
public final class Compilador {
    public void compilar(Path entrada, Path salida) throws IOException {
        String fuente = Files.readString(entrada, StandardCharsets.UTF_8);
        var instrucciones = new Parser(new Lexer(fuente).tokenizar()).parsear();
        String ir = new GeneradorIR().generar(instrucciones);
        // La salida solo se abre cuando todo el programa ha sido validado.
        Files.writeString(salida, ir, StandardCharsets.UTF_8);
    }
}
