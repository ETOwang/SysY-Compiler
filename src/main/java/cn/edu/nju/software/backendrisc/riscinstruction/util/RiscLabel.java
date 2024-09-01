package cn.edu.nju.software.backendrisc.riscinstruction.util;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;

public class RiscLabel extends RiscDefaultInstruction {
    private String label;

    public RiscLabel(String label) {
        super(RiscOpcode.LABEL);
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
