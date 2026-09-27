package minilang;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import minilang.lexer.LexicalException;
import minilang.parser.SyntaxException;
import minilang.semantic.SemanticException;

public final class Main {
    private static final Path INPUT = Path.of("programa.mini");
    private static final Path OUTPUT = Path.of("programa.ir");

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Uso: java -cp java/build minilang.Main (sin argumentos)");
            System.exit(2);
            return;
        }
        try {
            new MiniLangCompiler().compile(INPUT, OUTPUT);
            System.out.println("Programa valido. Se genero programa.ir.");
        } catch (LexicalException | SyntaxException | SemanticException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        } catch (NoSuchFileException error) {
            System.err.println("No se encontro " + error.getFile() + " en "
                + Path.of("").toAbsolutePath() + ". Ejecute desde la carpeta del proyecto.");
            System.exit(1);
        } catch (CharacterCodingException error) {
            System.err.println(INPUT + " no esta guardado en UTF-8. Guardelo con codificacion UTF-8.");
            System.exit(1);
        } catch (IOException error) {
            System.err.println("Error de lectura/escritura: " + error.getMessage());
            System.exit(1);
        }
    }
}
