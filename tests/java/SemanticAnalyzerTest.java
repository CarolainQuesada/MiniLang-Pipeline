import java.util.List;
import minilang.lexer.Lexer;
import minilang.model.Instruction;
import minilang.parser.Parser;
import minilang.semantic.SemanticAnalyzer;
import minilang.semantic.SemanticException;

/** Syntactically valid programs that may or may not make sense after REDUCE. */
public final class SemanticAnalyzerTest {
    public static void main(String[] args) {
        assertAccepted("DATA 3 8 5 10 12\nFILTER > 5\nMAP * 2\nREDUCE SUM\nPRINT");
        assertAccepted("DATA 3 8 5\nFILTER > 4\nPRINT");
        assertAccepted("DATA 3 8 5\nREDUCE MAX\nPRINT");
        assertAccepted("DATA 1 2 3\nMAP + 1\nMAP * 2\nFILTER >= 4\nFILTER < 10\nREDUCE MIN\nPRINT");
        assertRejected("DATA 3 8 5\nREDUCE SUM\nMAP * 2\nPRINT", 3, "MAP necesita una lista");
        assertRejected("DATA 3 8 5\nREDUCE SUM\nFILTER > 1\nPRINT", 3, "FILTER necesita una lista");
        assertRejected("DATA 3 8 5\nREDUCE SUM\nREDUCE MAX\nPRINT", 3, "REDUCE de la linea 2");
        assertRejected("DATA 1\nMAP + 1\nREDUCE SUM\n\nMAP * 2\nFILTER > 0\nPRINT", 5, "solo puede venir PRINT");
        System.out.println("PASS: valid operations and rejection of operations after REDUCE with line");
    }

    private static List<Instruction> parse(String source) {
        return new Parser(new Lexer(source).tokenize()).parse();
    }

    private static void assertAccepted(String source) {
        new SemanticAnalyzer().check(parse(source));
    }

    private static void assertRejected(String source, int line, String expected) {
        try {
            assertAccepted(source);
            throw new AssertionError("Should reject: " + source);
        } catch (SemanticException error) {
            check(error.getLine() == line, "Wrong line for: " + source);
            check(error.getMessage().contains("Linea " + line + ":")
                && error.getMessage().contains(expected), "Diagnostic: " + error.getMessage());
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
