package cn.edu.nju.software.ir.instruction;

import java.util.Arrays;

public class Operator {
    private final static String[] operators = new String[]{
            "add", "sub", "mul", "sdiv", "srem", "and", "or",
            "fptosi", "sitofp", "br", "icmp", "fcmp", "xor", "zext",
            "load", "store", "alloca", "getelementptr",
            "call", "ret", "fadd", "fsub", "fmul", "fdiv","ashr", "shl", "lshr", "phi"
    };

    public final static int CmpNE = 0; // !=
    public final static int CmpEQ = 1; // ==
    public final static int CmpSGT = 2; // >
    public final static int CmpSLT = 3; // <
    public final static int CmpSGE = 4; // >=
    public final static int CmpSLE = 5; // <=

    public final static String[] cmpType = new String[]{
            "ne", "eq", "sgt", "slt", "sge", "sle",
            "one", "oeq", "ogt", "olt", "oge", "ole"
    };

    protected static String getOperator(OpEnum index) {
        return operators[index.ordinal()];
    }

    protected static OpEnum getOp(String op) {
        int index = Arrays.asList(operators).indexOf(op);
        for (OpEnum opEnum : OpEnum.values()) {
            if (opEnum.ordinal() == index) {
                return opEnum;
            }
        }
        return null;
    }
}
