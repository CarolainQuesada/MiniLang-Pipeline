import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Runs the real application in isolated temporary folders. */
public final class JavaStageTest {
    private static final String SOURCE = "DATA 3 8 5 10 12\nFILTER > 5\nMAP * 2\nREDUCE SUM\nPRINT\n";
    private static final String IR = "DATA|3,8,5,10,12\nFILTER|>|5\nMAP|*|2\nREDUCE|SUM\nPRINT\n";

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("minilang-java-test-");
        try {
            Path input = folder.resolve("programa.mini");
            Path output = folder.resolve("programa.ir");
            check(run(folder, "No se encontro programa.mini") == 1, "Missing input");
            check(!Files.exists(output), "No IR without input");

            // Windows PowerShell 5.1 saves with '>' as UTF-16 with BOM.
            Files.write(input, "﻿DATA 1\nMAP + 1\nPRINT\n".getBytes(StandardCharsets.UTF_16LE));
            check(run(folder, "no esta guardado en UTF-8") == 1, "Input that is not UTF-8");
            check(!Files.exists(output), "No IR without UTF-8");

            Files.writeString(input, "DATA 1\nREDUCE SUM\nMAP * 2\nPRINT\n", StandardCharsets.UTF_8);
            check(run(folder, "Linea 3:") == 1, "Semantic error");
            check(!Files.exists(output), "No IR with semantic error");

            Files.writeString(input, "DATA 1\nMAP / 2\nPRINT\n", StandardCharsets.UTF_8);
            check(run(folder, "Linea 2:") == 1, "Lexical error");
            check(!Files.exists(output), "No IR with lexical error");

            Files.writeString(input, "DATA 1\nMAP +\nPRINT\n", StandardCharsets.UTF_8);
            check(run(folder, "Linea 3:") == 1, "Syntax error");
            check(!Files.exists(output), "No IR with syntax error");

            Files.writeString(input, SOURCE, StandardCharsets.UTF_8);
            check(run(folder, "Se genero programa.ir.") == 0, "Valid compilation");
            check(Arrays.equals(Files.readAllBytes(output), IR.getBytes(StandardCharsets.UTF_8)),
                "Exact IR file in UTF-8, without BOM and with LF");
            check(Files.readString(input).equals(SOURCE), "Source untouched");

            check(run(folder, "Uso:", "unexpected") == 2, "Unsupported arguments");
            check(Files.readString(output).equals(IR), "Keep IR on invalid arguments");
            check(Files.readString(input).equals(SOURCE), "Keep source on invalid arguments");

            Files.writeString(input, "DATA 1\nMAP / 2\nPRINT", StandardCharsets.UTF_8);
            check(run(folder, "Linea 2:") == 1, "Lexical error after success");
            check(Files.readString(output).equals(IR), "Keep previous IR on lexical error");

            Files.writeString(input, "DATA 1 PRINT", StandardCharsets.UTF_8);
            check(run(folder, "Linea 1:") == 1, "Reject after success");
            check(Files.readString(output).equals(IR), "Keep previous IR when analysis fails");

            Files.writeString(input, "DATA 0 REDUCE MAX PRINT", StandardCharsets.UTF_8);
            check(run(folder, "Se genero programa.ir.") == 0, "Valid replacement");
            check(Files.readString(output).equals("DATA|0\nREDUCE|MAX\nPRINT\n"), "Replace without leftovers");

            Files.writeString(input, "﻿" + SOURCE, StandardCharsets.UTF_8);
            check(run(folder, "Se genero programa.ir.") == 0, "UTF-8 input with BOM");
            check(Files.readString(output).equals(IR), "BOM does not reach the IR");

            Files.delete(output);
            Files.createDirectory(output);
            check(run(folder, "Error de lectura/escritura:") == 1, "Write error");
            check(Files.isDirectory(output), "Keep existing directory");
            System.out.println("PASS: real Java stage, files, diagnostics and exit codes");
        } finally {
            try (var paths = Files.walk(folder)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static int run(Path folder, String expectedMessage, String... arguments) throws Exception {
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(path -> Path.of(path).toAbsolutePath().toString())
            .collect(Collectors.joining(File.pathSeparator));
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Path stdoutLog = folder.resolve("stdout.log");
        Path stderrLog = folder.resolve("stderr.log");
        List<String> command = new ArrayList<>(List.of(java, "-cp", classpath, "minilang.Main"));
        command.addAll(Arrays.asList(arguments));
        Process process = new ProcessBuilder(command)
            .directory(folder.toFile()).redirectOutput(stdoutLog.toFile())
            .redirectError(stderrLog.toFile()).start();
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor();
            throw new AssertionError("Execution timed out");
        }
        // The child JVM writes in the platform encoding; decoding leniently keeps a path
        // like C:\Users\José from failing the test. The checked messages are ASCII.
        String stdout = new String(Files.readAllBytes(stdoutLog), StandardCharsets.UTF_8);
        String stderr = new String(Files.readAllBytes(stderrLog), StandardCharsets.UTF_8);
        String diagnostic = process.exitValue() == 0 ? stdout : stderr;
        check(diagnostic.contains(expectedMessage),
            "Expected diagnostic: " + expectedMessage + "; actual: " + diagnostic);
        check(process.exitValue() == 0 ? stderr.isEmpty() : stdout.isEmpty(),
            "Success only on stdout and errors only on stderr");
        return process.exitValue();
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
