package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmLsl extends ArmDefaultInstruction {

    public ArmLsl(ArmOperand... armOperands) {
        super(ArmOpcode.LSL, armOperands);
    }
}
