# MiniLang-Pipeline

Proyecto en pareja para la Parte B del examen de Paradigmas de Programación.

## Estado actual

Se ha preparado la estructura inicial, el modelo de instrucciones Java, el lexer,
el parser y la generación de texto IR en memoria. Todavía no hay lectura de
`programa.mini`, escritura de `programa.ir` ni aplicación ejecutable.
No se utilizan dependencias externas.

## Estructura

```text
java/src/      Código fuente de la etapa Java.
python/        Reservado para el motor funcional de la compañera.
mips/          Reservado para la implementación MIPS de la compañera.
pruebas/java/  Pruebas exclusivas de la etapa Java.
```

Los archivos `.gitkeep` permiten conservar las carpetas vacías en Git.

## División del trabajo

Mi parte comprende el modelo orientado a objetos, lexer, parser, errores con
número de línea y generación de `programa.ir` desde `programa.mini` válido,
además de las pruebas y documentación correspondientes a Java.

La parte de mi compañera comprende el motor funcional Python (FILTER, MAP y
REDUCE), `resultado.txt`, MIPS y su checksum/firma, `firma.txt`, la integración
final, `ejecutar.bat` y el conjunto completo de casos de prueba obligatorios.

La implementación Java separará lectura, análisis, modelo y generación de IR.

## Modelo Java

El paquete `minilang.modelo` contiene la clase abstracta `Instruccion` y sus
subclases `DataInstr`, `FilterInstr`, `MapInstr`, `ReduceInstr` y `PrintInstr`.
La clase base conserva la línea de origen (desde 1); cada subclase implementa
`getNombre()`. Así, una colección de `Instruccion` puede consultar los nombres
mediante polimorfismo, sin comprobar el tipo concreto.

Los objetos son inmutables: DATA copia su lista y los operadores se representan
mediante enumeraciones limitadas a la gramática. Los números usan `BigInteger`
para no imponer un límite artificial de `int` a los enteros no negativos.
Los constructores rechazan datos inválidos; la detección y presentación de
errores del archivo fuente corresponderá al análisis en pasos posteriores.
Estas clases solo representan instrucciones: no ejecutan operaciones.

Para compilar el modelo desde la raíz del repositorio con JDK 21 en PowerShell:

```powershell
javac -encoding UTF-8 -d java/build java/src/minilang/modelo/*.java
```

## Lexer Java

`minilang.lexer.Lexer` recibe texto y devuelve tokens inmutables con tipo,
lexema y número de línea, más un token final `EOF`. Reconoce las palabras
reservadas en mayúsculas, dígitos ASCII y los operadores de la gramática.
Admite espacios, tabulaciones y saltos LF, CRLF o CR; CRLF cuenta una sola línea.
Los símbolos o palabras desconocidos producen `ErrorLexico` con la línea.

Los números conservan su texto sin convertirlo a `int`. El signo `-` se reconoce
como operador independiente: será responsabilidad del parser rechazarlo donde
se espere un número no negativo. El lexer tampoco comprueba el orden de las
instrucciones ni exige una instrucción por línea, pues la gramática no establece
esa restricción. No se admiten comentarios ni operadores adicionales.

## Parser Java

`minilang.parser.Parser` recibe los tokens del lexer y devuelve una lista
inmutable de `Instruccion`. Usa análisis descendente con métodos pequeños para
DATA, FILTER, MAP y REDUCE. Valida exactamente la estructura
`DATA <numero> { <numero> } <operacion> { <operacion> } PRINT` y exige el fin
del archivo después de PRINT. Cada instrucción conserva su línea de inicio.

El primer error produce `ErrorSintactico` con la línea del token inesperado,
lo esperado y lo encontrado. Si falta contenido al final, informa la línea de
EOF. Rechaza números negativos, operandos faltantes, operadores fuera de su
contexto e instrucciones fuera de orden. No agrega restricciones semánticas:
por ejemplo, la gramática permite varias reducciones y operaciones después de
REDUCE. No ejecuta operaciones, lee archivos ni escribe IR.

Uso desde código Java: `new Parser(new Lexer(texto).tokenizar()).parsear()`.
La lista de entrada debe provenir del lexer y terminar en un único token EOF.

## Generación de IR

`minilang.ir.GeneradorIR` serializa la lista de instrucciones validada por el
parser. Mantiene la lógica del formato fuera del modelo y del análisis.
Utiliza selección por tipo de Java 21, sin ejecutar FILTER, MAP ni REDUCE.
La validación del programa sigue siendo responsabilidad del parser: se debe
completar antes de llamar al generador.

El contrato de salida es una instrucción por línea, con campos separados por
`|`, números de DATA separados por comas y ningún espacio adicional:

```text
DATA|3,8,5,10,12
FILTER|>|5
MAP|*|2
REDUCE|SUM
PRINT
```

Se usan saltos LF (`\n`), incluido uno al final. Los enteros se escriben en
decimal sin ceros iniciales. El orden de las instrucciones se conserva.
El resultado es texto en memoria; la escritura del archivo y su codificación
se conectarán en el siguiente paso de la etapa Java.

Uso desde Java:

```java
var instrucciones = new Parser(new Lexer(texto).tokenizar()).parsear();
String ir = new GeneradorIR().generar(instrucciones);
```

Para compilar todo Java y ejecutar las pruebas desde PowerShell con JDK 21:

```powershell
$fuentes = Get-ChildItem java/src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$pruebas = Get-ChildItem pruebas/java -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -Xlint:all -d java/build $fuentes $pruebas
java -cp java/build LexerTest
java -cp java/build ParserTest
java -cp java/build GeneradorIRTest
```

## Trabajo por pasos

`develop` es la base de las ramas de trabajo y contiene la estructura inicial,
el modelo Java, el lexer y el parser. La generación de IR se desarrolla en
`feature/java-ir-generator`, creada desde `develop` después de incorporar el parser
mediante avance directo (fast-forward).

Cada tarea pequeña se desarrolla en una rama específica y se registra mediante
un commit en inglés siguiendo Conventional Commits. No se integra en `main`
sin autorización. Tras cada paso se revisa el resultado y se espera la
indicación explícita `continuar` antes de avanzar.
