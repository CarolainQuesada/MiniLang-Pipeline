# MiniLang-Pipeline

Proyecto en pareja para la Parte B del examen de Paradigmas de Programación.

## Estado actual

Solo se ha preparado la estructura inicial. Todavía no hay código ejecutable,
dependencias externas ni lógica del examen.

## Estructura

```text
java/src/      Código fuente de la etapa Java.
python/        Reservado para el motor funcional de la compañera.
mips/          Reservado para la implementación MIPS de la compañera.
pruebas/java/  Pruebas exclusivas de la etapa Java, pendientes.
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
Las clases y sus paquetes se incorporarán en los pasos correspondientes.

## Trabajo por pasos

Cada tarea pequeña se desarrolla en una rama específica y se registra mediante
un commit en inglés siguiendo Conventional Commits. No se integra en `main`
sin autorización. Tras cada paso se revisa el resultado y se espera la
indicación explícita `continuar` antes de avanzar.
