package minilang;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import minilang.lexer.LexicalException;
import minilang.parser.SyntaxException;
import minilang.semantic.SemanticException;

/**
 * Entry point of the Java stage of the MiniLang pipeline.
 *
 * <p>The application reads {@code programa.mini}, validates the complete source,
 * generates the IR contract, and writes the result to {@code programa.ir}.
 * It exits with codes 0, 1 or 2 depending on the final status of execution.</p>
 */
public final class Main {
    private static final Path INPUT = Path.of("programa.mini");
    private static final Path OUTPUT = Path.of("programa.ir");

    private Main() {
    }

    /**
     * Runs the Java build pipeline without accepting any command-line arguments.
     *
     * @param args command-line parameters; the program rejects any value other than none
     */
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
