package cn.edu.nju.software.backendrisc.riscinstruction.pseudo;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscLabelAddress;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscLabel;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscCall extends RiscDefaultInstruction {

    public RiscCall(String label) {
        super(RiscOpcode.CALL, new RiscLabelAddress(new RiscLabel(label)));
    }

}
