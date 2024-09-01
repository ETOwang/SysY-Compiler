package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFsubs extends RiscDefaultInstruction {

    public RiscFsubs(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.FSUB_S, rd, rs1, rs2);
    }
}
