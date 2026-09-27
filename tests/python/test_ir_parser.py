import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "python"))

from ir_parser import IrFormatError, Operation, parse  # noqa: E402

EXAMPLE = "DATA|3,8,5,10,12\nFILTER|>|5\nMAP|*|2\nREDUCE|SUM\nPRINT\n"


class ParseValidIrTest(unittest.TestCase):
    def test_example_from_assignment(self):
        program = parse(EXAMPLE)
        self.assertEqual(program.data, (3, 8, 5, 10, 12))
        self.assertEqual(program.operations, (
            Operation(2, "FILTER", ">", 5),
            Operation(3, "MAP", "*", 2),
            Operation(4, "REDUCE", "SUM"),
        ))

    def test_every_comparator_operator_and_aggregate(self):
        program = parse("DATA|0\nFILTER|>|1\nFILTER|<|2\nFILTER|>=|3\nFILTER|<=|4\nFILTER|==|5\n"
                        "MAP|+|6\nMAP|-|7\nMAP|*|8\nPRINT\n")
        self.assertEqual(tuple(map(lambda op: op.symbol, program.operations)),
                         (">", "<", ">=", "<=", "==", "+", "-", "*"))
        for aggregate in ("SUM", "MAX", "MIN"):
            with self.subTest(aggregate=aggregate):
                self.assertEqual(parse(f"DATA|1\nREDUCE|{aggregate}\nPRINT\n").operations[0].symbol, aggregate)

    def test_labels_used_in_trace(self):
        operations = parse(EXAMPLE).operations
        self.assertEqual(tuple(map(lambda op: op.label, operations)), ("FILTER > 5", "MAP * 2", "REDUCE SUM"))

    def test_program_without_reduce(self):
        program = parse("DATA|1,2\nMAP|+|1\nPRINT\n")
        self.assertEqual(program.operations, (Operation(2, "MAP", "+", 1),))

    def test_accepts_crlf_and_missing_final_line_break(self):
        self.assertEqual(parse(EXAMPLE.replace("\n", "\r\n")), parse(EXAMPLE))
        self.assertEqual(parse(EXAMPLE.rstrip("\n")), parse(EXAMPLE))

    def test_big_numbers(self):
        big = "9" * 40
        self.assertEqual(parse(f"DATA|{big}\nMAP|*|{big}\nPRINT\n").data, (int(big),))


class RejectInvalidIrTest(unittest.TestCase):
    def assert_rejected(self, text, line, detail):
        with self.assertRaises(IrFormatError) as context:
            parse(text)
        self.assertEqual(context.exception.line, line)
        self.assertTrue(str(context.exception).startswith(f"programa.ir, linea {line}: "), context.exception)
        self.assertIn(detail, str(context.exception))

    def test_empty_file(self):
        self.assert_rejected("", 1, "el archivo esta vacio")

    def test_unknown_instruction_and_empty_line(self):
        self.assert_rejected("DATA|1\nSORT|ASC\nPRINT\n", 2, "instruccion desconocida 'SORT|ASC'")
        self.assert_rejected("DATA|1\n\nMAP|+|1\nPRINT\n", 2, "linea vacia")
        self.assert_rejected("DATA|1\nmap|+|1\nPRINT\n", 2, "instruccion desconocida")

    def test_invalid_formats(self):
        cases = (
            ("DATA|\nMAP|+|1\nPRINT\n", 1, "DATA|numero,numero,..."),
            ("DATA|1,-2\nMAP|+|1\nPRINT\n", 1, "formato invalido"),
            ("DATA|3 8\nMAP|+|1\nPRINT\n", 1, "formato invalido"),
            ("DATA|1\nFILTER|!=|5\nPRINT\n", 2, "comparador"),
            ("DATA|1\nFILTER|>|x\nPRINT\n", 2, "comparador"),
            ("DATA|1\nMAP|/|2\nPRINT\n", 2, "operador"),
            ("DATA|1\nMAP|+|-2\nPRINT\n", 2, "operador"),
            ("DATA|1\nREDUCE|AVG\nPRINT\n", 2, "REDUCE|SUM, REDUCE|MAX o REDUCE|MIN"),
            ("DATA|1\nMAP|+|1\nPRINT|\n", 3, "formato invalido"),
            ("DATA|1\nMAP|+|1 \nPRINT\n", 2, "formato invalido"),
        )
        for text, line, detail in cases:
            with self.subTest(text=text):
                self.assert_rejected(text, line, detail)

    def test_structure(self):
        self.assert_rejected("MAP|+|1\nPRINT\n", 1, "se esperaba DATA en la primera linea")
        self.assert_rejected("DATA|1\nMAP|+|1\n", 2, "se esperaba PRINT en la ultima linea")
        self.assert_rejected("DATA|1\n", 1, "se esperaba PRINT en la ultima linea")
        self.assert_rejected("DATA|1\nPRINT\n", 2, "al menos una operacion")
        self.assert_rejected("DATA|1\nDATA|2\nMAP|+|1\nPRINT\n", 2, "DATA solo puede ir en la primera linea")
        self.assert_rejected("DATA|1\nMAP|+|1\nPRINT\nPRINT\n", 3, "PRINT solo puede ir en la ultima linea")

    def test_only_print_after_reduce(self):
        self.assert_rejected("DATA|1\nREDUCE|SUM\nMAP|*|2\nPRINT\n", 3, "despues de REDUCE solo puede venir PRINT")
        self.assert_rejected("DATA|1\nREDUCE|SUM\nREDUCE|MAX\nPRINT\n", 3, "despues de REDUCE")


if __name__ == "__main__":
    unittest.main()
