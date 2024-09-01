package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMovge extends ArmDefaultInstruction {
    public ArmMovge(ArmOperand... armOperands) {
        super(ArmOpcode.MOVGE, armOperands);
    }
}
