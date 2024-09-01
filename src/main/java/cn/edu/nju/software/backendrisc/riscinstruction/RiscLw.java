package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscLw extends RiscDefaultInstruction {
    public RiscLw(RiscOperand rd, RiscOperand rs1)
    {
        super(RiscOpcode.LW, rd, rs1);

    }
}
