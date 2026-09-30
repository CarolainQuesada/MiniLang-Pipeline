package minilang.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import minilang.lexer.Token;
import minilang.lexer.TokenType;
import minilang.model.DataInstr;
import minilang.model.FilterInstr;
import minilang.model.Instruction;
import minilang.model.MapInstr;
import minilang.model.PrintInstr;
import minilang.model.ReduceInstr;

/**
 * Parses the token stream produced by the lexer and builds the instruction model.
 *
 * <p>The grammar accepted by this parser is a simplified pipeline:
 * {@code DATA ... FILTER/MAP/REDUCE ... PRINT}. If an invalid token or structure is found,
 * it throws a syntax exception with the source line and the expected token.</p>
 */
public final class Parser {
    private final List<Token> tokens;
    private int position;

    /**
     * Creates a parser over a complete token stream that must finish in EOF.
     *
     * @param tokens list of tokens obtained from the lexer
     */
    public Parser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        if (this.tokens.isEmpty()
                || this.tokens.get(this.tokens.size() - 1).type() != TokenType.EOF) {
            throw new IllegalArgumentException("token list must end with EOF");
        }
        for (int i = 0; i < this.tokens.size() - 1; i++) {
            if (this.tokens.get(i).type() == TokenType.EOF) {
                throw new IllegalArgumentException("EOF may only appear at the end");
            }
        }
    }

    /**
     * Parses the whole program and returns the instruction model.
     *
     * @return immutable list of instructions ready for semantic checks and IR generation
     */
    public List<Instruction> parse() {
        position = 0;
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(parseData());
        instructions.add(parseOperation());
        while (isOperation(current().type())) {
            instructions.add(parseOperation());
        }
        Token print = expect(TokenType.PRINT, "PRINT");
        instructions.add(new PrintInstr(print.line()));
        expect(TokenType.EOF, "fin del programa despues de PRINT");
        return List.copyOf(instructions);
    }

    private DataInstr parseData() {
        Token keyword = expect(TokenType.DATA, "DATA al inicio del programa");
        List<BigInteger> numbers = new ArrayList<>();
        numbers.add(parseNumber());
        while (current().type() == TokenType.NUMBER) {
            numbers.add(parseNumber());
        }
        return new DataInstr(keyword.line(), numbers);
    }

    private Instruction parseOperation() {
        return switch (current().type()) {
            case FILTER -> parseFilter();
            case MAP -> parseMap();
            case REDUCE -> parseReduce();
            default -> throw unexpected("una operacion FILTER, MAP o REDUCE");
        };
    }

    private FilterInstr parseFilter() {
        Token keyword = expect(TokenType.FILTER, "FILTER");
        FilterInstr.Comparison comparison = switch (current().type()) {
            case GREATER -> FilterInstr.Comparison.GREATER;
            case LESS -> FilterInstr.Comparison.LESS;
            case GREATER_EQUAL -> FilterInstr.Comparison.GREATER_OR_EQUAL;
            case LESS_EQUAL -> FilterInstr.Comparison.LESS_OR_EQUAL;
            case EQUAL_EQUAL -> FilterInstr.Comparison.EQUAL;
            default -> throw unexpected("un comparador >, <, >=, <= o ==");
        };
        position++;
        return new FilterInstr(keyword.line(), comparison, parseNumber());
    }

    private MapInstr parseMap() {
        Token keyword = expect(TokenType.MAP, "MAP");
        MapInstr.Operator operator = switch (current().type()) {
            case PLUS -> MapInstr.Operator.ADD;
            case MINUS -> MapInstr.Operator.SUBTRACT;
            case STAR -> MapInstr.Operator.MULTIPLY;
            default -> throw unexpected("un operador +, - o *");
        };
        position++;
        return new MapInstr(keyword.line(), operator, parseNumber());
    }

    private ReduceInstr parseReduce() {
        Token keyword = expect(TokenType.REDUCE, "REDUCE");
        ReduceInstr.Aggregate aggregate = switch (current().type()) {
            case SUM -> ReduceInstr.Aggregate.SUM;
            case MAX -> ReduceInstr.Aggregate.MAX;
            case MIN -> ReduceInstr.Aggregate.MIN;
            default -> throw unexpected("SUM, MAX o MIN");
        };
        position++;
        return new ReduceInstr(keyword.line(), aggregate);
    }

    private BigInteger parseNumber() {
        return new BigInteger(expect(TokenType.NUMBER, "un entero no negativo").lexeme());
    }

    private Token expect(TokenType type, String expected) {
        if (current().type() != type) {
            throw unexpected(expected);
        }
        Token token = current();
        if (type != TokenType.EOF) {
            position++;
        }
        return token;
    }

    private Token current() {
        return tokens.get(position);
    }

    private SyntaxException unexpected(String expected) {
        Token token = current();
        String found = token.type() == TokenType.EOF ? "fin del archivo" : "'" + token.lexeme() + "'";
        return new SyntaxException(token.line(), "Se esperaba " + expected + "; se encontro " + found);
    }

    private static boolean isOperation(TokenType type) {
        return type == TokenType.FILTER || type == TokenType.MAP || type == TokenType.REDUCE;
    }
}
