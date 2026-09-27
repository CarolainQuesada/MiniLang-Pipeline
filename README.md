# MiniLang-Pipeline

Proyecto en pareja para la Parte B del examen de Paradigmas de Programación.

## Estado actual

Se ha preparado la estructura inicial, el modelo de instrucciones Java y el lexer.
Todavía no hay parser, generación de IR ni aplicación ejecutable.
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

Para compilar todo Java y ejecutar las pruebas del lexer desde PowerShell:

```powershell
$fuentes = Get-ChildItem java/src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -Xlint:all -d java/build $fuentes pruebas/java/LexerTest.java
java -cp java/build LexerTest
```

## Trabajo por pasos

`develop` es la base de las ramas de trabajo y contiene la estructura inicial
y el modelo Java. La etapa del lexer se desarrolla en `feature/java-lexer`,
creada desde `develop`.

Cada tarea pequeña se desarrolla en una rama específica y se registra mediante
un commit en inglés siguiendo Conventional Commits. No se integra en `main`
sin autorización. Tras cada paso se revisa el resultado y se espera la
indicación explícita `continuar` antes de avanzar.
