# SysY Compiler Testing Framework Overview

## Architecture

The SysY Compiler Testing Framework is designed to systematically test the correctness and performance of the SysY compiler across different architectures and optimization levels. The framework consists of the following components:

```
test/
├── cases/                # Test cases organized by category
│   ├── syntax/           # Tests for syntax validation
│   ├── semantic/         # Tests for semantic analysis
│   ├── optimization/     # Tests for specific optimizations
│   └── functional/       # End-to-end functional tests
├── expected/             # Expected outputs for test cases
├── scripts/              # Testing utilities
│   ├── run_tests.py      # Main test runner script
│   ├── create_test.py    # Helper to create new tests
│   ├── setup.sh          # Linux/Mac dependency installer
│   └── setup.bat         # Windows setup script
├── docs/                 # Documentation
│   ├── overview.md       # This file
│   └── custom_io.md      # Guide for implementing I/O in tests
├── utils/                # Utility code for tests
│   └── io/               # I/O utilities for advanced tests
└── emulators/            # Configuration for architecture emulators
```

## How It Works

1. **Test Collection**: The framework scans the `cases/` directory to find all test cases, parsing their configuration from comments.

2. **Compilation**: For each test case and configuration (target + optimization level), the framework compiles the SysY source file to assembly code.

3. **Execution**: The compiled assembly is then executed in the appropriate emulator (QEMU for ARM or RISC-V), with output captured.

4. **Validation**: The output is compared against the expected output found in the `expected/` directory.

5. **Reporting**: Results are collected and summarized.

## Test Categories

The framework organizes tests into several categories:

- **Syntax Tests**: Validate the parser's ability to correctly identify valid and invalid SysY syntax.
- **Semantic Tests**: Check for proper semantic analysis, including type checking, scope resolution, etc.
- **Optimization Tests**: Target specific optimizations to ensure they are applied correctly.
- **Functional Tests**: Validate the end-to-end behavior of compiled programs.

## Test Case Format

Each test case consists of:

1. A SysY source file (`.sy`) with an optional configuration comment:
   ```c
   // TEST_CONFIG: target=arm,riscv; optimization=O0,O1,O2; timeout=30
   ```

2. An expected output file (`.out`) with the same base name in the `expected/` directory.

## Key Features

- **Cross-Architecture Testing**: Tests can be run against multiple target architectures (ARM, RISC-V).
- **Optimization Level Testing**: Tests can be executed with different optimization levels (O0, O1, O2).
- **Detailed Reporting**: Results include pass/fail status, error messages, and execution outputs.
- **Custom I/O Support**: Advanced tests can implement custom I/O mechanisms for more complex validation.
- **Extensibility**: The framework can be extended with new test categories and target architectures.

## Running Tests

Basic usage:
```bash
python scripts/run_tests.py
```

Run a specific category of tests:
```bash
python scripts/run_tests.py --category functional
```

Run with specific configuration:
```bash
python scripts/run_tests.py --target arm --optimization O2
```

Run a single test file:
```bash
python scripts/run_tests.py --file cases/functional/factorial.sy
```

Enable verbose output:
```bash
python scripts/run_tests.py -v
```

## Creating New Tests

To create a new test:
```bash
python scripts/create_test.py functional my_new_test --description "My new test case"
```

This will create:
- `cases/functional/my_new_test.sy` with a skeleton test case
- `expected/my_new_test.out` with a default expected output

Edit these files to implement your test case.

## Configuration Parameters

Test cases can specify the following configuration parameters:

- `target`: Target architectures to test (`arm`, `riscv`)
- `optimization`: Optimization levels to test (`O0`, `O1`, `O2`)
- `timeout`: Maximum execution time in seconds

## Dependencies

The framework requires:

- Python 3.6 or higher
- QEMU for ARM and RISC-V emulation
- GCC cross-compilers for ARM and RISC-V
- SysY Compiler JAR file

Use the provided setup scripts (`setup.sh` or `setup.bat`) to install these dependencies. 