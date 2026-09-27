import ast
import sys
import unittest
from pathlib import Path

PYTHON_DIR = Path(__file__).resolve().parents[2] / "python"
sys.path.insert(0, str(PYTHON_DIR))

from ir_parser import Operation, parse  # noqa: E402
from operations import ExecutionError, Step, apply_filter, apply_map, apply_reduce, execute  # noqa: E402

DATA = (3, 8, 5, 10, 12)


class FilterTest(unittest.TestCase):
    def test_every_comparator(self):
        cases = (
            (">", 5, (8, 10, 12)),
            ("<", 5, (3,)),
            (">=", 8, (8, 10, 12)),
            ("<=", 8, (3, 8, 5)),
            ("==", 10, (10,)),
        )
        for symbol, operand, expected in cases:
            with self.subTest(symbol=symbol):
                self.assertEqual(apply_filter(DATA, Operation(1, "FILTER", symbol, operand)), expected)

    def test_can_leave_the_list_empty(self):
        self.assertEqual(apply_filter(DATA, Operation(1, "FILTER", ">", 100)), ())


class MapTest(unittest.TestCase):
    def test_every_operator(self):
        self.assertEqual(apply_map(DATA, Operation(1, "MAP", "+", 1)), (4, 9, 6, 11, 13))
        self.assertEqual(apply_map(DATA, Operation(1, "MAP", "*", 2)), (6, 16, 10, 20, 24))
        self.assertEqual(apply_map(DATA, Operation(1, "MAP", "-", 10)), (-7, -2, -5, 0, 2))

    def test_empty_list_stays_empty(self):
        self.assertEqual(apply_map((), Operation(1, "MAP", "*", 2)), ())


class ReduceTest(unittest.TestCase):
    def test_every_aggregate(self):
        self.assertEqual(apply_reduce(DATA, Operation(1, "REDUCE", "SUM")), 38)
        self.assertEqual(apply_reduce(DATA, Operation(1, "REDUCE", "MAX")), 12)
        self.assertEqual(apply_reduce(DATA, Operation(1, "REDUCE", "MIN")), 3)
        self.assertEqual(apply_reduce((-7, -2), Operation(1, "REDUCE", "MAX")), -2)

    def test_sum_of_empty_list_is_zero(self):
        self.assertEqual(apply_reduce((), Operation(1, "REDUCE", "SUM")), 0)

    def test_max_and_min_of_empty_list_are_errors(self):
        for symbol in ("MAX", "MIN"):
            with self.subTest(symbol=symbol), self.assertRaises(ExecutionError) as context:
                apply_reduce((), Operation(4, "REDUCE", symbol))
            self.assertEqual(context.exception.line, 4)
            self.assertEqual(str(context.exception), f"programa.ir, linea 4: REDUCE {symbol} no se puede "
                                                     "aplicar a una lista vacia; FILTER no dejo ningun elemento")


class ExecuteTest(unittest.TestCase):
    def test_example_from_assignment(self):
        program = parse("DATA|3,8,5,10,12\nFILTER|>|5\nMAP|*|2\nREDUCE|SUM\nPRINT\n")
        result, steps = execute(program.data, program.operations)
        self.assertEqual(result, 60)
        self.assertEqual(steps, (
            Step("FILTER > 5", (8, 10, 12)),
            Step("MAP * 2", (16, 20, 24)),
            Step("REDUCE SUM", 60),
        ))

    def test_consecutive_operations(self):
        program = parse("DATA|1,2,3,4\nMAP|+|1\nMAP|*|3\nFILTER|>|6\nFILTER|<|15\nPRINT\n")
        result, steps = execute(program.data, program.operations)
        self.assertEqual(result, (9, 12))
        self.assertEqual(len(steps), 4)

    def test_without_reduce_the_result_is_the_list(self):
        program = parse("DATA|3,8\nMAP|-|10\nPRINT\n")
        self.assertEqual(execute(program.data, program.operations)[0], (-7, -2))


class FunctionalStyleTest(unittest.TestCase):
    """The assignment asks to avoid for/while loops; this stage uses none at all."""

    def test_no_for_while_or_comprehension_in_python_stage(self):
        forbidden = (ast.For, ast.While, ast.comprehension)
        for source in sorted(PYTHON_DIR.glob("*.py")):
            with self.subTest(file=source.name):
                nodes = ast.walk(ast.parse(source.read_text(encoding="utf-8")))
                loops = list(filter(lambda node: isinstance(node, forbidden), nodes))
                self.assertEqual(loops, [], f"{source.name} line {getattr(loops[0], 'lineno', '?') if loops else ''}")


if __name__ == "__main__":
    unittest.main()
