package minilang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import minilang.ir.IrGenerator;
import minilang.lexer.Lexer;
import minilang.parser.Parser;
import minilang.semantic.SemanticAnalyzer;

/**
 * Connects the lexical analysis, syntactic validation, semantic checks and IR generation.
 *
 * <p>The compiler reads a source file in UTF-8, strips a BOM if present, parses the program,
 * validates the meaning of the operations and writes the final IR text to disk.</p>
 */
public final class MiniLangCompiler {
    private static final String BOM = "﻿";

    /**
     * Creates the compiler used to process the source program.
     */
    public MiniLangCompiler() {
    }

    /**
     * Compiles the input program into a valid IR file.
     *
     * @param input source file to read, usually {@code programa.mini}
     * @param output destination file, usually {@code programa.ir}
     * @throws IOException if the read or write operation fails
     */
    public void compile(Path input, Path output) throws IOException {
        String source = stripBom(Files.readString(input, StandardCharsets.UTF_8));
        var instructions = new Parser(new Lexer(source).tokenize()).parse();
        new SemanticAnalyzer().check(instructions);
        String ir = new IrGenerator().generate(instructions);
        // The output is opened only after the whole program has been validated.
        Files.writeString(output, ir, StandardCharsets.UTF_8);
    }

    /** Some Windows editors add a BOM when saving as UTF-8; it is not part of the program. */
    private static String stripBom(String source) {
        return source.startsWith(BOM) ? source.substring(BOM.length()) : source;
    }
}
