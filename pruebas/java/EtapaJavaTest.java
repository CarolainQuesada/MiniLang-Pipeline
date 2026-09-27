import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Ejecuta la aplicacion real en carpetas temporales aisladas. */
public final class EtapaJavaTest {
    private static final String FUENTE = "DATA 3 8 5 10 12\nFILTER > 5\nMAP * 2\nREDUCE SUM\nPRINT\n";
    private static final String IR = "DATA|3,8,5,10,12\nFILTER|>|5\nMAP|*|2\nREDUCE|SUM\nPRINT\n";

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("minilang-java-test-");
        try {
            Path entrada = carpeta.resolve("programa.mini");
            Path salida = carpeta.resolve("programa.ir");
            comprobar(ejecutar(carpeta, "Error de lectura/escritura:") == 1, "Entrada ausente");
            comprobar(!Files.exists(salida), "No crear IR sin entrada");

            Files.writeString(entrada, "DATA 1\nMAP / 2\nPRINT\n", StandardCharsets.UTF_8);
            comprobar(ejecutar(carpeta, "Linea 2:") == 1, "Error lexico");
            comprobar(!Files.exists(salida), "No crear IR con error lexico");

            Files.writeString(entrada, "DATA 1\nMAP +\nPRINT\n", StandardCharsets.UTF_8);
            comprobar(ejecutar(carpeta, "Linea 3:") == 1, "Error sintactico");
            comprobar(!Files.exists(salida), "No crear IR con error sintactico");

            Files.writeString(entrada, FUENTE, StandardCharsets.UTF_8);
            comprobar(ejecutar(carpeta, "Se genero programa.ir.") == 0, "Compilacion valida");
            comprobar(Arrays.equals(Files.readAllBytes(salida), IR.getBytes(StandardCharsets.UTF_8)),
                "Archivo IR exacto en UTF-8, sin BOM y con LF");
            comprobar(Files.readString(entrada).equals(FUENTE), "Fuente intacta");

            Files.writeString(entrada, "DATA 1 PRINT", StandardCharsets.UTF_8);
            comprobar(ejecutar(carpeta, "Linea 1:") == 1, "Rechazar despues de exito");
            comprobar(Files.readString(salida).equals(IR), "Conservar IR anterior si falla el analisis");

            Files.writeString(entrada, "DATA 0 REDUCE MAX PRINT", StandardCharsets.UTF_8);
            comprobar(ejecutar(carpeta, "Se genero programa.ir.") == 0, "Reemplazo valido");
            comprobar(Files.readString(salida).equals("DATA|0\nREDUCE|MAX\nPRINT\n"), "Reemplazar sin residuos");

            Files.delete(salida);
            Files.createDirectory(salida);
            comprobar(ejecutar(carpeta, "Error de lectura/escritura:") == 1, "Error al escribir");
            comprobar(Files.isDirectory(salida), "Conservar directorio existente");
            System.out.println("PASS: etapa Java real, archivos, diagnosticos y codigos de salida");
        } finally {
            try (var rutas = Files.walk(carpeta)) {
                for (Path ruta : rutas.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(ruta);
                }
            }
        }
    }

    private static int ejecutar(Path carpeta, String mensaje) throws Exception {
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(ruta -> Path.of(ruta).toAbsolutePath().toString())
            .collect(Collectors.joining(File.pathSeparator));
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Path registro = carpeta.resolve("salida.log");
        Process proceso = new ProcessBuilder(java, "-cp", classpath, "minilang.Main")
            .directory(carpeta.toFile()).redirectErrorStream(true).redirectOutput(registro.toFile()).start();
        if (!proceso.waitFor(15, TimeUnit.SECONDS)) {
            proceso.destroyForcibly().waitFor();
            throw new AssertionError("Tiempo de ejecucion excedido");
        }
        String texto = Files.readString(registro, StandardCharsets.UTF_8);
        comprobar(texto.contains(mensaje), "Diagnostico esperado: " + mensaje + "; obtenido: " + texto);
        return proceso.exitValue();
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
    }
}
