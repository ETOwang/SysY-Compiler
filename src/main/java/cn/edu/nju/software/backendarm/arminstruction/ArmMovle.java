package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMovle extends ArmDefaultInstruction {
    public ArmMovle(ArmOperand... armOperands) {
        super(ArmOpcode.MOVLE, armOperands);
    }
}
