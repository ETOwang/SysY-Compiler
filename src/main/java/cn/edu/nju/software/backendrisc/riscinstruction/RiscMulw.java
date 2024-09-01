package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscMulw extends RiscDefaultInstruction {

    public RiscMulw(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.MULW, rd, rs1, rs2);
    }
}
