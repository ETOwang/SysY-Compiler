package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMovlt extends ArmDefaultInstruction {

    public ArmMovlt(ArmOperand... armOperands) {
        super(ArmOpcode.MOVLT, armOperands);
    }
}
