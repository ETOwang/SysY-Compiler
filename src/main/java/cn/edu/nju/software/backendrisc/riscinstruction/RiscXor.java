package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscXor extends RiscDefaultInstruction {
        public RiscXor(RiscOperand rd, RiscOperand rs1, RiscOperand rs2) {
            super(RiscOpcode.XOR, rd, rs1, rs2);
        }
}
