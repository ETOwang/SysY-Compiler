package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscAddw extends RiscDefaultInstruction {

    public RiscAddw(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.ADDW, rd, rs1, rs2);
    }

}
