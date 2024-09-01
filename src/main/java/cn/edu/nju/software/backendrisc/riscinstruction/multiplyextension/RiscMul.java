package cn.edu.nju.software.backendrisc.riscinstruction.multiplyextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscMul extends RiscDefaultInstruction {

    public RiscMul(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.MUL, rd, rs1, rs2);
    }
}
