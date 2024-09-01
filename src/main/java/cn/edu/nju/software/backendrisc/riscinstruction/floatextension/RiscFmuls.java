package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;
import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFmuls extends RiscDefaultInstruction {

    public RiscFmuls(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.FMUL_S, rd, rs1, rs2);
    }
}
