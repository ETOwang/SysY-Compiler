package cn.edu.nju.software.backendrisc.riscinstruction.pseudo;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;


public class RiscSnez extends RiscDefaultInstruction {

    public RiscSnez(RiscOperand rd, RiscOperand rs1) {
        super(RiscOpcode.SNEZ, rd, rs1);
    }
}

