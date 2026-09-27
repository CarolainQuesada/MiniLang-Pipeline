"""FILTER, MAP and REDUCE as pure functions: no loops, no I/O and no mutation."""

import operator
from dataclasses import dataclass
from functools import reduce

from ir_parser import Operation

Value = tuple[int, ...] | int

# Operators are functions stored as values, so no if/elif chain is needed.
COMPARATORS = {
    ">": operator.gt,
    "<": operator.lt,
    ">=": operator.ge,
    "<=": operator.le,
    "==": operator.eq,
}
ARITHMETIC = {"+": operator.add, "-": operator.sub, "*": operator.mul}
AGGREGATES = {"SUM": operator.add, "MAX": max, "MIN": min}


class ExecutionError(Exception):
    """An operation cannot produce a result, e.g. REDUCE MAX on an empty list."""

    def __init__(self, line: int, detail: str) -> None:
        super().__init__(f"programa.ir, linea {line}: {detail}")
        self.line = line


@dataclass(frozen=True)
class Step:
    """One line of the trace: the operation and the value it produced."""

    label: str
    value: Value


def apply_filter(values: tuple[int, ...], operation: Operation) -> tuple[int, ...]:
    compare = COMPARATORS[operation.symbol]
    return tuple(filter(lambda value: compare(value, operation.operand), values))


def apply_map(values: tuple[int, ...], operation: Operation) -> tuple[int, ...]:
    combine = ARITHMETIC[operation.symbol]
    return tuple(map(lambda value: combine(value, operation.operand), values))


def apply_reduce(values: tuple[int, ...], operation: Operation) -> int:
    combine = AGGREGATES[operation.symbol]
    if operation.symbol == "SUM":
        # 0 is the neutral element of addition: the sum of an empty list is 0.
        return reduce(combine, values, 0)
    if not values:
        raise ExecutionError(
            operation.line,
            f"REDUCE {operation.symbol} no se puede aplicar a una lista vacia; FILTER no dejo ningun elemento",
        )
    return reduce(combine, values)


HANDLERS = {"FILTER": apply_filter, "MAP": apply_map, "REDUCE": apply_reduce}


def execute(data: tuple[int, ...], operations: tuple[Operation, ...]) -> tuple[Value, tuple[Step, ...]]:
    """Applies each operation to the previous result (a left fold) and records the trace."""

    def apply(state: tuple[Value, tuple[Step, ...]], operation: Operation) -> tuple[Value, tuple[Step, ...]]:
        value, steps = state
        result = HANDLERS[operation.kind](value, operation)
        return result, steps + (Step(operation.label, result),)

    return reduce(apply, operations, (data, ()))
