package cn.edu.nju.software.backendrisc.riscinstruction.floatextension;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscFsw extends RiscDefaultInstruction {

        public RiscFsw(RiscOperand rs1, RiscOperand rs2) {
            super(RiscOpcode.FSW, rs1, rs2);
        }
}
