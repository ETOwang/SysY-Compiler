package cn.edu.nju.software.backendarm.arminstruction.operand;

import cn.edu.nju.software.backendarm.arminstruction.util.ArmLabel;

public class ArmLabelAddress implements ArmOperand {
    ArmLabel label;

    public ArmLabelAddress(ArmLabel label) {
        this.label = label;
    }

    public ArmLabel getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label.getLabel();
    }
}
