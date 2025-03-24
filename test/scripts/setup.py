#!/usr/bin/env python3
"""
SysY Compiler Testing Framework - Cross-platform Setup
This script prepares the environment for testing the SysY compiler across different platforms.
"""

import os
import sys
import platform
import subprocess
import shutil
from pathlib import Path

# Detect operating system
IS_WINDOWS = platform.system() == "Windows"
IS_MACOS = platform.system() == "Darwin"
IS_LINUX = platform.system() == "Linux"

# Set up paths
SCRIPT_DIR = Path(os.path.dirname(os.path.abspath(__file__)))
TEST_ROOT = SCRIPT_DIR.parent
PROJECT_ROOT = TEST_ROOT.parent
LIB_DIR = PROJECT_ROOT / "lib"

def create_directory_structure():
    """Create the necessary directories for testing"""
    print("Creating directory structure...")
    
    directories = [
        TEST_ROOT / "cases" / "syntax",
        TEST_ROOT / "cases" / "semantic",
        TEST_ROOT / "cases" / "optimization",
        TEST_ROOT / "cases" / "functional",
        TEST_ROOT / "expected",
        TEST_ROOT / "emulators",
        LIB_DIR
    ]
    
    for directory in directories:
        directory.mkdir(exist_ok=True, parents=True)
        
    print("Directory structure created successfully.")

def check_library_files():
    """Check if the required library files exist"""
    sylib_h = LIB_DIR / "sylib.h"
    libsysy_a = LIB_DIR / "libsysy.a"
    
    missing_files = []
    if not sylib_h.exists():
        missing_files.append(str(sylib_h))
    
    if not libsysy_a.exists():
        missing_files.append(str(libsysy_a))
    
    if missing_files:
        print("Warning: The following library files are missing:")
        for file in missing_files:
            print(f"  - {file}")
        print("SysY library functions may not be available for testing.")
        
        # If sylib.c exists but libsysy.a doesn't, suggest rebuilding
        if (LIB_DIR / "sylib.c").exists() and not libsysy_a.exists():
            print("\nYou can rebuild the library by running:")
            if IS_WINDOWS:
                print("  cd lib && gcc -c sylib.c && ar rcs libsysy.a sylib.o")
            else:
                print("  cd lib && make")

def check_dependencies():
    """Check if required dependencies are installed"""
    print("Checking dependencies...")
    
    if IS_LINUX:
        check_linux_dependencies()
    elif IS_MACOS:
        check_macos_dependencies()
    elif IS_WINDOWS:
        check_windows_dependencies()
    else:
        print(f"Unsupported platform: {platform.system()}")

def check_linux_dependencies():
    """Check dependencies on Linux"""
    missing = []
    
    # Check for Python 3
    if sys.version_info.major < 3:
        missing.append("Python 3")
    
    # Check for QEMU
    if not shutil.which("qemu-arm"):
        missing.append("qemu-user (qemu-arm)")
    
    # Check for ARM toolchain
    if not shutil.which("arm-linux-gnueabihf-gcc"):
        missing.append("gcc-arm-linux-gnueabihf")
    
    # Check for RISC-V toolchain
    if not shutil.which("riscv64-unknown-elf-gcc"):
        missing.append("gcc-riscv64-unknown-elf")
    
    if missing:
        print("Missing dependencies:")
        for dep in missing:
            print(f"  - {dep}")
        
        print("\nYou can install them with:")
        print("  sudo apt-get update")
        print("  sudo apt-get install -y qemu-user gcc-arm-linux-gnueabihf gcc-riscv64-unknown-elf python3")
    else:
        print("All dependencies are installed.")

def check_macos_dependencies():
    """Check dependencies on macOS"""
    missing = []
    
    # Check for Python 3
    if sys.version_info.major < 3:
        missing.append("Python 3")
    
    # Check for QEMU
    if not shutil.which("qemu-arm"):
        missing.append("qemu")
    
    # Check for ARM toolchain
    if not shutil.which("arm-none-eabi-gcc"):
        missing.append("gcc-arm-embedded")
    
    # Check for RISC-V toolchain
    if not shutil.which("riscv64-unknown-elf-gcc"):
        missing.append("riscv-gnu-toolchain")
    
    if missing:
        print("Missing dependencies:")
        for dep in missing:
            print(f"  - {dep}")
        
        print("\nYou can install them with Homebrew:")
        print("  brew update")
        print("  brew install qemu")
        print("  brew install --cask gcc-arm-embedded")
        print("  brew tap riscv/riscv")
        print("  brew install riscv-gnu-toolchain")
    else:
        print("All dependencies are installed.")

def check_windows_dependencies():
    """Check dependencies on Windows"""
    print("On Windows, dependencies need to be installed manually.")
    print("Options for setup:")
    print("\n1. Install Python 3 from: https://www.python.org/downloads/")
    print("2. Install MinGW for local testing: https://www.mingw-w64.org/downloads/")
    print("\nFor ARM and RISC-V emulation, you have several options:")
    print("\nA. Use WSL2 (recommended) and run this script within Linux")
    print("   See: https://docs.microsoft.com/en-us/windows/wsl/install")
    print("\nB. Use Docker with ARM and RISC-V images")
    print("   See: https://hub.docker.com/r/arm64v8/ubuntu/ and https://hub.docker.com/r/riscv64/ubuntu/")
    print("\nC. Install QEMU and cross-compilers manually:")
    print("   - QEMU: https://www.qemu.org/download/#windows")
    print("   - ARM toolchain: https://developer.arm.com/tools-and-software/open-source-software/developer-tools/gnu-toolchain/downloads")
    print("   - RISC-V toolchain: https://github.com/riscv/riscv-gnu-toolchain")
    
    # Check if running in a WSL environment
    if "microsoft" in platform.uname().release.lower():
        print("\nWSL detected! You can install Linux dependencies with:")
        print("  sudo apt-get update")
        print("  sudo apt-get install -y qemu-user gcc-arm-linux-gnueabihf gcc-riscv64-unknown-elf")

def main():
    print(f"SysY Compiler Testing Framework - Setup ({platform.system()})")
    print("-" * 60)
    
    create_directory_structure()
    check_library_files()
    check_dependencies()
    
    print("\nSetup complete. You can now run tests with:")
    if IS_WINDOWS:
        print(f"  python {SCRIPT_DIR / 'run_tests.py'}")
    else:
        print(f"  python3 {SCRIPT_DIR / 'run_tests.py'}")

if __name__ == "__main__":
    main() 