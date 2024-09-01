package cn.edu.nju.software.backendrisc.riscinstruction;


import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscSd extends RiscDefaultInstruction {

    public RiscSd(RiscOperand d, RiscOperand s){
        super( RiscOpcode.SD, d, s);
    }
}
