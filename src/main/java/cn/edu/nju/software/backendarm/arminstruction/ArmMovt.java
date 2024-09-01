package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMovt extends ArmDefaultInstruction {

    public ArmMovt(ArmOperand... armOperands) {
        super(ArmOpcode.MOVT, armOperands);
    }

    @Override
    public String emitCode() {
        return "\tmovt" + " " + getOperands().get(0) + ", " + getOperands().get(1);
    }
}
