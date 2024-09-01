package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscOr extends RiscDefaultInstruction {

    public RiscOr(RiscOperand d, RiscOperand s, RiscOperand t) {
        super(RiscOpcode.OR, d, s, t);
    }
}
