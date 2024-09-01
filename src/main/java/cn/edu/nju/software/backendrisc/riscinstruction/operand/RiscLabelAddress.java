package cn.edu.nju.software.backendrisc.riscinstruction.operand;

import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscLabel;

public class RiscLabelAddress implements RiscOperand {

    RiscLabel label;

    public RiscLabelAddress(RiscLabel label) {
        this.label = label;
    }

    public RiscLabel getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label.getLabel();
    }
}
