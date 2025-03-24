# SysY-Compiler

[![GitHub license](https://img.shields.io/github/license/ETOwang/SysY-Compiler.svg)](https://github.com/ETOwang/SysY-Compiler/blob/main/LICENSE)

A powerful compiler for the SysY programming language, a subset of C used for educational purposes. This compiler translates SysY source code into LLVM IR, ARM assembly, or RISC-V assembly.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Library Functions](#library-functions)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Command-line Options](#command-line-options)
- [Optimizations](#optimizations)
- [License Statement](#license-statement)
- [Testing Framework](#testing-framework)
- [Acknowledgements](#acknowledgements)


## Overview

SysY-Compiler is a fully functional compiler designed for the SysY language, a simplified subset of the C programming language often used in compiler design courses. The compiler follows a traditional three-phase design (frontend, middle-end, backend) and supports both ARM and RISC-V assembly generation with various optimization levels.

## Features

- **Complete Compilation Pipeline**: Lexical analysis, syntax parsing, semantic analysis, IR generation, optimization, and assembly code generation
- **Multiple Target Architectures**: Support for both ARM (default) and RISC-V backends
- **Optimization Levels**: Different optimization strategies (-O0, -O1, -O2)
- **LLVM IR Generation**: Option to emit LLVM Intermediate Representation
- **Rich Optimization Techniques**: Includes strength reduction, dead code elimination, loop optimizations, and more
- **Automatic Library Function Recognition**: SysY library functions are automatically recognized and don't require explicit imports

## Architecture

The compiler is structured into three main components:

### Frontend

- **Lexer/Parser**: Built with ANTLR4 for syntax analysis
- **Semantic Analysis**: Type checking and validation
- **IR Generation**: Translates SysY programs into LLVM-like intermediate representation

### Middle-end (Optimizer)

Implements various optimization passes:
- Constant propagation and folding
- Dead code elimination
- Common subexpression elimination
- Loop invariant code motion
- Strength reduction
- Memory-to-register promotion
- Function inlining
- And many more...

### Backend

- **ARM Backend**: Generates optimized ARM assembly code
- **RISC-V Backend**: Generates RISC-V assembly code
- **Register Allocation**: Efficient register allocation algorithms
- **Instruction Selection**: Target-specific instruction selection and peephole optimizations

## Library Functions

SysY includes a set of built-in library functions defined in `sylib.h` that provide I/O and timing functionality. The compiler automatically recognizes these functions without requiring explicit imports in the source files.

### Available Library Functions

#### Input/Output Functions
- `int getint()`: Reads an integer from stdin
- `int getch()`: Reads a character from stdin
- `float getfloat()`: Reads a floating-point number from stdin
- `int getarray(int a[])`: Reads an array of integers
- `int getfarray(float a[])`: Reads an array of floating-point numbers
- `void putint(int a)`: Prints an integer to stdout
- `void putch(int a)`: Prints a character to stdout
- `void putarray(int n, int a[])`: Prints an array of integers
- `void putfloat(float a)`: Prints a floating-point number
- `void putfarray(int n, float a[])`: Prints an array of floating-point numbers
- `void putf(char a[], ...)`: Printf-like function for formatted output

#### Timing Functions
- `void starttime()`: Start timing
- `void stoptime()`: Stop timing and record elapsed time

### Library Files

The SysY library files are located in the `lib` directory:
- `lib/sylib.h`: Header file containing library function declarations
- `lib/libsysy.a`: Static library for linking with compiled SysY programs
- `lib/sylib.c`: Source code for the library functions
- `lib/Makefile`: Makefile for rebuilding the library

#### Rebuilding the Library

If you need to modify the library functions or rebuild the static library, you can use the provided Makefile:

```bash
cd lib
make clean
make
```

This will recompile the source files and regenerate the `libsysy.a` static library.

### Using Library Functions

To use these library functions in your SysY programs, simply call them directly without any `#include` statement:

```c
int main() {
    int a = getint();
    putint(a + 1);
    putch(10);  // newline
    return 0;
}
```

The compiler will automatically recognize and link these functions using `libsysy.a` during compilation.

## Getting Started

### Prerequisites

- JDK 17 or higher
- Maven

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/ETOwang/SysY-Compiler.git
   cd SysY-Compiler
   ```

2. **Build the project**:
   ```bash
   mvn clean compile assembly:single
   ```

## Usage

### Basic Usage

Compile a SysY source file to ARM assembly (default):

```bash
java -jar target/compiler-1.0-SNAPSHOT-jar-with-dependencies.jar input.sy -S -o output.s
```

### Generate LLVM IR

```bash
java -jar target/compiler-1.0-SNAPSHOT-jar-with-dependencies.jar input.sy --emit-llvm -o output.ll
```

### Generate RISC-V Assembly

```bash
java -jar target/compiler-1.0-SNAPSHOT-jar-with-dependencies.jar input.sy -S --target riscv -o output.s
```

### Enable Optimizations

```bash
# Level 1 optimizations
java -jar target/compiler-1.0-SNAPSHOT-jar-with-dependencies.jar input.sy -S -O1 -o output.s

# Level 2 optimizations
java -jar target/compiler-1.0-SNAPSHOT-jar-with-dependencies.jar input.sy -S -O2 -o output.s
```

## Command-line Options

| Option | Description |
|--------|-------------|
| `-o <file>` | Specify output file |
| `-S` | Generate assembly code |
| `--emit-llvm` | Generate LLVM IR |
| `-O0` | No optimizations (default) |
| `-O1`, `-O2` | Enable optimizations |
| `--target <arch>` | Specify target architecture (arm or riscv, default: arm) |

## Optimizations

The compiler implements a wide range of optimization techniques:

### Middle-end Optimizations

- **Constant Propagation and Folding**: Evaluates constant expressions at compile time
- **Dead Code Elimination**: Removes code that doesn't affect program output
- **Common Subexpression Elimination**: Avoids redundant computations
- **Strength Reduction**: Replaces expensive operations with cheaper ones
- **Memory-to-Register Promotion**: Optimizes memory accesses
- **Loop Invariant Code Motion**: Moves code outside of loops
- **Function Inlining**: Replaces function calls with function bodies
- **Redundant Block Elimination**: Removes unnecessary basic blocks
- **Global Variable Localization**: Converts globals to locals when possible

### Backend Optimizations

- **Peephole Optimizations**: Local instruction-level optimizations
- **Register Allocation**: Efficiently assigns variables to registers
- **Instruction Selection**: Chooses optimal target machine instructions

## License Statement

This project is licensed under the [Apache License 2.0 License](https://img.shields.io/github/license/ETOwang/SysY-Compiler.svg) - see the [LICENSE](https://github.com/ETOwang/SysY-Compiler/blob/main/LICENSE) file for details.

## Testing Framework

The compiler includes a comprehensive cross-platform testing framework to validate functionality across different target architectures and optimization levels. The framework supports Windows, macOS, and Linux platforms.

### Setting up the Testing Environment

```bash
# On Linux or macOS
python3 test/scripts/setup.py

# On Windows
python test/scripts/setup.py
```

The setup script will detect your operating system, create the necessary directory structure, and guide you through installing any required dependencies.

### Running Tests

```bash
# Run all tests
python3 test/scripts/run_tests.py

# Run tests with specific options
python3 test/scripts/run_tests.py --target arm --optimization O2

# Run tests in a specific category
python3 test/scripts/run_tests.py --category functional

# Run a single test file
python3 test/scripts/run_tests.py --file test/cases/functional/example.sy

# Verbose output for debugging
python3 test/scripts/run_tests.py --verbose
```

### Adding New Tests

1. Create a SysY source file in the appropriate category directory under `test/cases/`
2. Create a corresponding expected output file in `test/expected/`
3. Optionally, add test configuration in a comment at the top of your test file:

```c
// TEST_CONFIG: target=arm,riscv; optimization=O0,O1,O2; timeout=30
```

For more detailed information about the testing framework, see [test/README.md](test/README.md).

## Acknowledgements

This project is a continuation and enhancement of a previous team project. In the original project, the author was primarily responsible for mid-level and backend optimizations, along with minor debugging tasks in both the frontend and backend. This iteration refactors the entire backend implementation and further optimizes parts of the middle-end to improve performance and maintainability.

The original project can be found [here](https://gitlab.eduxiji.net/T202410284203580/compilers). **Note: We apologize that the original repository link may no longer be valid or accessible due to changes in the hosting platform or access permissions.**


  