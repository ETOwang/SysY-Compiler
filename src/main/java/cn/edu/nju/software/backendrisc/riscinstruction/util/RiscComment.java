package cn.edu.nju.software.backendrisc.riscinstruction.util;

import cn.edu.nju.software.backendrisc.riscinstruction.RiscDefaultInstruction;

public class RiscComment extends RiscDefaultInstruction {
    private String comment;

    public RiscComment(String comment) {
        super(RiscOpcode.COMMENT);
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String emitCode() {
        return System.lineSeparator() + "\t# " + comment;
    }
}
