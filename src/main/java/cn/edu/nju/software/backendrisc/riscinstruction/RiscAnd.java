package cn.edu.nju.software.backendrisc.riscinstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscAnd extends RiscDefaultInstruction {

    public RiscAnd(RiscOperand d, RiscOperand s, RiscOperand t) {
        super(RiscOpcode.AND, d, s, t);
    }
}
