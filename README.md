# SysY-Compiler

[![GitHub license](https://img.shields.io/github/license/ETOwang/SysY-Compiler.svg)](https://github.com/ETOwang/SysY-Compiler/blob/main/LICENSE)

A powerful compiler for the SysY programming language, a subset of C used for educational purposes. This compiler translates SysY source code into LLVM IR, ARM assembly, or RISC-V assembly.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Command-line Options](#command-line-options)
- [Optimizations](#optimizations)
- [License Statement](#license-statement)
- [Acknowledgements](#acknowledgements)

## Overview

SysY-Compiler is a fully functional compiler designed for the SysY language, a simplified subset of the C programming language often used in compiler design courses. The compiler follows a traditional three-phase design (frontend, middle-end, backend) and supports both ARM and RISC-V assembly generation with various optimization levels.

## Features

- **Complete Compilation Pipeline**: Lexical analysis, syntax parsing, semantic analysis, IR generation, optimization, and assembly code generation
- **Multiple Target Architectures**: Support for both ARM (default) and RISC-V backends
- **Optimization Levels**: Different optimization strategies (-O0, -O1, -O2)
- **LLVM IR Generation**: Option to emit LLVM Intermediate Representation
- **Rich Optimization Techniques**: Includes strength reduction, dead code elimination, loop optimizations, and more

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

## Acknowledgements

This project is a continuation and enhancement of a previous team project. In the original project, the author was primarily responsible for mid-level and backend optimizations, along with minor debugging tasks in both the frontend and backend. This iteration refactors the entire backend implementation and further optimizes parts of the middle-end to improve performance and maintainability.

The original project can be found [here](https://gitlab.eduxiji.net/T202410284203580/compilers). **Note: We apologize that the original repository link may no longer be valid or accessible due to changes in the hosting platform or access permissions.**
  