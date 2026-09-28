"""Runs mips/signature.asm in MARS on temporary folders and checks firma.txt."""

import os
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROGRAM = ROOT / "mips" / "signature.asm"
MARS_JAR = Path(os.environ.get("MARS_JAR", Path.home() / "Downloads" / "Mars45.jar"))
EXAMPLE = "FILTER > 5 => [8, 10, 12]\nMAP * 2 => [16, 20, 24]\nREDUCE SUM => 60\nRESULT=60\n"


def expected_signature(result_text):
    """Reference implementation of the formula, modulo 2^32."""
    lines = list(filter(None, result_text.replace("\r", "").split("\n")))
    operations = len(lines) - 1
    acc = 0
    for value in map(int, re.findall(r"-?\d+", lines[-1][len("RESULT="):])):
        acc = (acc * 31 + value) % 2**32
    checksum = ((acc ^ operations) + 17) % 2**32
    return f"OPERATIONS={operations}\nCHECKSUM={checksum}\n"


@unittest.skipUnless(MARS_JAR.is_file(), f"MARS not found at {MARS_JAR}; set MARS_JAR")
class SignatureTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="minilang-mips-test-")
        self.addCleanup(temporary.cleanup)
        self.folder = Path(temporary.name)
        self.input = self.folder / "resultado.txt"
        self.output = self.folder / "firma.txt"

    def run_stage(self, result_bytes=None):
        if result_bytes is not None:
            self.input.write_bytes(result_bytes)
        return subprocess.run(["java", "-jar", str(MARS_JAR), "nc", "sm", "ae1", "se1", str(PROGRAM)],
                              cwd=self.folder, capture_output=True, encoding="utf-8", errors="replace",
                              timeout=60)

    def assert_signature(self, result_text):
        process = self.run_stage(result_text.encode("utf-8"))
        self.assertEqual(process.returncode, 0, process.stderr)
        self.assertEqual(process.stdout.strip(), "Firma calculada. Se genero firma.txt.")
        self.assertEqual(process.stderr, "")
        self.assertEqual(self.output.read_bytes(), expected_signature(result_text).encode("utf-8"))

    def assert_failed(self, result_bytes, message):
        process = self.run_stage(result_bytes)
        self.assertEqual(process.returncode, 1)
        self.assertIn(message, process.stderr)
        self.assertEqual(process.stdout.strip(), "")
        self.assertFalse(self.output.exists())

    def test_example_from_assignment_gives_80(self):
        self.input.write_text(EXAMPLE, encoding="utf-8")
        self.assertEqual(self.run_stage().returncode, 0)
        self.assertEqual(self.output.read_bytes(), b"OPERATIONS=3\nCHECKSUM=80\n")

    def test_single_values(self):
        self.assert_signature("REDUCE MAX => 24\nRESULT=24\n")
        self.assert_signature("FILTER > 100 => []\nREDUCE SUM => 0\nRESULT=0\n")
        self.assert_signature("MAP - 20 => [-17]\nREDUCE MIN => -17\nRESULT=-17\n")

    def test_list_results_depend_on_order(self):
        self.assert_signature("MAP * 2 => [16, 20, 24]\nRESULT=[16, 20, 24]\n")
        self.assert_signature("MAP - 10 => [-7, -2]\nRESULT=[-7, -2]\n")
        self.assert_signature("FILTER > 100 => []\nRESULT=[]\n")
        first = expected_signature("MAP + 0 => [1, 2]\nRESULT=[1, 2]\n")
        self.assertNotEqual(first, expected_signature("MAP + 0 => [2, 1]\nRESULT=[2, 1]\n"))

    def test_numbers_beyond_32_bits_wrap_around(self):
        big = "9" * 30
        self.assert_signature(f"MAP * {big} => [{big}]\nREDUCE SUM => {big}\nRESULT={big}\n")

    def test_crlf_and_missing_final_line_break(self):
        self.assert_signature(EXAMPLE.replace("\n", "\r\n"))
        self.assert_signature(EXAMPLE.rstrip("\n"))

    def test_missing_input(self):
        self.assert_failed(None, "No se encontro resultado.txt")

    def test_invalid_inputs(self):
        cases = (
            (b"", "no tiene la linea RESULT="),
            (b"MAP * 2 => [2]\n", "no tiene la linea RESULT="),
            (b"MAP * 2 => [2]\nRESULT=6x\n", "resultado.txt, linea 2: caracter invalido en RESULT="),
            (b"MAP * 2 => [2]\nRESULT=--6\n", "resultado.txt, linea 2: numero invalido en RESULT="),
            (b"MAP * 2 => [2]\nRESULT=[-]\n", "resultado.txt, linea 2: numero invalido en RESULT="),
            (b"MAP * 2 => [2]\nRESULT=\n", "resultado.txt, linea 2: RESULT= no tiene un valor"),
            (b"RESULT=2\nMAP * 2 => [4]\n", "resultado.txt, linea 2: RESULT= debe ser la ultima linea"),
            (b"RESULT=" + b"1" * 70000 + b"\n", "demasiado grande"),
        )
        for content, message in cases:
            with self.subTest(message=message):
                self.assert_failed(content, message)

    def test_error_keeps_previous_signature(self):
        self.output.write_text("previous", encoding="utf-8")
        process = self.run_stage(b"RESULT=abc\n")
        self.assertEqual(process.returncode, 1)
        self.assertEqual(self.output.read_text(encoding="utf-8"), "previous")


if __name__ == "__main__":
    unittest.main()
