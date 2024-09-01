package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFcvtsw extends RiscDefaultInstruction {

    public RiscFcvtsw(RiscOperand rs1, RiscOperand rd) {
        super(RiscOpcode.FCVT_S_W, rs1, rd);
    }
}