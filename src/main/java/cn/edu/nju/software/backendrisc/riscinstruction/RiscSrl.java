package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSrl extends RiscDefaultInstruction {

    public RiscSrl(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.SRL,rd, rs1, rs2);
    }
}
