package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmSub extends ArmDefaultInstruction {

    public ArmSub(ArmOperand... armOperands) {
        super(ArmOpcode.SUB, armOperands);
    }
}
