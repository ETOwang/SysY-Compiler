package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

//todo() 所有浮点数暂时用32bt
public class RiscFlw extends RiscDefaultInstruction {

    public RiscFlw(RiscOperand rs1, RiscOperand rs2) {
        super(RiscOpcode.FLW, rs1, rs2);
    }
}
