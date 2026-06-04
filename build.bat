@echo off
setlocal enabledelayedexpansion

:: --- SETTINGS ---
set "STARSECTOR_CORE=C:\Program Files (x86)\Fractal Softworks\Starsector\starsector-core"
set "MODS_DIR=C:\Program Files (x86)\Fractal Softworks\Starsector\mods"
set "JAR_NAME=Mad-Avionics"

:: --- BUILD CLASSPATH ---
:: Initialize classpath with core jars
set "CLASSPATH="
for /r "%STARSECTOR_CORE%" %%i in (*.jar *.zip) do (
    set "CLASSPATH=!CLASSPATH!%%i;"
)

:: Add specific Mod dependencies
set "CLASSPATH=%CLASSPATH%%MODS_DIR%\LazyLib\jars\LazyLib.jar;"
set "CLASSPATH=%CLASSPATH%%MODS_DIR%\MagicLib\jars\MagicLib.jar;"
set "CLASSPATH=%CLASSPATH%%MODS_DIR%\GraphicsLib\jars\Graphics.jar;"
set "CLASSPATH=%CLASSPATH%%MODS_DIR%\Nexerelin\jars\ExerelinCore.jar;"
set "CLASSPATH=%CLASSPATH%%MODS_DIR%\LunaLib\jars\LunaLib.jar;"
set "CLASSPATH=%CLASSPATH%%MODS_DIR%\Diable-Avionics\jars\Diable_Avionics.jar;"

:: --- PREPARE DIRECTORIES ---
if not exist bin mkdir bin
if not exist jars mkdir jars

:: --- GENERATE SOURCES LIST ---
:: Windows equivalent of 'find' for java files
if exist sources.txt del sources.txt
for /f "delims=" %%f in ('dir /s /b src\*.java') do (
    set "LINE=%%f"
    set "LINE=!LINE:\=/!"
    echo "!LINE!" >> sources.txt
)

:: --- COMPILE ---
echo Building ...
javac -Xlint:-options --release 17 -sourcepath "" -implicit:none -cp "%CLASSPATH%" -d bin @sources.txt

echo Packaging ...

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

:: --- PACKAGE ---
cd bin
jar cf ..\jars\MadAvionics.jar  .
cd ..

echo Jar built successfully: jars/%JAR_NAME%
pause