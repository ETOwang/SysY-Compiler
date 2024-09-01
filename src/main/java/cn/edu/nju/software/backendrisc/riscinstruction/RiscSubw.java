package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSubw extends RiscDefaultInstruction {
    public RiscSubw(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.SUBW, rd, rs1, rs2);
    }
}
