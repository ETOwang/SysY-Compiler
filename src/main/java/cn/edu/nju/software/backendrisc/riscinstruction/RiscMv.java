package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscMv extends RiscDefaultInstruction {

    public RiscMv(RiscOperand d, RiscOperand s) {
        super(RiscOpcode.MV, d, s);
    }
}
