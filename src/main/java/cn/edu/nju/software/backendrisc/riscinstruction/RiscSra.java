package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSra extends RiscDefaultInstruction {

    public RiscSra(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.SRA, rd, rs1, rs2);
    }
}
