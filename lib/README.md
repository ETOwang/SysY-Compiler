# SysY Library Functions

This directory contains the library files for the SysY compiler. These files provide I/O and timing functionality that can be used in SysY programs without explicit imports.

## Files

- `sylib.h`: Header file containing declarations for all library functions
- `sylib.c`: Source code implementation of the library functions
- `libsysy.a`: Compiled static library for linking with SysY programs
- `Makefile`: Script for rebuilding the library

## Available Functions

### Input Functions
- `int getint()`: Reads an integer from stdin
- `int getch()`: Reads a character from stdin
- `float getfloat()`: Reads a floating-point number from stdin
- `int getarray(int a[])`: Reads an array of integers
- `int getfarray(float a[])`: Reads an array of floating-point numbers

### Output Functions
- `void putint(int a)`: Prints an integer to stdout
- `void putch(int a)`: Prints a character to stdout
- `void putarray(int n, int a[])`: Prints an array of integers
- `void putfloat(float a)`: Prints a floating-point number
- `void putfarray(int n, float a[])`: Prints an array of floating-point numbers
- `void putf(char a[], ...)`: Printf-like function for formatted output

### Timing Functions
- `void starttime()`: Start timing (defined as a macro in sylib.h)
- `void stoptime()`: Stop timing and record elapsed time (defined as a macro in sylib.h)

## Rebuilding the Library

If you need to modify the library functions, you can rebuild the library using the provided Makefile:

```bash
make clean
make
```

This will recompile the source files and regenerate the `libsysy.a` static library.

## Integration with SysY Programs

The SysY compiler automatically recognizes these library functions without requiring explicit imports in the source files. When compiling SysY programs, the generated assembly is linked with `libsysy.a` to provide the implementations. 