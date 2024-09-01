# SysY-Compiler

[![GitHub license](https://img.shields.io/github/license/ETOwang/SysY-Compiler.svg)](https://github.com/ETOwang/SysY-Compiler/blob/main/LICENSE)

## License Statement

This project is licensed under the [Apache License 2.0 License](https://img.shields.io/github/license/ETOwang/SysY-Compiler.svg) - see the [LICENSE](https://github.com/ETOwang/SysY-Compiler/blob/main/LICENSE) file for details.

## Quick Start

To get started with SysY-Compiler, follow these simple steps:

1. **Clone the repository**:
   ```bash  
   git clone https://github.com/ETOwang/SysY-Compiler.git  
   cd SysY-Compiler
2. **Build the project**:
   ```bash  
   mvn clean compile assembly:single

3. **Run the compiler**:
   * Generate Assembly Code (Default ARM)
     ``` bash
     java -jar .\target\compiler-1.0-SNAPSHOT-jar-with-dependencies.jar source -S -o dest 

   * Generate LLVM IR 
     ``` bash
     java -jar .\target\compiler-1.0-SNAPSHOT-jar-with-dependencies.jar source -emit-llvm -o dest 

   * Enable Optimization 
     ``` bash
     java -jar .\target\compiler-1.0-SNAPSHOT-jar-with-dependencies.jar source -S -O1 -o dest 
     java -jar .\target\compiler-1.0-SNAPSHOT-jar-with-dependencies.jar source -S -O2 -o dest 
   
## Project Introduction
SysY-Compiler is a compiler designed specifically for the sysy language, a subset of the C programming language. It has the capability to translate sysy source code into specified target assembly code.

## Statement
This project is a continuation and enhancement of a previous team project. In the original project, I was primarily responsible for mid-level and backend optimizations, along with minor debugging tasks in both the frontend and backend.In this iteration, I will be refactoring the entire backend implementation and further optimizing parts of the mid-level to improve performance and maintainability.
Click [ here ](https://gitlab.eduxiji.net/T202410284203580/compilers) to see the original project .
  