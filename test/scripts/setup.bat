@echo off
:: Setup script for SysY compiler testing framework on Windows
:: Note: This approach is more complex; using WSL is recommended

echo SysY Compiler Testing Framework - Windows Setup

:: Create directory structure
echo Creating directory structure...
mkdir "%~dp0..\cases\syntax" 2>nul
mkdir "%~dp0..\cases\semantic" 2>nul
mkdir "%~dp0..\cases\optimization" 2>nul
mkdir "%~dp0..\cases\functional" 2>nul
mkdir "%~dp0..\expected" 2>nul
mkdir "%~dp0..\emulators" 2>nul
mkdir "%~dp0..\..\lib" 2>nul

:: Check for library files
set "PROJECT_ROOT=%~dp0..\.."
set "LIB_DIR=%PROJECT_ROOT%\lib"
set "SYLIB_H=%LIB_DIR%\sylib.h"
set "LIBSYSY_A=%LIB_DIR%\libsysy.a"

if not exist "%SYLIB_H%" (
    echo Warning: sylib.h not found at %SYLIB_H%
    echo SysY library functions will not be available for testing.
)

if not exist "%LIBSYSY_A%" (
    echo Warning: libsysy.a not found at %LIBSYSY_A%
    echo SysY library functions will not be available for testing.
)

echo.
echo NOTE: Windows requires manual installation of dependencies:
echo.
echo 1. Install Python 3 from: https://www.python.org/downloads/
echo 2. Install MinGW for local testing: https://www.mingw-w64.org/downloads/
echo.
echo For ARM and RISC-V emulation, you have several options:
echo.
echo A. Install WSL2 (recommended) and run setup.sh within Linux
echo    See: https://docs.microsoft.com/en-us/windows/wsl/install
echo.
echo B. Use Docker with ARM and RISC-V images
echo    See: https://hub.docker.com/r/arm64v8/ubuntu/ and https://hub.docker.com/r/riscv64/ubuntu/
echo.
echo C. Install QEMU and cross-compilers manually
echo    - QEMU: https://www.qemu.org/download/#windows
echo    - ARM toolchain: https://developer.arm.com/tools-and-software/open-source-software/developer-tools/gnu-toolchain/downloads
echo    - RISC-V toolchain: https://github.com/riscv/riscv-gnu-toolchain
echo.
echo After installation, you may need to modify run_tests.py to point to your installed tools
echo.

echo Setup complete. You can now run tests with:
echo python "%~dp0run_tests.py"

pause 