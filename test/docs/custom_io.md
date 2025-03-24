# Adding Custom I/O to SysY Tests

SysY as a language subset doesn't have built-in I/O functions like `printf` and `scanf`, which can make testing more complex behaviors challenging. This document explains how to implement custom I/O for more advanced testing.

## Approach 1: Using Exit Codes

The simplest approach is to return test results as exit codes, which our testing framework already captures:

```c
int main() {
    int result = perform_calculation();
    return result;  // This becomes the exit code
}
```

**Limitation**: This only works for single integer values in a specific range (typically 0-255).

## Approach 2: Implementing External Functions

For more complex testing, you can declare external functions that will be linked to C standard library functions:

1. Declare the functions in your SysY file:

```c
// Declare external functions
int getchar();
void putchar(int c);
```

2. Create a C wrapper file for these functions (e.g., `io_wrapper.c`):

```c
#include <stdio.h>

int getchar_wrapper() {
    return getchar();
}

void putchar_wrapper(int c) {
    putchar(c);
}
```

3. Update your test script to compile and link with this wrapper:

```bash
# 1. Compile SysY to assembly
java -jar compiler.jar input.sy -S -o output.s

# 2. Compile the wrapper
gcc -c io_wrapper.c -o io_wrapper.o

# 3. Link them together
gcc output.s io_wrapper.o -o test_executable

# 4. Run and capture output
./test_executable > actual_output.txt
```

## Approach 3: Using System Calls Directly

For even more control, you can implement I/O using system calls directly in your SysY file:

```c
// SysY program using system calls for I/O
int main() {
    // ARM system call numbers:
    // write: 4, read: 3, stdout: 1
    
    // Example: writing a character to stdout
    int c = 65; // ASCII 'A'
    int syscall_num = 4; // write
    int file_descriptor = 1; // stdout
    int buffer_address = c;
    int length = 1;
    
    // ARM system call (using inline assembly via a wrapper function)
    syscall(syscall_num, file_descriptor, buffer_address, length);
    
    return 0;
}
```

This approach requires implementing the `syscall` function differently for each target architecture and is more advanced.

## Integration with the Testing Framework

To integrate custom I/O with our testing framework:

1. Create a directory for I/O utilities:

```bash
mkdir -p test/utils/io
```

2. Add reusable wrappers:

```bash
cp io_wrapper.c test/utils/io/
```

3. Update `run_tests.py` to link with these utilities when needed.

## Example: Testing a Calculation with Output

Here's a complete example of a test that calculates Fibonacci numbers and writes them to stdout:

```c
// Declare external function
void putchar(int c);

// Helper to print a number
void print_number(int n) {
    int digits[10];
    int count = 0;
    
    // Handle 0 specially
    if (n == 0) {
        putchar('0');
        return;
    }
    
    // Extract digits
    while (n > 0) {
        digits[count] = n % 10;
        n = n / 10;
        count = count + 1;
    }
    
    // Print digits in reverse order
    while (count > 0) {
        count = count - 1;
        putchar('0' + digits[count]);
    }
    
    // Print newline
    putchar('\n');
}

// Calculate and print Fibonacci numbers
int main() {
    int a = 0;
    int b = 1;
    int i = 0;
    int temp;
    
    while (i < 10) {
        print_number(a);
        temp = a;
        a = b;
        b = temp + b;
        i = i + 1;
    }
    
    return 0;
}
```

With this approach, your test can produce more complex output that can be captured and compared to expected results. 