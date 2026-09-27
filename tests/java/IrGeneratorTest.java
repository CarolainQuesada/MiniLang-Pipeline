import minilang.ir.IrGenerator;
import minilang.lexer.Lexer;
import minilang.lexer.LexicalException;
import minilang.parser.Parser;
import minilang.parser.SyntaxException;
import minilang.semantic.SemanticAnalyzer;
import minilang.semantic.SemanticException;

/** Checks the textual IR contract from analysis to serialization. */
public final class IrGeneratorTest {
    public static void main(String[] args) {
        assertIr(
            "DATA 3 8 5 10 12\nFILTER > 5\nMAP * 2\nREDUCE SUM\nPRINT",
            "DATA|3,8,5,10,12\nFILTER|>|5\nMAP|*|2\nREDUCE|SUM\nPRINT\n");
        assertIr(
            "DATA 0 FILTER > 1 FILTER < 2 FILTER >= 3 FILTER <= 4 FILTER == 5 PRINT",
            "DATA|0\nFILTER|>|1\nFILTER|<|2\nFILTER|>=|3\nFILTER|<=|4\nFILTER|==|5\nPRINT\n");
        assertIr("DATA 1 MAP + 0 MAP - 2 MAP * 3 PRINT",
            "DATA|1\nMAP|+|0\nMAP|-|2\nMAP|*|3\nPRINT\n");
        assertIr("DATA 1 REDUCE SUM PRINT", "DATA|1\nREDUCE|SUM\nPRINT\n");
        assertIr("DATA 1 REDUCE MAX PRINT", "DATA|1\nREDUCE|MAX\nPRINT\n");
        assertIr("DATA 1 REDUCE MIN PRINT", "DATA|1\nREDUCE|MIN\nPRINT\n");
        String big = "999999999999999999999999999999999999999";
        assertIr("DATA 000 007 " + big + " FILTER == " + big + " MAP + 002 PRINT",
            "DATA|0,7," + big + "\nFILTER|==|" + big + "\nMAP|+|2\nPRINT\n");
        assertIr("\r\nDATA\t1\r\nMAP\r-\n2\r\nPRINT\r\n", "DATA|1\nMAP|-|2\nPRINT\n");
        assertRejected("DATA 1 MAP / 2 PRINT", LexicalException.class);
        assertRejected("DATA 1 MAP + 2", SyntaxException.class);
        assertRejected("DATA 1 PRINT", SyntaxException.class);
        assertRejected("DATA 1 REDUCE SUM MAP * 2 PRINT", SemanticException.class);
        System.out.println("PASS: IR contract, operators, normalization and rejection of invalid sources");
    }

    private static String generate(String source) {
        var instructions = new Parser(new Lexer(source).tokenize()).parse();
        new SemanticAnalyzer().check(instructions);
        return new IrGenerator().generate(instructions);
    }

    private static void assertIr(String source, String expected) {
        String actual = generate(source);
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected IR:\n" + expected + "Actual IR:\n" + actual);
        }
    }

    private static void assertRejected(String source, Class<? extends RuntimeException> type) {
        try {
            generate(source);
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Unexpected error", error);
        }
        throw new AssertionError("Must not generate IR for: " + source);
    }
}
