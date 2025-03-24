@echo off
REM Windows batch script to build libsysy.a using MinGW

echo Building SysY library for Windows...
echo.

REM Check if gcc is available (assuming MinGW is installed)
where gcc >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: gcc not found in PATH
    echo Please install MinGW and ensure it's in your PATH
    echo You can download it from: https://www.mingw-w64.org/downloads/
    exit /b 1
)

REM Check if ar is available
where ar >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: ar not found in PATH
    echo This tool is typically included with MinGW
    exit /b 1
)

echo Compiling sylib.c...
gcc -Wall -O2 -c sylib.c

if %ERRORLEVEL% NEQ 0 (
    echo Error: Compilation failed
    exit /b 1
)

echo Creating libsysy.a...
ar rcs libsysy.a sylib.o

if %ERRORLEVEL% NEQ 0 (
    echo Error: Archive creation failed
    exit /b 1
)

echo.
echo Build successful! Library created at: %CD%\libsysy.a
echo.

REM Clean up object file
del sylib.o

exit /b 0 