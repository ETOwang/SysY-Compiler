package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscAdd extends RiscDefaultInstruction {

    public RiscAdd(RiscOperand d, RiscOperand s1, RiscOperand s2) {
        super(RiscOpcode.ADD, d, s1, s2);
    }

}
