package cn.edu.nju.software.backendrisc.riscinstruction.pseudo;
import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSltz extends RiscDefaultInstruction {

        public RiscSltz(RiscOperand rd, RiscOperand rs1) {
            super(RiscOpcode.SLTZ, rd, rs1);
        }
}
