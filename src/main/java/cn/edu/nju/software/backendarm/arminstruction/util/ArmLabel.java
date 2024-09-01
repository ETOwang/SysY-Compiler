package cn.edu.nju.software.backendarm.arminstruction.util;

import cn.edu.nju.software.backendarm.arminstruction.ArmDefaultInstruction;

public class ArmLabel extends ArmDefaultInstruction {
    private String label;

    public ArmLabel(String label) {
        super(ArmOpcode.LABEL);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    @Override
    public String emitCode() {
        return System.lineSeparator() + label + ":";
    }
}
