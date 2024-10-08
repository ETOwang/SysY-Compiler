package cn.edu.nju.software.backendarm.arminstruction.util;

public enum ArmOpcode {
    LABEL,
    LDR,
    STR,
    MOV,
    MOVW,
    MOVT,
    ADD,
    SUB,
    MUL,
    MLA,
    SDIV,
    UDIV,
    OR,
    AND,
    EOR,
    LSL,
    LSR,
    ASR,
    CMP,
    BEQ,
    BX,
    BL,
    B,
    BLT,
    BGE,
    BNE,
    BLE,
    BGT,
    VADD_F32,
    VSUB_F32,
    VMUL_F32,
    VDIV_F32,
    VLDR_F32,
    VSTR_F32,
    MLS,
    VCMP_F32,
    VMRS,
    MOVGT,
    MOVLT,
    MOVGE,
    MOVLE,
    VCVT_F32_S32,
    VCVT_S32_F32,
    VMOV,
    VMOV_F32_S32,
    VMOV_S32_F32,
    VMOV_F32,
    VMOV_S32,
    COMMENT, BIC,
    MOVEQ,
    MOVNE,
}
