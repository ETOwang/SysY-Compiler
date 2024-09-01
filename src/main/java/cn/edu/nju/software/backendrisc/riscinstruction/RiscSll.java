package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSll extends RiscDefaultInstruction {

    public RiscSll(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.SLL, rd, rs1, rs2);
    }
}
