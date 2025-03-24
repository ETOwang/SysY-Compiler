#!/bin/bash
# Setup script for SysY compiler testing framework
# Installs required dependencies for testing

set -e

# Detect OS
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Detected Linux"
    PACKAGE_MANAGER="apt-get"
    if command -v dnf &> /dev/null; then
        PACKAGE_MANAGER="dnf"
    elif command -v yum &> /dev/null; then
        PACKAGE_MANAGER="yum"
    fi
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Detected macOS"
    if ! command -v brew &> /dev/null; then
        echo "Homebrew not found. Please install Homebrew first: https://brew.sh/"
        exit 1
    fi
    PACKAGE_MANAGER="brew"
elif [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
    echo "Windows detected"
    echo "For Windows, we recommend using WSL2 or manual installation of dependencies."
    echo "Please see the documentation for details."
    exit 1
else
    echo "Unsupported OS: $OSTYPE"
    exit 1
fi

# Install dependencies based on package manager
if [[ "$PACKAGE_MANAGER" == "apt-get" ]]; then
    echo "Installing dependencies with apt-get..."
    sudo apt-get update
    sudo apt-get install -y qemu-user gcc-arm-linux-gnueabihf gcc-riscv64-unknown-elf python3 python3-pip
elif [[ "$PACKAGE_MANAGER" == "dnf" ]] || [[ "$PACKAGE_MANAGER" == "yum" ]]; then
    echo "Installing dependencies with $PACKAGE_MANAGER..."
    sudo $PACKAGE_MANAGER install -y qemu-user gcc-arm-linux-gnu gcc-riscv64-unknown-elf python3 python3-pip
elif [[ "$PACKAGE_MANAGER" == "brew" ]]; then
    echo "Installing dependencies with Homebrew..."
    brew update
    brew install qemu
    brew install --cask gcc-arm-embedded
    brew tap riscv/riscv
    brew install riscv-gnu-toolchain
    brew install python3
fi

# Create directory structure if it doesn't exist
echo "Creating directory structure..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_ROOT="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$TEST_ROOT")"

mkdir -p "$TEST_ROOT/cases/syntax"
mkdir -p "$TEST_ROOT/cases/semantic"
mkdir -p "$TEST_ROOT/cases/optimization"
mkdir -p "$TEST_ROOT/cases/functional"
mkdir -p "$TEST_ROOT/expected"
mkdir -p "$TEST_ROOT/emulators"
mkdir -p "$PROJECT_ROOT/lib"

# Check for required library files
LIB_DIR="$PROJECT_ROOT/lib"
SYLIB_H="$LIB_DIR/sylib.h"
LIBSYSY_A="$LIB_DIR/libsysy.a"

if [[ ! -f "$SYLIB_H" ]]; then
    echo "Warning: sylib.h not found at $SYLIB_H"
    echo "SysY library functions will not be available for testing."
fi

if [[ ! -f "$LIBSYSY_A" ]]; then
    echo "Warning: libsysy.a not found at $LIBSYSY_A"
    echo "SysY library functions will not be available for testing."
fi

echo "Installation complete. You can now run tests with:"
echo "  python3 $SCRIPT_DIR/run_tests.py" 