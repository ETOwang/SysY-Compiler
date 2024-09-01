package cn.edu.nju.software.ir.instruction;

public enum OpEnum {
     ADD,
     SUB,
     MUL,
     DIV,
     MOD,
     AND,
     OR,
     F2I,
     I2F,
     BR,
     ICMP,
     FCMP,
     XOR,
     ZEXT, // unsigned extension
     LOAD,
     STORE,
     ALLOC,
     GEP,
     CALL,
     RETURN,
     FADD,
     FSUB,
     FMUL,
     FDIV,
     ASHR, SHL, LSHR, PHI
}
