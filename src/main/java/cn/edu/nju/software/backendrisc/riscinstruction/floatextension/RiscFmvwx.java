package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFmvwx extends RiscDefaultInstruction {

    public RiscFmvwx(RiscOperand rd, RiscOperand rs) {
        super(RiscOpcode.FMV_W_X, rd, rs);
    }
}
