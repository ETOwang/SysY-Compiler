package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscAddi extends RiscDefaultInstruction {

    public RiscAddi(RiscOperand d, RiscOperand s, RiscOperand imm) {
        super( RiscOpcode.ADDI, d, s, imm);
    }
}
