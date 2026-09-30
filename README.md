# MiniLang Pipeline

Practical assignment for Part B of the EIF400 Programming Paradigms midterm
exam (National University, Brunca Regional Campus – Coto Campus,
Semester II, 2026).

The project reads and validates a small data-transformation language, translates
it into an intermediate representation, executes its operations using a
functional style, and generates a verification signature. Each stage uses a
different language and paradigm, and communicates with the next stage only
through files.

## Team Members

- Ashly Delgado
- Carolain Quesada

## Pipeline

```text
programa.mini ──► Java ──► programa.ir ──► Python ──► resultado.txt ──► MIPS ──► firma.txt
```

| Stage | Language and paradigm | Input | Output | Status |
|---|---|---|---|---|
| 1. Analysis and translation | Java, object-oriented | `programa.mini` | `programa.ir` | Complete |
| 2. Operation execution | Python, functional | `programa.ir` | `resultado.txt` | Complete |
| 3. Verification signature | MIPS assembly | `resultado.txt` | `firma.txt` | Complete |

## Pipeline Diagram and File Contracts

```text
             │ programa.mini   MiniLang program
             │                   DATA 3 8 5 10 12
             │                   FILTER > 5
             │                   MAP * 2
             │                   REDUCE SUM
             ▼                   PRINT
┌─────────────────────────┐
│ 1. JAVA                 │
│ Lexer + parser + OOP    │
└────────────┬────────────┘
             │ programa.ir     One instruction per line, fields separated by |
             │                   DATA|3,8,5,10,12
             │                   FILTER|>|5
             │                   MAP|*|2
             │                   REDUCE|SUM
             ▼                   PRINT
┌─────────────────────────┐
│ 2. PYTHON               │
│ Functional style        │
│ FILTER / MAP / REDUCE   │
└────────────┬────────────┘
             │ resultado.txt   One trace line per operation, then RESULT=
             │                   FILTER > 5 => [8, 10, 12]
             │                   MAP * 2 => [16, 20, 24]
             │                   REDUCE SUM => 60
             ▼                   RESULT=60
┌─────────────────────────┐
│ 3. MIPS                 │
│ Checksum / verification │
└────────────┬────────────┘
             │ firma.txt       Number of operations and checksum
             ▼                   OPERATIONS=3
                                 CHECKSUM=80
```

Each contract is described in detail in the section of its stage below.

The same diagram as an image: [`docs/pipeline-diagram.png`](docs/pipeline-diagram.png).

## Requirements

- JDK 17 or later, with `java` and `javac` available in the terminal.
- Python 3.11 or later, with `python` available in the terminal. If an older
  version is used, the Python stage displays a message instead of failing.
- MARS 4.5 simulator (`Mars45.jar`), run with the same `java` executable. The
  commands in this README assume it is in the user's Downloads folder.

No external libraries are used.

## Project Structure

```text
programa.mini   Sample program from the assignment.
programa.ir     IR generated from programa.mini by the Java stage.
java/src/       Java stage source code.
tests/java/     Java stage tests.
python/         Python stage source code.
tests/python/   Python stage tests.
mips/           MIPS stage source code.
tests/mips/     MIPS stage tests.
```

## MiniLang Language

Example (`programa.mini`):

```text
DATA 3 8 5 10 12
FILTER > 5
MAP * 2
REDUCE SUM
PRINT
```

The assignment's grammar is used without changes:

```text
<program>    ::= <data> <operation> { <operation> } "PRINT"
<data>       ::= "DATA" <number> { <number> }
<operation>  ::= <filter> | <map> | <reduce>
<filter>     ::= "FILTER" <comparator> <number>
<map>        ::= "MAP" <arithmetic> <number>
<reduce>     ::= "REDUCE" ("SUM" | "MAX" | "MIN")
<comparator> ::= ">" | "<" | ">=" | "<=" | "=="
<arithmetic> ::= "+" | "-" | "*"
<number>     ::= non-negative integer
```

- Reserved words must be uppercase.
- Spaces, tabs, and line breaks separate tokens, but are not required between an
  operator and a number (`MAP*2` is valid).
- **Semantic rule:** REDUCE converts the list into a single number, so only PRINT
  may follow REDUCE. REDUCE is optional; without it, the result is the
  transformed list.

## Stage 1: Java

### Running the Java Stage

From the repository root in PowerShell:

```powershell
$sources = Get-ChildItem java/src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d java/build $sources
java -cp java/build minilang.Main
```

The program takes no arguments: it reads `programa.mini` and writes
`programa.ir` to the current directory. `programa.mini` must be encoded as
UTF-8, with or without a BOM.

| Exit code | Meaning |
|---|---|
| 0 | Program is valid; `programa.ir` was generated. |
| 1 | Lexical, syntax, semantic, or file I/O error. |
| 2 | Arguments were provided. |

On error, a message is written to standard error and `programa.ir` is not created
or modified. If a file from a previous run exists, it remains unchanged;
therefore, later stages must check the exit code before using it.

### Detected Errors

| Type | Input | Message |
|---|---|---|
| Lexical | `FILTER ! 5` | `Linea 2: Caracter no reconocido: !` |
| Syntax | `FILTER + 5` | `Linea 2: Se esperaba un comparador >, <, >=, <= o ==; se encontro '+'` |
| Syntax | Program without DATA | `Linea 1: Se esperaba DATA al inicio del programa; se encontro 'FILTER'` |
| Semantic | `REDUCE SUM` followed by `MAP * 2` | `Linea 3: MAP necesita una lista, pero REDUCE de la linea 2 ya la convirtio en un solo numero; despues de REDUCE solo puede venir PRINT` |

Missing `programa.mini` files and files that are not UTF-8 (for example, UTF-16
files created with `>` in Windows PowerShell 5.1) are also reported clearly.
Invisible or non-ASCII characters, which can appear when copying and pasting,
are displayed with their Unicode code point (for example, `U+00A0`). Unknown
words are shown in full, including accented characters (`FILTÉR`).

### Design

```text
MiniLangCompiler:  Lexer ──► Parser ──► SemanticAnalyzer ──► IrGenerator
                  (tokens)  (List<Instruction>)   (rules)       (programa.ir)
```

| Class | Package | Responsibility |
|---|---|---|
| `Main` | `minilang` | Entry point: displays messages and sets the exit code. |
| `MiniLangCompiler` | `minilang` | Connects reading, analysis, and writing; writes output only when the program is valid. |
| `Lexer` | `minilang.lexer` | Converts source text into tokens and tracks their line numbers. |
| `Parser` | `minilang.parser` | Recursive-descent parser with one method per grammar rule. |
| `SemanticAnalyzer` | `minilang.semantic` | Rejects operations after REDUCE. |
| `Instruction` and subclasses | `minilang.model` | Represent the program as objects. |
| `IrGenerator` | `minilang.ir` | Combines the lines produced by each instruction. |

Instruction hierarchy:

```text
Instruction (abstract)
├── DataInstr
├── FilterInstr
├── MapInstr
├── ReduceInstr
└── PrintInstr
```

- **Inheritance:** `Instruction` stores the source line and number validation,
  which all subclasses reuse.
- **Overriding:** each subclass implements the abstract methods `getName()` and
  `toIR()`. FILTER, MAP, and REDUCE override `requiresList()`, and REDUCE also
  overrides `producesNumber()`.
- **Polymorphism:** `IrGenerator` and `SemanticAnalyzer` iterate over a
  `List<Instruction>` without checking concrete types; Java selects the correct
  implementation at runtime. Adding an instruction only requires a new subclass.

The objects are immutable, operators are enums restricted to the grammar, and
numbers use `BigInteger` because the grammar does not define an integer limit.

### Generating Javadoc

From the repository root, generate HTML documentation for all Java packages with:

```powershell
javadoc -encoding UTF-8 -d java/doc -sourcepath java/src minilang minilang.ir minilang.lexer minilang.model minilang.parser minilang.semantic
```

Open `java/doc/index.html` to browse the documentation. The class index includes
all 17 top-level Java types and their three nested operator and comparison enums.

## `programa.ir` Contract (Java → Python)

One instruction per line, fields separated by `|`, and DATA numbers separated
by commas with no spaces:

```text
DATA|3,8,5,10,12
FILTER|>|5
MAP|*|2
REDUCE|SUM
PRINT
```

| Instruction | Format | Values |
|---|---|---|
| DATA | `DATA\|n1,n2,...` | Non-negative integers |
| FILTER | `FILTER\|comparator\|n` | `>` `<` `>=` `<=` `==` |
| MAP | `MAP\|operator\|n` | `+` `-` `*` |
| REDUCE | `REDUCE\|type` | `SUM` `MAX` `MIN` |
| PRINT | `PRINT` | — |

The Java stage guarantees that:

- The file is UTF-8 without a BOM, uses LF line endings, and ends with a newline.
- The first line is DATA and the last is PRINT; program order is preserved.
- There is at most one REDUCE, and if present, it is immediately before PRINT.
- Integers are written in decimal, without a sign or leading zeroes.

`.gitattributes` makes Git preserve LF line endings in `programa.ir`,
`resultado.txt`, and `firma.txt` on every computer, even when
`core.autocrlf=true`.

## Java Tests

From the repository root in PowerShell:

```powershell
$sources = Get-ChildItem java/src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$tests = Get-ChildItem tests/java -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -Xlint:all -d java/build $sources $tests
java -cp java/build LexerTest
java -cp java/build ParserTest
java -cp java/build SemanticAnalyzerTest
java -cp java/build IrGeneratorTest
java -cp java/build JavaStageTest
```

| Test | What it verifies |
|---|---|
| `LexerTest` | Tokens, operators, line endings (LF, CRLF, and CR), numbers, and lexical errors. |
| `ParserTest` | Object construction, grammar variants, and syntax errors with line numbers. |
| `SemanticAnalyzerTest` | Valid programs and rejection of operations after REDUCE. |
| `IrGeneratorTest` | Exact IR format and prevention of IR generation for invalid programs. |
| `JavaStageTest` | Runs the application in temporary directories: exit codes, messages, BOM, UTF-16, missing input, and preservation of previous IR. |

Each test prints `PASS: ...` when successful. If a test fails, it exits with an
`AssertionError` that identifies the case. JUnit is not used, avoiding external
library dependencies.

## Stage 2: Python

### Running the Python Stage

From the repository root, after running the Java stage:

```powershell
python python/executor.py
```

The program takes no arguments: it reads `programa.ir` and writes
`resultado.txt` to the current directory. It uses the same exit codes as Java:
0 if `resultado.txt` was generated, 1 on any error, and 2 if arguments are
provided. On error, a message is written to standard error and `resultado.txt`
is not created or modified.

### Functional Style

| Operation | Functional construct |
|---|---|
| FILTER | `filter()` with the comparator as a function (`operator.gt`, `operator.lt`, ...) |
| MAP | `map()` with the operator as a function (`operator.add`, `operator.sub`, `operator.mul`) |
| REDUCE | `functools.reduce()` with `operator.add`, `max`, or `min` |

- **Functions as values:** comparators and operators are stored in dictionaries
  (`">"` → `operator.gt`), avoiding chains of `if/elif` statements.
- **No loops:** the stage uses no `for`, `while`, or comprehensions. The entire
  program is also executed with `reduce()`, where each operation receives the
  previous result. A test verifies this by analyzing the source code.
- **Immutability:** lists are tuples and data objects use `dataclass(frozen=True)`.
- **Pure core:** `operations.py` only transforms data; file I/O is confined to
  `executor.py`.

| File | Responsibility |
|---|---|
| `ir_parser.py` | Reads `programa.ir` and validates the contract (line formats and DATA, operation, PRINT order). |
| `operations.py` | Implements FILTER, MAP, and REDUCE as pure functions and executes the program. |
| `executor.py` | Entry point: connects the components, writes `resultado.txt`, and sets the exit code. |

### Empty Lists

If FILTER leaves the list empty, subsequent operations are applied to `[]`:

- `REDUCE SUM` returns `0`, because 0 is the identity element for addition.
- `REDUCE MAX` and `REDUCE MIN` have no result because an empty list has no
  maximum or minimum. The stage stops with an error, and the pipeline does not
  continue:

```text
programa.ir, linea 3: REDUCE MAX no se puede aplicar a una lista vacia; FILTER no dejo ningun elemento
```

### `resultado.txt` Contract (Python → MIPS)

One trace line per operation in the form `OPERATION => value`, followed by the
`RESULT=` line, as in the assignment example:

```text
FILTER > 5 => [8, 10, 12]
MAP * 2 => [16, 20, 24]
REDUCE SUM => 60
RESULT=60
```

- The number of executed operations is the number of lines before `RESULT=`.
- Without REDUCE, the result is a list: `RESULT=[16, 20, 24]`. If FILTER empties
  it, the result is `RESULT=[]`.
- Values can be negative after `MAP -` (for example, `[-7, -2]`).
- The file is UTF-8 without a BOM, uses LF line endings, and ends with a newline.

### Python Tests

From the repository root:

```powershell
python -m unittest discover -s tests/python -v
```

| Test | What it verifies |
|---|---|
| `test_ir_parser.py` | `programa.ir` contract: valid formats, CRLF, large numbers, invalid lines, and incorrect ordering. |
| `test_operations.py` | Every comparator, operator, and aggregate; empty lists; sample trace; and absence of loops. |
| `test_executor.py` | Runs the stage in temporary directories: exact `resultado.txt`, exit codes, errors, and preservation of previous results. |

The tests use Python's built-in `unittest` module to avoid external dependencies.

## Stage 3: MIPS

### Running the MIPS Stage

From the repository root, after running the Python stage:

```powershell
java -jar "$env:USERPROFILE\Downloads\Mars45.jar" nc sm ae1 se1 mips/signature.asm
```

If the MARS file has a different name or is in another folder (the official
download is named `Mars4_5.jar`), update the path in the command.

MARS options: `nc` suppresses the copyright notice, `sm` starts at `main`, and
`ae1`/`se1` exit with code 1 if an assembly or runtime error occurs.

To step through execution in the MARS interface (registers and memory), launch
MARS from the project directory with
`java -jar "$env:USERPROFILE\Downloads\Mars45.jar"`, then open
`mips/signature.asm`. If MARS is opened by double-clicking the JAR, it looks for
`resultado.txt` in the JAR's directory and will not find it.
The program reads `resultado.txt` and writes `firma.txt` to the current
directory. It exits with code 0 when the signature is generated and code 1 on
any error. In that case, the message is written to standard error and
`firma.txt` is not created or modified.

### Verification Signature

The formula from the assignment is generalized to support list results:

```text
operations = trace lines before RESULT=
acc         = 0
acc         = acc * 31 + value        for each RESULT= value, in order
checksum    = acc XOR operations
checksum    = checksum + 17
```

For the assignment example (`RESULT=60`, 3 operations), `acc = 60`,
`60 XOR 3 = 63`, and `63 + 17 = 80`. For a list result, multiplying by 31
before adding makes the order significant: `[1, 2]` and `[2, 1]` have different
signatures. For an empty list, `acc = 0`.

Arithmetic is 32-bit, modulo 2³², as in common checksums. `mul`, `addu`, and
`addiu` do not stop on overflow, so a valid result is never rejected for being
large. The signature is written as an unsigned integer.

`firma.txt` (UTF-8, LF line endings):

```text
OPERATIONS=3
CHECKSUM=80
```

### Section 6 Requirements

| Requirement | Where it is met |
|---|---|
| Registers | `$s0`–`$s7` store traversal state; `$t0`–`$t9` store temporary values. |
| Memory access | `resultado.txt` is loaded into `text` and traversed with `lbu`; `firma.txt` is built with `sb`; `$ra` is saved on the stack with `sw`/`lw`. |
| Loop or traversal | `scan_lines` traverses lines, `value_loop` traverses `RESULT=` values, and `digit_loop` traverses digits. |
| Arithmetic operation | `mul` and `addu` calculate `acc * 31 + value`; `addiu` adds 17. |
| Logical operation | `xor` combines the operation count; `or` validates the `-` sign. |
| Conditional branch | `beq`, `bne`, `beqz`, `bnez`, `bltz`, and `bgeu` classify characters and detect errors. |

MIPS processes data generated by the previous stage: it reads `resultado.txt`,
not hard-coded program constants.

### Detected Errors

| Condition | Message |
|---|---|
| Missing `resultado.txt` | `No se encontro resultado.txt. Ejecute primero la etapa Python...` |
| No `RESULT=` line | `resultado.txt no tiene la linea RESULT=.` |
| Invalid character | `resultado.txt, linea 2: caracter invalido en RESULT=` |
| Misplaced sign | `resultado.txt, linea 2: numero invalido en RESULT=` |
| Empty `RESULT=` | `resultado.txt, linea 2: RESULT= no tiene un valor` |
| Text after `RESULT=` | `resultado.txt, linea 2: RESULT= debe ser la ultima linea` |
| File larger than 64 KB | `resultado.txt es demasiado grande (maximo 65536 bytes).` |

### MIPS Tests

From the repository root, with MARS in Downloads (or at the path specified by
the `MARS_JAR` environment variable):

```powershell
python -m unittest discover -s tests/mips -v
```

`test_signature.py` runs `signature.asm` in MARS using temporary directories
and compares `firma.txt` with a reference implementation of the formula. It
covers the assignment example (80), numbers, lists, empty lists, negative
values, numbers larger than 32 bits, CRLF, errors, and preservation of the
previous signature.

## Final delivery checklist

Before submitting the project, verify the following in order:

```powershell
# 1) Java stage and tests
$sources = Get-ChildItem java/src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$tests = Get-ChildItem tests/java -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -Xlint:all -d java/build $sources $tests
java -cp java/build LexerTest
java -cp java/build ParserTest
java -cp java/build SemanticAnalyzerTest
java -cp java/build IrGeneratorTest
java -cp java/build JavaStageTest

# 2) Python stage and tests
python -m unittest discover -s tests/python -v

# 3) Full end-to-end pipeline
java -cp java/build minilang.Main
python python/executor.py
java -jar "$env:USERPROFILE\Downloads\Mars45.jar" nc sm ae1 se1 mips/signature.asm

# 4) MIPS verification tests
python -m unittest discover -s tests/mips -v
```

Expected generated artifacts:

- `programa.ir`
- `resultado.txt`
- `firma.txt`

The project is ready when all three test suites pass and the pipeline generates
all three output files without errors.
