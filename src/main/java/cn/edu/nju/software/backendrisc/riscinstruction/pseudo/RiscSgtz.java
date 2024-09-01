package cn.edu.nju.software.backendrisc.riscinstruction.pseudo;
import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;


public class RiscSgtz extends RiscDefaultInstruction {

    public RiscSgtz(RiscOperand rd, RiscOperand rs1) {
        super(RiscOpcode.SGTZ, rd, rs1);
    }

}
