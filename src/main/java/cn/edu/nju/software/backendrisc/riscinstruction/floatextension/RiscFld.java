package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFld extends RiscDefaultInstruction {

    public RiscFld(RiscOperand rd, RiscOperand rs1) {
        super(RiscOpcode.FLD, rd, rs1);
    }
}
