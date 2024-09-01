package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscDivw extends RiscDefaultInstruction {
    public RiscDivw(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.DIVW, rd, rs1, rs2);
    }
}
