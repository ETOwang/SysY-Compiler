package cn.edu.nju.software.backendarm.arminstruction.util;

import cn.edu.nju.software.backendarm.arminstruction.ArmDefaultInstruction;

public class ArmComment extends ArmDefaultInstruction {

    private String comment;

    public ArmComment(String comment) {
        super(ArmOpcode.COMMENT);
        this.comment = comment;
    }

    @Override
    public String emitCode() {return System.lineSeparator() + "# " + comment;
    }
}
