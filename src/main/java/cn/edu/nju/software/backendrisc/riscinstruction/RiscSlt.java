package cn.edu.nju.software.backendrisc.riscinstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;


public class RiscSlt extends RiscDefaultInstruction {

    public RiscSlt(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.SLT, rd, rs1, rs2);
    }
}