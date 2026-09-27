"""Runs the real Python stage in isolated temporary folders."""

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

EXECUTOR = Path(__file__).resolve().parents[2] / "python" / "executor.py"
IR = "DATA|3,8,5,10,12\nFILTER|>|5\nMAP|*|2\nREDUCE|SUM\nPRINT\n"
RESULT = "FILTER > 5 => [8, 10, 12]\nMAP * 2 => [16, 20, 24]\nREDUCE SUM => 60\nRESULT=60\n"


class ExecutorTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="minilang-python-test-")
        self.addCleanup(temporary.cleanup)
        self.folder = Path(temporary.name)
        self.input = self.folder / "programa.ir"
        self.output = self.folder / "resultado.txt"

    def run_stage(self, *args):
        # errors="replace" keeps a path like C:\Users\José from breaking the test.
        return subprocess.run([sys.executable, str(EXECUTOR), *args], cwd=self.folder,
                              capture_output=True, encoding="utf-8", errors="replace", timeout=30)

    def assert_failed(self, process, message):
        self.assertEqual(process.returncode, 1)
        self.assertIn(message, process.stderr)
        self.assertEqual(process.stdout, "")

    def test_valid_program_writes_exact_result(self):
        self.input.write_bytes(IR.encode("utf-8"))
        process = self.run_stage()
        self.assertEqual(process.returncode, 0, process.stderr)
        self.assertEqual(process.stdout.strip(), "Ejecucion valida. Se genero resultado.txt.")
        self.assertEqual(process.stderr, "")
        self.assertEqual(self.output.read_bytes(), RESULT.encode("utf-8"), "UTF-8 without BOM and with LF")
        self.assertEqual(self.input.read_bytes(), IR.encode("utf-8"), "programa.ir untouched")

    def test_program_without_reduce(self):
        self.input.write_bytes(b"DATA|3,8\nMAP|-|10\nPRINT\n")
        self.assertEqual(self.run_stage().returncode, 0)
        self.assertEqual(self.output.read_text(encoding="utf-8"), "MAP - 10 => [-7, -2]\nRESULT=[-7, -2]\n")

    def test_filter_empties_the_list(self):
        self.input.write_bytes(b"DATA|3,8\nFILTER|>|100\nREDUCE|SUM\nPRINT\n")
        self.assertEqual(self.run_stage().returncode, 0)
        self.assertEqual(self.output.read_text(encoding="utf-8"), "FILTER > 100 => []\nREDUCE SUM => 0\nRESULT=0\n")

    def test_max_of_empty_list_stops_the_pipeline(self):
        self.input.write_bytes(b"DATA|3,8\nFILTER|>|100\nREDUCE|MAX\nPRINT\n")
        self.assert_failed(self.run_stage(), "programa.ir, linea 3: REDUCE MAX no se puede aplicar a una lista vacia")
        self.assertFalse(self.output.exists())

    def test_missing_input(self):
        self.assert_failed(self.run_stage(), "No se encontro programa.ir")
        self.assertFalse(self.output.exists())

    def test_invalid_ir_keeps_previous_result(self):
        self.output.write_text("previous", encoding="utf-8")
        self.input.write_bytes(b"DATA|1\nMAP|/|2\nPRINT\n")
        self.assert_failed(self.run_stage(), "programa.ir, linea 2: formato invalido")
        self.assertEqual(self.output.read_text(encoding="utf-8"), "previous")

    def test_utf8_bom_is_accepted_and_utf16_is_reported(self):
        self.input.write_bytes(b"\xef\xbb\xbf" + IR.encode("utf-8"))
        self.assertEqual(self.run_stage().returncode, 0)
        self.input.write_bytes(("\ufeff" + IR).encode("utf-16-le"))
        self.assert_failed(self.run_stage(), "programa.ir no esta guardado en UTF-8")

    def test_arguments_are_rejected(self):
        self.input.write_bytes(IR.encode("utf-8"))
        process = self.run_stage("unexpected")
        self.assertEqual(process.returncode, 2)
        self.assertIn("Uso:", process.stderr)
        self.assertFalse(self.output.exists())

    def test_old_python_gets_a_clear_message(self):
        # Simulates Python 3.10 by replacing sys.version_info before the stage starts.
        code = ("import runpy, sys; sys.version_info = (3, 10, 0); "
                f"runpy.run_path({str(EXECUTOR)!r}, run_name='__main__')")
        process = subprocess.run([sys.executable, "-c", code], cwd=self.folder, capture_output=True,
                                 encoding="utf-8", errors="replace", timeout=30)
        self.assert_failed(process, "Se requiere Python 3.11 o superior")
        self.assertNotIn("Traceback", process.stderr)

    def test_write_error(self):
        self.input.write_bytes(IR.encode("utf-8"))
        self.output.mkdir()
        self.assert_failed(self.run_stage(), "Error de lectura/escritura")
        self.assertTrue(self.output.is_dir())


if __name__ == "__main__":
    unittest.main()
