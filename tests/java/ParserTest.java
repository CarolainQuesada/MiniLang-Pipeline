import java.math.BigInteger;
import java.util.List;
import minilang.lexer.Lexer;
import minilang.model.DataInstr;
import minilang.model.FilterInstr;
import minilang.model.Instruction;
import minilang.model.MapInstr;
import minilang.model.ReduceInstr;
import minilang.parser.Parser;
import minilang.parser.SyntaxException;

/** Syntax and model construction cases; does not run the pipeline. */
public final class ParserTest {
    public static void main(String[] args) {
        checkExample();
        checkVariants();
        checkErrors();
        System.out.println("PASS: model, grammar variants and syntax errors with line");
    }

    private static List<Instruction> parse(String source) {
        return new Parser(new Lexer(source).tokenize()).parse();
    }

    private static void checkExample() {
        Parser parser = new Parser(new Lexer(
            "DATA 3 8 5 10 12\r\nFILTER > 5\r\nMAP * 2\r\nREDUCE SUM\r\nPRINT").tokenize());
        List<Instruction> instructions = parser.parse();
        check(instructions.stream().map(Instruction::getName).toList()
            .equals(List.of("DATA", "FILTER", "MAP", "REDUCE", "PRINT")), "Model order");
        check(((DataInstr) instructions.get(0)).getNumbers().equals(
            List.of(3, 8, 5, 10, 12).stream().map(BigInteger::valueOf).toList()), "DATA numbers");
        FilterInstr filter = (FilterInstr) instructions.get(1);
        check(filter.getComparison() == FilterInstr.Comparison.GREATER
            && filter.getOperand().equals(BigInteger.valueOf(5)), "FILTER model");
        MapInstr map = (MapInstr) instructions.get(2);
        check(map.getOperator() == MapInstr.Operator.MULTIPLY
            && map.getOperand().equals(BigInteger.TWO), "MAP model");
        check(((ReduceInstr) instructions.get(3)).getAggregate() == ReduceInstr.Aggregate.SUM,
            "REDUCE model");
        for (int i = 0; i < instructions.size(); i++) {
            check(instructions.get(i).getLine() == i + 1, "Instruction line");
        }
        check(parser.parse().size() == 5, "Reusable parser");
        try {
            instructions.clear();
            throw new AssertionError("The list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Immutable result confirmed.
        }
    }

    private static void checkVariants() {
        for (FilterInstr.Comparison comparison : FilterInstr.Comparison.values()) {
            FilterInstr filter = (FilterInstr) parse("DATA 0 FILTER" + comparison.getSymbol() + "0 PRINT").get(1);
            check(filter.getComparison() == comparison, "Comparison " + comparison);
        }
        for (MapInstr.Operator operator : MapInstr.Operator.values()) {
            MapInstr map = (MapInstr) parse("DATA 0 MAP " + operator.getSymbol() + " 0 PRINT").get(1);
            check(map.getOperator() == operator, "Operator " + operator);
        }
        for (ReduceInstr.Aggregate aggregate : ReduceInstr.Aggregate.values()) {
            ReduceInstr reduce = (ReduceInstr) parse("DATA 0 REDUCE " + aggregate + " PRINT").get(1);
            check(reduce.getAggregate() == aggregate, "Aggregate " + aggregate);
        }
        String big = "999999999999999999999999999999999999999";
        List<Instruction> instructions = parse("\nDATA\n007 " + big + "\nMAP\n-\n" + big + " PRINT\n");
        check(((DataInstr) instructions.get(0)).getNumbers()
            .equals(List.of(BigInteger.valueOf(7), new BigInteger(big))), "Numbers without int limit");
        check(((MapInstr) instructions.get(1)).getOperand().equals(new BigInteger(big)), "Big operand");
        check(instructions.get(0).getLine() == 2 && instructions.get(1).getLine() == 4, "Start line");
        check(parse("DATA 1 REDUCE SUM REDUCE MAX MAP + 2 FILTER == 3 PRINT").size() == 6,
            "The grammar allows operations after REDUCE; the semantic analyzer rejects them");
    }

    private static void checkErrors() {
        assertRejected("", 1, "DATA");
        assertRejected("MAP + 1 PRINT", 1, "DATA");
        assertRejected("DATA\nPRINT", 2, "entero no negativo");
        assertRejected("DATA 1\nPRINT", 2, "una operacion");
        assertRejected("DATA -1 MAP + 1 PRINT", 1, "entero no negativo");
        assertRejected("DATA 1\nFILTER + 2 PRINT", 2, "comparador");
        assertRejected("DATA 1\nFILTER > -2 PRINT", 2, "entero no negativo");
        assertRejected("DATA 1\nFILTER >\nPRINT", 3, "entero no negativo");
        assertRejected("DATA 1\nMAP > 2 PRINT", 2, "operador");
        assertRejected("DATA 1\nMAP + -2 PRINT", 2, "entero no negativo");
        assertRejected("DATA 1\nMAP +", 2, "entero no negativo");
        assertRejected("DATA 1\nREDUCE PRINT", 2, "SUM, MAX o MIN");
        assertRejected("DATA 1\nREDUCE", 2, "SUM, MAX o MIN");
        assertRejected("DATA 1\nMAP + 2\n", 3, "PRINT");
        assertRejected("DATA 1 MAP + 2 DATA 3 PRINT", 1, "PRINT");
        assertRejected("DATA 1 MAP + 2 3 PRINT", 1, "PRINT");
        assertRejected("DATA 1 MAP + 2 PRINT\nPRINT", 2, "fin del programa");
        assertRejected("DATA 1 MAP + 2 PRINT\nMAP + 1", 2, "fin del programa");
        assertRejected("DATA 1\n", 2, "una operacion");
    }

    private static void assertRejected(String source, int line, String expected) {
        try {
            parse(source);
            throw new AssertionError("Should reject: " + source);
        } catch (SyntaxException error) {
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
