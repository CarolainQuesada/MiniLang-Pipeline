@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "REPO=%~dp0"
set "WORKDIR=%REPO%"
if not "%~1"=="" set "WORKDIR=%~f1"
if not exist "%WORKDIR%\programa.mini" (
    echo programa.mini not found in %WORKDIR%
    exit /b 1
)
set "MARS_JAR=%USERPROFILE%\Downloads\Mars45.jar"
if not "%MARS_JAR_OVERRIDE%"=="" set "MARS_JAR=%MARS_JAR_OVERRIDE%"

cd /d "%REPO%"

echo [1/3] Building Java sources and tests...
set "FUENTES="
for /r "%REPO%java\src" %%f in (*.java) do set "FUENTES=!FUENTES! "%%~ff""
set "PRUEBAS="
for %%f in ("%REPO%tests\java\*.java") do set "PRUEBAS=!PRUEBAS! "%%~ff""

javac -encoding UTF-8 -Xlint:all -d "%REPO%java\build" !FUENTES! !PRUEBAS!
if errorlevel 1 (
    echo Java compilation failed.
    exit /b 1
)

echo [2/3] Running Java tests...
java -cp "%REPO%java\build" LexerTest
if errorlevel 1 exit /b 1
java -cp "%REPO%java\build" ParserTest
if errorlevel 1 exit /b 1
java -cp "%REPO%java\build" SemanticAnalyzerTest
if errorlevel 1 exit /b 1
java -cp "%REPO%java\build" IrGeneratorTest
if errorlevel 1 exit /b 1
java -cp "%REPO%java\build" JavaStageTest
if errorlevel 1 exit /b 1

echo [3/3] Running Python tests and the full pipeline...
python -m unittest discover -s "%REPO%tests\python" -v
if errorlevel 1 exit /b 1

cd /d "%WORKDIR%"
java -cp "%REPO%java\build" minilang.Main
if errorlevel 1 exit /b 1
python "%REPO%python\executor.py"
if errorlevel 1 exit /b 1

if not exist "%MARS_JAR%" (
    echo MARS jar not found at %MARS_JAR%
    echo Install Mars45.jar in Downloads or set MARS_JAR_OVERRIDE.
    exit /b 1
)

java -jar "%MARS_JAR%" nc sm ae1 se1 "%REPO%mips\signature.asm"
if errorlevel 1 exit /b 1

cd /d "%REPO%"
python -m unittest discover -s "%REPO%tests\mips" -v
if errorlevel 1 exit /b 1

echo.
echo Pipeline completed successfully.
exit /b 0
