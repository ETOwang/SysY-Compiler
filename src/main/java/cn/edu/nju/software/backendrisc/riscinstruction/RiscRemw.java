package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscRemw extends RiscDefaultInstruction {

    public RiscRemw(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.REMW, rd, rs1, rs2);
    }
}
