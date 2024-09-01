package cn.edu.nju.software.backendrisc.riscinstruction.pseudo;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSeqz extends RiscDefaultInstruction {

    public RiscSeqz(RiscOperand rd, RiscOperand rs1) {
        super(RiscOpcode.SEQZ, rd, rs1);
    }

}
