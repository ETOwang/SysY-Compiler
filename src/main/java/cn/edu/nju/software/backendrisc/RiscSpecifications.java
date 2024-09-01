package cn.edu.nju.software.backendrisc;

import cn.edu.nju.software.ir.type.*;

/**
 * use to provide the basic information of the RiscvMachine
 */
public record RiscSpecifications() {

    private static final int INT_SIZE = 4;
    private static final int FLOAT_SIZE = 4;
    private static final int POINTER_SIZE = 8;
    private static final int BOOL_SIZE = 4;
    private static final String[] libCallerSavedRegs= new String[] {"ra", "s0", "fs0"};
    private static final String[] callerSavedRegs = new String[] {"ra",
            "s0","s1","s2","s3","s4","s5","s7","s8","s9","s10","s11",
            "fs0","fs1","fs2","fs3", "fs4", "fs5", "fs6", "fs7", "fs8", "fs9", "fs10", "fs11"};
    private static final String[] calleeSavedRegs = new String[] {};
    private static final String[] argRegs = new String[] {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
    private static final String[] fArgRegs = new String[] {"fa0", "fa1", "fa2", "fa3", "fa4", "fa5", "fa6", "fa7"};
    private static final String[] tempVarRegs = new String[] {"s0","s1","s2","s3","fs0","fs1","fs2", "fs3"};
    private static final String[] intLocalVarRegs = new String[] {
            "s4","s5","s7","s8","s9","s10","s11"
    };

    private static final String[] floatLocalVarRegs = new String[] {
            "fs4", "fs5", "fs6", "fs7", "fs8", "fs9", "fs10", "fs11"
    };
    private static final boolean isDebug = false;

    public static String[] getCallerSavedRegs() {
        return callerSavedRegs;
    }

    public static String[] getLibCallerSavedRegs() {
        return libCallerSavedRegs;
    }

    public static String[] getCalleeSavedRegs() {
        return calleeSavedRegs;
    }

    public static String[] getArgRegs() {
        return argRegs;
    }

    public static String[] getFArgRegs() {
        return fArgRegs;
    }

    public static String[] getTempVarRegs() {
        return tempVarRegs;
    }

    public static String[] getIntVarRegs() {
        return intLocalVarRegs;
    }

    public static String[] getFloatVarRegs() {
        return floatLocalVarRegs;
    }
    public static boolean isFloatReg(String regName) {
        return regName.startsWith("f");
    }

    public static boolean isGeneralReg(String regName) {
        return !isFloatReg(regName);
    }

    public static boolean isGeneralType(TypeRef type) {
        return type instanceof IntType || type instanceof Pointer || type instanceof BoolType;
    }

    public static boolean isFloatType(TypeRef type) {
        return type instanceof FloatType;
    }

    public static int getIntSize() {
        return INT_SIZE;
    }

    public static int getFloatSize() {
        return FLOAT_SIZE;
    }

    public static int getPointerSize() {
        return POINTER_SIZE;
    }

    public static int getBoolSize() {
        return BOOL_SIZE;
    }

    public static boolean getIsDebug() {
        return isDebug;
    }
}
