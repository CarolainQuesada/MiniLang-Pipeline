"""Reads programa.ir and validates the contract produced by the Java stage."""

import re
from dataclasses import dataclass
from itertools import count
from typing import NamedTuple

PATTERNS = {
    "DATA": re.compile(r"DATA\|([0-9]+(?:,[0-9]+)*)"),
    "FILTER": re.compile(r"FILTER\|(>=|<=|==|>|<)\|([0-9]+)"),
    "MAP": re.compile(r"MAP\|([-+*])\|([0-9]+)"),
    "REDUCE": re.compile(r"REDUCE\|(SUM|MAX|MIN)"),
    "PRINT": re.compile(r"PRINT"),
}

EXPECTED = {
    "DATA": "DATA|numero,numero,...",
    "FILTER": "FILTER|comparador|numero (comparador: > < >= <= ==)",
    "MAP": "MAP|operador|numero (operador: + - *)",
    "REDUCE": "REDUCE|SUM, REDUCE|MAX o REDUCE|MIN",
    "PRINT": "PRINT",
}


class IrFormatError(Exception):
    """programa.ir does not follow the Java -> Python contract."""

    def __init__(self, line: int, detail: str) -> None:
        super().__init__(f"programa.ir, linea {line}: {detail}")
        self.line = line


@dataclass(frozen=True)
class Operation:
    """FILTER, MAP or REDUCE with its line in programa.ir."""

    line: int
    kind: str
    symbol: str
    operand: int | None = None

    @property
    def label(self) -> str:
        """Text used in the trace, e.g. 'FILTER > 5' or 'REDUCE SUM'."""
        if self.operand is None:
            return f"{self.kind} {self.symbol}"
        return f"{self.kind} {self.symbol} {self.operand}"


@dataclass(frozen=True)
class Program:
    data: tuple[int, ...]
    operations: tuple[Operation, ...]


class _Line(NamedTuple):
    number: int
    kind: str
    fields: tuple[str, ...]


def parse(text: str) -> Program:
    """Validates every line and the order DATA, operations, PRINT; raises IrFormatError."""
    lines = tuple(map(_parse_line, count(1), text.splitlines()))
    if not lines:
        raise IrFormatError(1, "el archivo esta vacio; se esperaba DATA")
    _check_structure(lines)
    return Program(
        data=tuple(map(int, lines[0].fields[0].split(","))),
        operations=tuple(map(_to_operation, lines[1:-1])),
    )


def _parse_line(number: int, text: str) -> _Line:
    kind = text.split("|", 1)[0]
    pattern = PATTERNS.get(kind)
    if pattern is None:
        raise IrFormatError(number, f"instruccion desconocida '{text}'" if text else "linea vacia")
    match = pattern.fullmatch(text)
    if match is None:
        raise IrFormatError(number, f"formato invalido '{text}'; se esperaba {EXPECTED[kind]}")
    return _Line(number, kind, match.groups())


def _check_structure(lines: tuple[_Line, ...]) -> None:
    first, last, middle = lines[0], lines[-1], lines[1:-1]
    if first.kind != "DATA":
        raise IrFormatError(first.number, f"se esperaba DATA en la primera linea; se encontro {first.kind}")
    if last.kind != "PRINT":
        raise IrFormatError(last.number, f"se esperaba PRINT en la ultima linea; se encontro {last.kind}")
    if not middle:
        raise IrFormatError(last.number, "se esperaba al menos una operacion FILTER, MAP o REDUCE antes de PRINT")
    misplaced = next(filter(lambda line: line.kind in ("DATA", "PRINT"), middle), None)
    if misplaced is not None:
        place = "la primera linea" if misplaced.kind == "DATA" else "la ultima linea"
        raise IrFormatError(misplaced.number, f"{misplaced.kind} solo puede ir en {place}")
    after_reduce = next(filter(lambda pair: pair[0].kind == "REDUCE", zip(middle, middle[1:])), None)
    if after_reduce is not None:
        raise IrFormatError(after_reduce[1].number, "despues de REDUCE solo puede venir PRINT")


def _to_operation(line: _Line) -> Operation:
    if line.kind == "REDUCE":
        return Operation(line.number, line.kind, line.fields[0])
    return Operation(line.number, line.kind, line.fields[0], int(line.fields[1]))
