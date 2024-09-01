package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFcvtws extends RiscDefaultInstruction {

    public RiscFcvtws(RiscOperand rs1, RiscOperand rd) {
        super(RiscOpcode.FCVT_W_S, rs1, rd);
    }

    @Override
    public String emitCode() {
        return super.emitCode() + ", rtz";
    }
}
