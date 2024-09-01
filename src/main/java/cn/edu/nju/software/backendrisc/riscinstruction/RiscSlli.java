package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSlli extends RiscDefaultInstruction {
    public RiscSlli(RiscOperand rd, RiscOperand rs1, RiscOperand imm) {
        super(RiscOpcode.SLLI, rd, rs1, imm);
    }
}
