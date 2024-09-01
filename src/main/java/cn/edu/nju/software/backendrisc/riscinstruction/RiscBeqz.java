package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscLabelAddress;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscLabel;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscBeqz extends RiscDefaultInstruction {
    public RiscBeqz(RiscOperand cond, String label) {
        super(RiscOpcode.BEQZ, cond, new RiscLabelAddress(new RiscLabel(label)));
    }
}
