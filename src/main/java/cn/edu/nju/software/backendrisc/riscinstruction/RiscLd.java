package cn.edu.nju.software.backendrisc.riscinstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscLd extends RiscDefaultInstruction {

    public RiscLd(RiscOperand d, RiscOperand s) {
        super(RiscOpcode.LD, d, s);
    }
}
