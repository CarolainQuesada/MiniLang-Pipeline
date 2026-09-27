"""Python stage: reads programa.ir, runs it in functional style and writes resultado.txt."""

import sys

# Checked before the other imports: they use syntax that fails on older versions.
if sys.version_info < (3, 11):
    sys.exit(f"Se requiere Python 3.11 o superior; version actual: {sys.version.split()[0]}")

from pathlib import Path  # noqa: E402

from ir_parser import IrFormatError, parse  # noqa: E402
from operations import ExecutionError, Step, Value, execute  # noqa: E402

INPUT = Path("programa.ir")
OUTPUT = Path("resultado.txt")


def format_value(value: Value) -> str:
    """Lists as [8, 10, 12] and numbers as 60, like the example in the assignment."""
    if isinstance(value, tuple):
        return "[" + ", ".join(map(str, value)) + "]"
    return str(value)


def render(result: Value, steps: tuple[Step, ...]) -> str:
    trace = map(lambda step: f"{step.label} => {format_value(step.value)}", steps)
    return "\n".join((*trace, f"RESULT={format_value(result)}")) + "\n"


def main(args: list[str]) -> int:
    if args:
        print("Uso: python python/executor.py (sin argumentos)", file=sys.stderr)
        return 2
    try:
        program = parse(INPUT.read_text(encoding="utf-8-sig"))
        result, steps = execute(program.data, program.operations)
        # The output is written only after the whole program ran successfully.
        OUTPUT.write_text(render(result, steps), encoding="utf-8", newline="\n")
    except (IrFormatError, ExecutionError) as error:
        print(error, file=sys.stderr)
        return 1
    except FileNotFoundError as error:
        print(f"No se encontro {error.filename} en {Path.cwd()}. "
              "Ejecute primero la etapa Java desde la carpeta del proyecto.", file=sys.stderr)
        return 1
    except UnicodeDecodeError:
        print(f"{INPUT} no esta guardado en UTF-8.", file=sys.stderr)
        return 1
    except OSError as error:
        print(f"Error de lectura/escritura: {error}", file=sys.stderr)
        return 1
    print("Ejecucion valida. Se genero resultado.txt.")
    return 0


if __name__ == "__main__":
    # Python limits int/str conversion to 4300 digits; the grammar and the Java stage have no limit.
    sys.set_int_max_str_digits(0)
    sys.exit(main(sys.argv[1:]))
