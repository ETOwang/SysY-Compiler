package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMul extends ArmDefaultInstruction {

    public ArmMul(ArmOperand... armOperands) {
        super(ArmOpcode.MUL, armOperands);
    }
}
