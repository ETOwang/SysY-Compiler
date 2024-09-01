package cn.edu.nju.software.backendrisc.riscinstruction.pseudo;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscLi extends RiscDefaultInstruction {
    public RiscLi(RiscOperand d, RiscOperand s) {
        super(RiscOpcode.LI, d, s);
    }
}
