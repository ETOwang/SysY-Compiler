package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmB extends ArmDefaultInstruction {

    public ArmB(ArmOperand... armOperands) {
        super(ArmOpcode.B, armOperands);
    }
}
