package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFmvxw extends RiscDefaultInstruction {

    public RiscFmvxw(RiscOperand rd, RiscOperand rs) {
        super(RiscOpcode.FMV_X_W, rd, rs);
    }
}
