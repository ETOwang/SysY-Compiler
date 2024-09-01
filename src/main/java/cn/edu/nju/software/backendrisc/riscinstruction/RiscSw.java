package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSw extends RiscDefaultInstruction {

    public RiscSw(RiscOperand rs2, RiscOperand rs1) {
        super(RiscOpcode.SW, rs2, rs1);
    }
}
