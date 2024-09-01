package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscLa extends RiscDefaultInstruction {

    public RiscLa(RiscOperand rd, RiscOperand label) {
        super(RiscOpcode.LA, rd, label);
    }
}
