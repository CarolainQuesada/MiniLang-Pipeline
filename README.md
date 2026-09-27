# MiniLang Pipeline

Reto práctico de la Parte B del examen parcial de EIF400 Paradigmas de
Programación (Universidad Nacional, Sede Regional Brunca – Campus Coto,
II Ciclo 2026).

El proyecto lee un pequeño lenguaje de transformación de datos, lo valida, lo
traduce a una representación intermedia, ejecuta las operaciones con estilo
funcional y genera una firma de verificación. Cada etapa usa un lenguaje y un
paradigma distinto, y se comunica con la siguiente únicamente mediante archivos.

## Integrantes

- Ashly Delgado
- Carolain Quesada

## Pipeline

```text
programa.mini ──► Java ──► programa.ir ──► Python ──► resultado.txt ──► MIPS ──► firma.txt
```

| Etapa | Lenguaje y paradigma | Entrada | Salida | Estado |
|---|---|---|---|---|
| 1. Análisis y traducción | Java, orientado a objetos | `programa.mini` | `programa.ir` | Completa |
| 2. Ejecución de operaciones | Python, funcional | `programa.ir` | `resultado.txt` | En desarrollo |
| 3. Firma de verificación | MIPS, ensamblador | `resultado.txt` | `firma.txt` | En desarrollo |

## Requisitos

- JDK 17 o superior, con `java` y `javac` disponibles en la terminal.
- Los requisitos de Python y MIPS se agregarán al completar esas etapas.

No se usan librerías externas.

## Estructura

```text
programa.mini   Programa de ejemplo del enunciado.
programa.ir     IR generado por la etapa Java a partir de programa.mini.
java/src/       Código fuente de la etapa Java.
tests/java/     Pruebas de la etapa Java.
python/         Etapa Python (en desarrollo).
mips/           Etapa MIPS (en desarrollo).
```

## Lenguaje MiniLang

Ejemplo (`programa.mini`):

```text
DATA 3 8 5 10 12
FILTER > 5
MAP * 2
REDUCE SUM
PRINT
```

Se usa la gramática del enunciado, sin cambios:

```text
<programa>   ::= <data> <operacion> { <operacion> } "PRINT"
<data>       ::= "DATA" <numero> { <numero> }
<operacion>  ::= <filter> | <map> | <reduce>
<filter>     ::= "FILTER" <comparador> <numero>
<map>        ::= "MAP" <aritmetico> <numero>
<reduce>     ::= "REDUCE" ("SUM" | "MAX" | "MIN")
<comparador> ::= ">" | "<" | ">=" | "<=" | "=="
<aritmetico> ::= "+" | "-" | "*"
<numero>     ::= entero no negativo
```

- Las palabras reservadas se escriben en mayúsculas.
- Los espacios, tabulaciones y saltos de línea separan elementos, pero no son
  obligatorios entre un operador y un número (`MAP*2` es válido).
- **Regla semántica:** REDUCE convierte la lista en un solo número, por lo que
  después de REDUCE solo puede venir PRINT. REDUCE es opcional; sin él, el
  resultado es la lista transformada.

## Etapa 1: Java

### Ejecución

Desde la raíz del repositorio, en PowerShell:

```powershell
$fuentes = Get-ChildItem java/src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d java/build $fuentes
java -cp java/build minilang.Main
```

El programa no recibe argumentos: lee `programa.mini` y escribe `programa.ir`
en la carpeta actual. `programa.mini` debe estar guardado en UTF-8 (con o sin
BOM).

| Código de salida | Significado |
|---|---|
| 0 | Programa válido; se generó `programa.ir`. |
| 1 | Error léxico, sintáctico, semántico o de lectura/escritura. |
| 2 | Se pasaron argumentos. |

Si hay un error, el mensaje se muestra en la salida de error y `programa.ir` no
se crea ni se modifica. Si existía uno de una ejecución anterior, queda intacto;
por eso las etapas siguientes deben revisar el código de salida antes de usarlo.

### Errores detectados

| Tipo | Entrada | Mensaje |
|---|---|---|
| Léxico | `FILTER ! 5` | `Linea 2: Caracter no reconocido: !` |
| Sintáctico | `FILTER + 5` | `Linea 2: Se esperaba un comparador >, <, >=, <= o ==; se encontro '+'` |
| Sintáctico | Programa sin DATA | `Linea 1: Se esperaba DATA al inicio del programa; se encontro 'FILTER'` |
| Semántico | `REDUCE SUM` seguido de `MAP * 2` | `Linea 3: MAP necesita una lista, pero REDUCE de la linea 2 ya la convirtio en un solo numero; despues de REDUCE solo puede venir PRINT` |

También se informan con un mensaje claro la falta de `programa.mini` y un
archivo que no está en UTF-8 (por ejemplo, UTF-16 creado con `>` en Windows
PowerShell 5.1). Los caracteres invisibles o no ASCII, que suelen aparecer al
copiar y pegar, se muestran con su código Unicode (por ejemplo, `U+00A0`), y las
palabras desconocidas se muestran completas aunque tengan tildes (`FILTÉR`).

### Diseño

```text
MiniLangCompiler:  Lexer ──► Parser ──► SemanticAnalyzer ──► IrGenerator
                  (tokens)  (List<Instruction>)  (reglas)    (programa.ir)
```

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `Main` | `minilang` | Punto de entrada: muestra mensajes y define el código de salida. |
| `MiniLangCompiler` | `minilang` | Conecta lectura, análisis y escritura; solo escribe si todo es válido. |
| `Lexer` | `minilang.lexer` | Convierte el texto en tokens con su número de línea. |
| `Parser` | `minilang.parser` | Analizador descendente recursivo: un método por regla de la gramática. |
| `SemanticAnalyzer` | `minilang.semantic` | Rechaza operaciones después de REDUCE. |
| `Instruction` y subclases | `minilang.model` | Representan el programa como objetos. |
| `IrGenerator` | `minilang.ir` | Une las líneas que produce cada instrucción. |

Jerarquía de instrucciones:

```text
Instruction (abstracta)
├── DataInstr
├── FilterInstr
├── MapInstr
├── ReduceInstr
└── PrintInstr
```

- **Herencia:** `Instruction` guarda la línea de origen y la validación de
  números, que todas las subclases reutilizan.
- **Sobrescritura:** cada subclase implementa los métodos abstractos `getName()`
  y `toIR()`. FILTER, MAP y REDUCE sobrescriben `requiresList()`, y REDUCE
  también `producesNumber()`.
- **Polimorfismo:** `IrGenerator` y `SemanticAnalyzer` recorren una
  `List<Instruction>` sin preguntar el tipo concreto; Java elige en tiempo de
  ejecución qué implementación usar. Agregar una instrucción nueva solo requiere
  una subclase nueva.

Los objetos son inmutables, los operadores son enumeraciones limitadas a la
gramática y los números usan `BigInteger`, porque la gramática no fija un
límite para los enteros.

## Contrato `programa.ir` (Java → Python)

Una instrucción por línea, campos separados por `|`, números de DATA separados
por comas y sin espacios:

```text
DATA|3,8,5,10,12
FILTER|>|5
MAP|*|2
REDUCE|SUM
PRINT
```

| Instrucción | Formato | Valores |
|---|---|---|
| DATA | `DATA\|n1,n2,...` | Enteros no negativos |
| FILTER | `FILTER\|comparador\|n` | `>` `<` `>=` `<=` `==` |
| MAP | `MAP\|operador\|n` | `+` `-` `*` |
| REDUCE | `REDUCE\|tipo` | `SUM` `MAX` `MIN` |
| PRINT | `PRINT` | — |

La etapa Java garantiza que:

- El archivo está en UTF-8 sin BOM, con saltos de línea LF, incluido uno al final.
- La primera línea es DATA y la última es PRINT; el orden del programa se conserva.
- Hay como máximo un REDUCE y, si existe, está justo antes de PRINT.
- Los enteros se escriben en decimal, sin signo y sin ceros a la izquierda.

`.gitattributes` obliga a Git a conservar los saltos LF de `programa.ir`,
`resultado.txt` y `firma.txt` en cualquier computadora, aunque tenga
`core.autocrlf=true`.

## Pruebas de Java

Desde la raíz del repositorio, en PowerShell:

```powershell
$fuentes = Get-ChildItem java/src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$pruebas = Get-ChildItem tests/java -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -Xlint:all -d java/build $fuentes $pruebas
java -cp java/build LexerTest
java -cp java/build ParserTest
java -cp java/build SemanticAnalyzerTest
java -cp java/build IrGeneratorTest
java -cp java/build JavaStageTest
```

| Prueba | Qué verifica |
|---|---|
| `LexerTest` | Tokens, operadores, líneas (LF, CRLF y CR), números y errores léxicos. |
| `ParserTest` | Construcción de objetos, variantes de la gramática y errores sintácticos con línea. |
| `SemanticAnalyzerTest` | Programas válidos y rechazo de operaciones después de REDUCE. |
| `IrGeneratorTest` | Formato exacto del IR y que no se genere IR para programas inválidos. |
| `JavaStageTest` | Ejecuta la aplicación en carpetas temporales: códigos de salida, mensajes, BOM, UTF-16, archivo ausente y conservación del IR anterior. |

Cada prueba imprime `PASS: ...` si todo está bien; si algo falla, termina con
un `AssertionError` que indica el caso. No se usa JUnit para no depender de
librerías externas.
