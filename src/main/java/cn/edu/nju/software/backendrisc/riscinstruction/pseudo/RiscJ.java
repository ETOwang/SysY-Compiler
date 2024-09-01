package cn.edu.nju.software.backendrisc.riscinstruction.pseudo;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscLabelAddress;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscLabel;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

public class RiscJ extends RiscDefaultInstruction {
    private RiscLabelAddress label;

    public RiscJ(String label) {
        super(RiscOpcode.J, new RiscLabelAddress(new RiscLabel(label)));
    }
}
